package com.example.data

import android.content.Context
import com.example.ui.viewmodel.MatchState
import com.example.ui.viewmodel.PlayerRole
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class MultiplayerStatus {
    UNINITIALIZED,
    READY,
    MATCHMAKING,
    CONNECTED,
    OPPONENT_LEFT,
    ERROR
}

data class OnlineRoom(
    val roomId: String = "",
    val player1Id: String = "",
    val player1Name: String = "",
    val player2Id: String = "",
    val player2Name: String = "",
    val status: String = "WAITING", // WAITING, PLAYING, FINISHED
    val player1Move: Int = 0,
    val player2Move: Int = 0,
    val ballsBowled: Int = 0,
    val firstInningsOver: Boolean = false,
    val currentInnings: Int = 1
)


class MultiplayerManager private constructor(context: Context) {

    private var database: FirebaseDatabase? = null
    private var auth: FirebaseAuth? = null
    private var roomRef: DatabaseReference? = null
    private var roomListener: ValueEventListener? = null

    private val _status = MutableStateFlow(MultiplayerStatus.UNINITIALIZED)
    val status: StateFlow<MultiplayerStatus> = _status

    private val _roomData = MutableStateFlow<OnlineRoom?>(null)
    val roomData: StateFlow<OnlineRoom?> = _roomData

    var myPlayerNum = 1 // 1 or 2
    var myUid = ""
    private var isSearching = false
    private var myUsername = ""


    init {
        try {
            // Attempt to check if Firebase is configured
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            } else {
                FirebaseApp.getInstance()
            }
            if (app != null) {
                database = FirebaseDatabase.getInstance()
                auth = FirebaseAuth.getInstance()
                _status.value = MultiplayerStatus.READY
            }
        } catch (e: Exception) {
            _status.value = MultiplayerStatus.UNINITIALIZED
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: MultiplayerManager? = null

        fun getInstance(context: Context): MultiplayerManager {
            return INSTANCE ?: synchronized(this) {
                val instance = MultiplayerManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    fun startMatchmaking(username: String, onMatchFound: (OnlineRoom) -> Unit) {
        val currentAuth = auth
        val currentDb = database
        if (currentAuth == null || currentDb == null) {
            _status.value = MultiplayerStatus.UNINITIALIZED
            return
        }

        if (isSearching) return
        isSearching = true
        myUsername = username
        _status.value = MultiplayerStatus.MATCHMAKING

        CoroutineScope(Dispatchers.IO).launch {

            try {
                // Perform anonymous sign in if not signed in
                if (currentAuth.currentUser == null) {
                    currentAuth.signInAnonymously().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            myUid = currentAuth.currentUser?.uid ?: UUID.randomUUID().toString()
                            findOrCreateRoom(currentDb, onMatchFound)
                        } else {
                            _status.value = MultiplayerStatus.ERROR
                            isSearching = false
                        }
                    }
                } else {
                    myUid = currentAuth.currentUser?.uid ?: UUID.randomUUID().toString()
                    findOrCreateRoom(currentDb, onMatchFound)
                }
            } catch (e: Exception) {
                _status.value = MultiplayerStatus.ERROR
                isSearching = false
            }
        }
    }

    private fun findOrCreateRoom(db: FirebaseDatabase, onMatchFound: (OnlineRoom) -> Unit) {
        val roomsQuery = db.getReference("rooms").orderByChild("status").equalTo("WAITING").limitToFirst(1)
        roomsQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Match found: Join existing room
                    val roomSnap = snapshot.children.first()
                    val room = roomSnap.getValue(OnlineRoom::class.java)
                    if (room != null) {
                        joinRoom(db, room.roomId, onMatchFound)
                    } else {
                        createNewRoom(db, onMatchFound)
                    }
                } else {
                    // No waiting room: Create a new room
                    createNewRoom(db, onMatchFound)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _status.value = MultiplayerStatus.ERROR
                isSearching = false
            }
        })
    }

    private fun createNewRoom(db: FirebaseDatabase, onMatchFound: (OnlineRoom) -> Unit) {
        val newRoomId = UUID.randomUUID().toString().substring(0, 8)
        val roomRef = db.getReference("rooms").child(newRoomId)
        val newRoom = OnlineRoom(
            roomId = newRoomId,
            player1Id = myUid,
            player1Name = myUsername,
            status = "WAITING"
        )
        myPlayerNum = 1


        roomRef.setValue(newRoom).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                listenToRoom(roomRef, onMatchFound)
            } else {
                _status.value = MultiplayerStatus.ERROR
                isSearching = false
            }
        }
    }

    private fun joinRoom(db: FirebaseDatabase, roomId: String, onMatchFound: (OnlineRoom) -> Unit) {
        val ref = db.getReference("rooms").child(roomId)
        myPlayerNum = 2

        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val room = mutableData.getValue(OnlineRoom::class.java) ?: return Transaction.success(mutableData)
                if (room.status == "WAITING" && room.player2Id.isEmpty()) {
                    mutableData.child("player2Id").value = myUid
                    mutableData.child("player2Name").value = myUsername
                    mutableData.child("status").value = "PLAYING"
                    return Transaction.success(mutableData)
                }
                return Transaction.abort()

            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (committed && snapshot != null) {
                    val room = snapshot.getValue(OnlineRoom::class.java)
                    if (room != null) {
                        listenToRoom(ref, onMatchFound)
                    } else {
                        createNewRoom(db, onMatchFound)
                    }
                } else {
                    createNewRoom(db, onMatchFound)
                }
            }
        })
    }

    private fun listenToRoom(ref: DatabaseReference, onMatchFound: (OnlineRoom) -> Unit) {
        roomRef = ref
        roomListener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val room = snapshot.getValue(OnlineRoom::class.java)
                if (room != null) {
                    _roomData.value = room
                    if (room.status == "PLAYING" && _status.value != MultiplayerStatus.CONNECTED) {
                        _status.value = MultiplayerStatus.CONNECTED
                        isSearching = false
                        onMatchFound(room)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _status.value = MultiplayerStatus.ERROR
                isSearching = false
            }
        })
    }

    fun submitMove(move: Int) {
        val ref = roomRef ?: return
        val path = if (myPlayerNum == 1) "player1Move" else "player2Move"
        ref.child(path).setValue(move)
    }

    fun resetRoomMoves(nextBallsCount: Int) {
        val ref = roomRef ?: return
        val updates = mapOf(
            "player1Move" to 0,
            "player2Move" to 0,
            "ballsBowled" to nextBallsCount
        )
        ref.updateChildren(updates)
    }

    fun updateInningsTransition(newRolePath1: PlayerRole, newRolePath2: PlayerRole) {
        val ref = roomRef ?: return
        val updates = mapOf(
            "player1Move" to 0,
            "player2Move" to 0,
            "ballsBowled" to 0,
            "currentInnings" to 2
        )
        ref.updateChildren(updates)
    }

    fun disconnect() {
        roomListener?.let { roomRef?.removeEventListener(it) }
        roomRef = null
        roomListener = null
        _roomData.value = null
        isSearching = false
        if (_status.value != MultiplayerStatus.UNINITIALIZED) {
            _status.value = MultiplayerStatus.READY
        }
    }
}
