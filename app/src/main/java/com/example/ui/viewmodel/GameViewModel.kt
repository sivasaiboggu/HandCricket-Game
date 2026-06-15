package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.MultiplayerManager
import com.example.data.SecurityHelper
import com.example.data.MultiplayerStatus
import com.example.data.OnlineRoom
import com.example.data.model.Achievement
import com.example.data.model.MatchHistory
import com.example.data.model.PlayerStats
import com.example.data.repository.GameRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class Screen {
    MENU, CONFIG, TOSS, PLAY, STATS, ACHIEVEMENTS, MULTIPLAYER_MATCHMAKING, PROFILE_SETUP, SIGN_IN, SIGN_UP
}


enum class PlayerRole {
    BATTING, BOWLING
}

enum class MatchPhase {
    NOT_STARTED, INNINGS_1, INNINGS_2, COMPLETED
}

enum class StadiumTheme(
    val id: String,
    val title: String,
    val description: String,
    val isPremium: Boolean,
    val levelRequired: Int
) {
    CLASSIC_TURF("classic_turf", "Classic Green Turf", "Traditional stadium under brilliant mid-day sun", false, 1),
    SUNSET_LAGOON("sunset_lagoon", "Sunset Lagoon", "Play in a beach-side arena under a warm golden sunset", true, 2),
    CYBER_ARENA("cyber_arena", "Cyber Grid Dome", "Vaporwave cyberpunk dome with pulsing neon stadium lights", true, 3)
}

data class MatchState(
    val phase: MatchPhase = MatchPhase.NOT_STARTED,
    val currentInnings: Int = 1,
    val playerRole: PlayerRole = PlayerRole.BATTING,
    val playerRuns: Int = 0,
    val playerWickets: Int = 0,
    val aiRuns: Int = 0,
    val aiWickets: Int = 0,
    val ballsBowled: Int = 0,
    val target: Int? = null,
    val isOutEvent: Boolean = false,
    val isScoreEvent: Boolean = false,
    val lastPlayerMove: Int = 0,
    val lastAiMove: Int = 0,
    val commentText: String = "Welcome to the pitch! Warm up your fingers.",
    val matchWicketsLimit: Int = 1,
    val matchOversLimit: Int = 1,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val stadiumTheme: StadiumTheme = StadiumTheme.CLASSIC_TURF,
    val dotBallsBowledThisMatch: Int = 0,
    val sixesHitThisMatch: Int = 0,
    val winMessage: String = "",
    val xpGained: Int = 0,
    val isActionShaking: Boolean = false,
    val timerValue: Int = 10,
    val recentBalls: List<String> = emptyList(),
    val isMultiplayer: Boolean = false,
    val myPlayerNum: Int = 1,
    val multiplayerStatus: MultiplayerStatus = MultiplayerStatus.UNINITIALIZED,
    val roomId: String = "",
    val myPlayerName: String = "YOU",
    val opponentPlayerName: String = "AI DEFENDER",
    val isOfflineMode: Boolean = false
)


class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    val playerStats: StateFlow<PlayerStats>
    val matchHistory: StateFlow<List<MatchHistory>>
    val achievements: StateFlow<List<Achievement>>

    private val multiplayerManager = MultiplayerManager.getInstance(application)
    private var mpObserverJob: kotlinx.coroutines.Job? = null
    private var isResolvingMpBall = false

    private val _currentScreen = MutableStateFlow(Screen.MENU)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _matchState = MutableStateFlow(MatchState())
    val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    // Toss logic state
    private val _tossWinner = MutableStateFlow<String?>(null) // "PLAYER" or "AI"
    val tossWinner: StateFlow<String?> = _tossWinner.asStateFlow()
    
    private val _tossResult = MutableStateFlow<String>("") // "HEADS" or "TAILS"
    val tossResult: StateFlow<String> = _tossResult.asStateFlow()

    private val _isCoinSpinning = MutableStateFlow(false)
    val isCoinSpinning: StateFlow<Boolean> = _isCoinSpinning.asStateFlow()

    private val aiEngine = HandCricketAi()
    private var timerJob: kotlinx.coroutines.Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao)
        
        playerStats = repository.playerStats.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerStats()
        )
        matchHistory = repository.matchHistory.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        achievements = repository.achievements.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            repository.initializeDataIfNeeded()
        }

        // Keep multiplayerStatus synced in local match state
        viewModelScope.launch {
            multiplayerManager.status.collect { status ->
                _matchState.update { it.copy(multiplayerStatus = status) }
            }
        }

        // Initialize Authentication State and navigate accordingly
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val secureName = SecurityHelper.secureGet(application, "username", "")
            if (secureName.isNotEmpty()) {
                _matchState.update { 
                    it.copy(
                        myPlayerName = secureName, 
                        isOfflineMode = false
                    ) 
                }
                _currentScreen.value = Screen.MENU
            } else {
                _currentScreen.value = Screen.PROFILE_SETUP
            }
        } else {
            _currentScreen.value = Screen.SIGN_IN
        }
    }

    fun startCountdownTimer() {
        timerJob?.cancel()
        _matchState.update { it.copy(timerValue = 10) }
        timerJob = viewModelScope.launch {
            while (_matchState.value.timerValue > 0) {
                delay(1000)
                if (_matchState.value.phase == MatchPhase.COMPLETED) {
                    timerJob?.cancel()
                    return@launch
                }
                _matchState.update { it.copy(timerValue = it.timerValue - 1) }
            }
            // Timer expired! Play 0 (representing dot ball / timeout)
            playBall(0)
        }
    }

    // Navigation helper
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // Match configurations
    fun configureMatch(overs: Int, wickets: Int, difficulty: Difficulty, theme: StadiumTheme) {
        _matchState.update {
            it.copy(
                matchOversLimit = overs,
                matchWicketsLimit = wickets,
                difficulty = difficulty,
                stadiumTheme = theme,
                phase = MatchPhase.NOT_STARTED,
                recentBalls = emptyList(),
                isMultiplayer = false
            )
        }
        aiEngine.resetSession()
        _tossWinner.value = null
        _tossResult.value = ""
        navigateTo(Screen.TOSS)
    }

    // Toss Choice
    fun playToss(playerChoice: String) { // "HEADS" or "TAILS"
        viewModelScope.launch {
            _isCoinSpinning.value = true
            delay(1500) // Spin animation delay
            _isCoinSpinning.value = false

            val coinSides = listOf("HEADS", "TAILS")
            val outcomes = coinSides.random()
            _tossResult.value = outcomes

            if (playerChoice.uppercase() == outcomes) {
                _tossWinner.value = "PLAYER"
            } else {
                _tossWinner.value = "AI"
                delay(800)
                // AI makes its choice
                val aiTossChoice = listOf("BATTING", "BOWLING").random()
                val role = if (aiTossChoice == "BATTING") PlayerRole.BOWLING else PlayerRole.BATTING
                
                _matchState.update {
                    it.copy(
                        playerRole = role,
                        commentText = "AI won the toss and elected to ${if (aiTossChoice == "BATTING") "bat" else "bowl"} first!",
                        phase = MatchPhase.INNINGS_1
                    )
                }
                delay(1200)
                navigateTo(Screen.PLAY)
                startCountdownTimer()
            }
        }
    }

    fun makeTossDecision(roleChoice: PlayerRole) {
        _matchState.update {
            it.copy(
                playerRole = roleChoice,
                commentText = "You won the toss and elected to ${if (roleChoice == PlayerRole.BATTING) "bat" else "bowl"} first!",
                phase = MatchPhase.INNINGS_1
            )
        }
        navigateTo(Screen.PLAY)
        startCountdownTimer()
    }

    // Authentication & Profile Setup helper methods
    fun signInWithEmail(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
                    val fallbackUsername = email.substringBefore("@")
                    try {
                        val database = com.google.firebase.database.FirebaseDatabase.getInstance()
                        val dbRef = database.getReference("users").child(uid)
                        dbRef.child("username").get().addOnCompleteListener { dbTask ->
                            val username = if (dbTask.isSuccessful) {
                                dbTask.result?.value as? String ?: fallbackUsername
                            } else {
                                fallbackUsername
                            }
                            // Save securely locally
                            com.example.data.SecurityHelper.secureSave(getApplication(), "username", username)
                            _matchState.update { 
                                it.copy(
                                    myPlayerName = username,
                                    isOfflineMode = false
                                ) 
                            }
                            navigateTo(Screen.MENU)
                            onSuccess()
                        }
                    } catch (e: Exception) {
                        // Database URL is missing fallback
                        com.example.data.SecurityHelper.secureSave(getApplication(), "username", fallbackUsername)
                        _matchState.update { 
                            it.copy(
                                myPlayerName = fallbackUsername,
                                isOfflineMode = false
                            ) 
                        }
                        navigateTo(Screen.MENU)
                        onSuccess()
                    }
                } else {
                    onError(task.exception?.localizedMessage ?: "Sign-in failed")
                }
            }
    }

    fun signUpWithEmail(username: String, email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
                    try {
                        val database = com.google.firebase.database.FirebaseDatabase.getInstance()
                        val userMap = mapOf("username" to username)
                        
                        database.getReference("users").child(uid).setValue(userMap)
                            .addOnCompleteListener { dbTask ->
                                // Save securely locally
                                com.example.data.SecurityHelper.secureSave(getApplication(), "username", username)
                                _matchState.update { 
                                    it.copy(
                                        myPlayerName = username,
                                        isOfflineMode = false
                                    ) 
                                }
                                navigateTo(Screen.MENU)
                                onSuccess()
                            }
                    } catch (e: Exception) {
                        // Fallback on database URL missing
                        com.example.data.SecurityHelper.secureSave(getApplication(), "username", username)
                        _matchState.update { 
                            it.copy(
                                myPlayerName = username,
                                isOfflineMode = false
                            ) 
                        }
                        navigateTo(Screen.MENU)
                        onSuccess()
                    }
                } else {
                    onError(task.exception?.localizedMessage ?: "Registration failed")
                }
            }
    }

    fun signInWithGoogle(idToken: String, defaultUsername: String, email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
                    var finalUsername = defaultUsername.ifEmpty { email.substringBefore("@") }
                    try {
                        val database = com.google.firebase.database.FirebaseDatabase.getInstance()
                        val dbRef = database.getReference("users").child(uid)
                        dbRef.child("username").get().addOnCompleteListener { dbTask ->
                            val existingName = dbTask.result?.value as? String
                            if (!existingName.isNullOrEmpty()) {
                                finalUsername = existingName
                            } else {
                                try {
                                    dbRef.child("username").setValue(finalUsername)
                                } catch (dbEx: Exception) {
                                    // Ignore db write failure
                                }
                            }
                            com.example.data.SecurityHelper.secureSave(getApplication(), "username", finalUsername)
                            _matchState.update { 
                                it.copy(
                                    myPlayerName = finalUsername,
                                    isOfflineMode = false
                                ) 
                            }
                            navigateTo(Screen.MENU)
                            onSuccess()
                        }
                    } catch (e: Exception) {
                        // Fallback on database URL missing
                        com.example.data.SecurityHelper.secureSave(getApplication(), "username", finalUsername)
                        _matchState.update { 
                            it.copy(
                                myPlayerName = finalUsername,
                                isOfflineMode = false
                            ) 
                        }
                        navigateTo(Screen.MENU)
                        onSuccess()
                    }
                } else {
                    onError(task.exception?.localizedMessage ?: "Google Sign-In failed")
                }
            }
    }

    fun signInWithGoogleSimulated(username: String, email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        auth.signInAnonymously().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = auth.currentUser?.uid ?: ""
                try {
                    val database = com.google.firebase.database.FirebaseDatabase.getInstance()
                    val userMap = mapOf(
                        "username" to username,
                        "email" to email,
                        "provider" to "google"
                    )
                    database.getReference("users").child(uid).setValue(userMap)
                        .addOnCompleteListener { dbTask ->
                            com.example.data.SecurityHelper.secureSave(getApplication(), "username", username)
                            _matchState.update { 
                                it.copy(
                                    myPlayerName = username,
                                    isOfflineMode = false
                                ) 
                            }
                            navigateTo(Screen.MENU)
                            onSuccess()
                        }
                } catch (e: Exception) {
                    com.example.data.SecurityHelper.secureSave(getApplication(), "username", username)
                    _matchState.update { 
                        it.copy(
                            myPlayerName = username,
                            isOfflineMode = false
                        ) 
                    }
                    navigateTo(Screen.MENU)
                    onSuccess()
                }
            } else {
                onError(task.exception?.localizedMessage ?: "Google Sign-in failed")
            }
        }
    }

    fun playOfflineMode() {
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        com.example.data.SecurityHelper.clearSecureCache(getApplication())
        _matchState.update { 
            it.copy(
                myPlayerName = "GUEST ATHLETE",
                isOfflineMode = true
            ) 
        }
        navigateTo(Screen.MENU)
    }

    fun signOut() {
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        com.example.data.SecurityHelper.clearSecureCache(getApplication())
        _matchState.update { 
            it.copy(
                myPlayerName = "YOU",
                isOfflineMode = false
            ) 
        }
        navigateTo(Screen.SIGN_IN)
    }

    // Multiplayer Matchmaking
    fun startMultiplayerMatchmaking() {
        if (_matchState.value.isOfflineMode) {
            // Cannot start matchmaking in offline mode!
            return
        }
        val cachedName = com.example.data.SecurityHelper.secureGet(getApplication(), "username", "")
        
        if (cachedName.isEmpty()) {
            navigateTo(Screen.PROFILE_SETUP)
        } else {
            initiateMatchmaking(cachedName)
        }
    }

    private fun initiateMatchmaking(username: String) {
        navigateTo(Screen.MULTIPLAYER_MATCHMAKING)
        multiplayerManager.startMatchmaking(username) { room ->
            val myRole = if (multiplayerManager.myPlayerNum == 1) PlayerRole.BATTING else PlayerRole.BOWLING
            val oppName = if (multiplayerManager.myPlayerNum == 1) room.player2Name else room.player1Name
            
            // Populate matching info first so visual match ticker slows down and highlights opponent name
            _matchState.update {
                it.copy(
                    myPlayerName = username,
                    opponentPlayerName = if (oppName.isEmpty()) "OPPONENT" else oppName,
                    myPlayerNum = multiplayerManager.myPlayerNum,
                    roomId = room.roomId,
                    isMultiplayer = true
                )
            }
            
            // Allow 2.2 seconds for matchmaking visual animations on UI before starting play
            viewModelScope.launch {
                delay(2200)
                
                _matchState.update {
                    it.copy(
                        phase = MatchPhase.INNINGS_1,
                        currentInnings = 1,
                        playerRole = myRole,
                        playerRuns = 0,
                        playerWickets = 0,
                        aiRuns = 0,
                        aiWickets = 0,
                        ballsBowled = 0,
                        target = null,
                        commentText = if (multiplayerManager.myPlayerNum == 1) {
                            "Match Found! You are batting first. Choose your number!"
                        } else {
                            "Match Found! You are bowling first. Guess the opponent's number!"
                        },
                        recentBalls = emptyList()
                    )
                }
                navigateTo(Screen.PLAY)
                startCountdownTimer()
                observeMultiplayerRoom(username)
            }
        }
    }

    fun saveUsernameAndMatch(username: String) {
        com.example.data.SecurityHelper.secureSave(getApplication(), "username", username)
        
        // Also save to database if online
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            try {
                val database = com.google.firebase.database.FirebaseDatabase.getInstance()
                database.getReference("users").child(uid).child("username").setValue(username)
            } catch (e: Exception) {
                // Ignore DB write failure, local secure cache is primary
            }
        }
        
        initiateMatchmaking(username)
    }

    fun cancelMultiplayerMatchmaking() {
        multiplayerManager.disconnect()
        navigateTo(Screen.MENU)
    }

    private fun observeMultiplayerRoom(myUsername: String) {
        mpObserverJob?.cancel()
        isResolvingMpBall = false
        mpObserverJob = viewModelScope.launch {
            var lastProcessedBall = -1
            multiplayerManager.roomData.collect { room ->
                if (room == null || room.status != "PLAYING") return@collect

                // Update opponent's name dynamically once they join the lobby
                val oppName = if (multiplayerManager.myPlayerNum == 1) room.player2Name else room.player1Name
                if (oppName.isNotEmpty() && _matchState.value.opponentPlayerName != oppName) {
                    _matchState.update {
                        it.copy(opponentPlayerName = oppName)
                    }
                }

                if (isResolvingMpBall) return@collect

                if (room.player1Move > 0 && room.player2Move > 0) {
                    if (room.ballsBowled > lastProcessedBall) {
                        lastProcessedBall = room.ballsBowled
                        isResolvingMpBall = true
                        resolveMultiplayerBall(room.player1Move, room.player2Move)
                    }
                }
            }
        }
    }


    // Play Ball action
    fun playBall(playerMove: Int) {
        if (_matchState.value.phase == MatchPhase.COMPLETED) return
        
        // Cancel timer immediately
        timerJob?.cancel()

        if (_matchState.value.isMultiplayer) {
            multiplayerManager.submitMove(playerMove)
            _matchState.update {
                it.copy(commentText = "Waiting for opponent's choice...")
            }
            return
        }

        viewModelScope.launch {
            // Put game into shaking phase first
            _matchState.update {
                it.copy(
                    isActionShaking = true,
                    lastPlayerMove = 0,
                    lastAiMove = 0,
                    commentText = "Shaking hands... 1... 2... 3..."
                )
            }
            delay(1000) // Shake for 1 sec

            val currentState = _matchState.value

            val role = if (currentState.playerRole == PlayerRole.BATTING) ChoiceRole.BATSMAN else ChoiceRole.BOWLER
            val aiMove = aiEngine.generateAiMove(role, currentState.difficulty)

            // Record player move in smart AI memory (ignore 0 timeout moves)
            if (playerMove in 1..6) {
                val isPlayerBatting = currentState.playerRole == PlayerRole.BATTING
                aiEngine.recordPlayerMove(playerMove, isPlayerBatting)
            }

            // Dynamic states to update
            var newPlayerRuns = currentState.playerRuns
            var newPlayerWickets = currentState.playerWickets
            var newAiRuns = currentState.aiRuns
            var newAiWickets = currentState.aiWickets
            var newBallsBowled = currentState.ballsBowled + 1
            var dotBallsThisBall = 0
            var sixesHitThisBall = 0

            var isOut = false
            var isScored = false
            var commentary = ""

            // Compare Moves
            if (playerMove == aiMove) {
                isOut = true
                commentary = generateOutCommentary(playerMove, currentState.playerRole)

                if (currentState.playerRole == PlayerRole.BATTING) {
                    newPlayerWickets++
                } else {
                    newAiWickets++
                }
            } else {
                isScored = true
                if (currentState.playerRole == PlayerRole.BATTING) {
                    newPlayerRuns += playerMove
                    commentary = if (playerMove == 0) {
                        "Timeout! You failed to play in time. Dot ball!"
                    } else {
                        generateBattingCommentary(playerMove)
                    }
                    if (playerMove == 6) sixesHitThisBall++
                } else {
                    newAiRuns += aiMove
                    commentary = if (playerMove == 0) {
                        "Timeout! You failed to bowl in time. AI batsman scored $aiMove!"
                    } else {
                        generateBowlingCommentary(aiMove)
                    }
                    if (aiMove == 6) {
                        // AI hit a 6
                    } else if (aiMove == 0 || playerMove == 6) {
                        // Dot ball if bowler restricts
                    }
                    if (aiMove == 0 || playerMove == 6) {
                        dotBallsThisBall++
                    }
                }
            }

            // Timeline result calculation for Crex scoreboard
            val ballResult = if (isOut) {
                "W"
            } else if (currentState.playerRole == PlayerRole.BATTING) {
                playerMove.toString()
            } else {
                aiMove.toString()
            }
            val updatedRecentBalls = currentState.recentBalls + ballResult

            // Reveal results
            _matchState.update {
                it.copy(
                    isActionShaking = false,
                    lastPlayerMove = playerMove,
                    lastAiMove = aiMove,
                    isOutEvent = isOut,
                    isScoreEvent = isScored,
                    commentText = commentary
                )
            }

            delay(1800) // Keep reveal visible to allow celebration overlay to finish smoothly

            // Clear visual screen flash
            _matchState.update {
                it.copy(
                    isOutEvent = false,
                    isScoreEvent = false
                )
            }

            // Check if end of Innings 1
            val maxBalls = currentState.matchOversLimit * 6
            val isInnings1Ended = if (currentState.currentInnings == 1) {
                if (currentState.playerRole == PlayerRole.BATTING) {
                    newPlayerWickets >= currentState.matchWicketsLimit || newBallsBowled >= maxBalls
                } else {
                    newAiWickets >= currentState.matchWicketsLimit || newBallsBowled >= maxBalls
                }
            } else false

            if (currentState.currentInnings == 1 && isInnings1Ended) {
                // Swap Roles for Innings 2
                val currentInnings1Score = if (currentState.playerRole == PlayerRole.BATTING) newPlayerRuns else newAiRuns
                val targetScore = currentInnings1Score + 1
                val newRole = if (currentState.playerRole == PlayerRole.BATTING) PlayerRole.BOWLING else PlayerRole.BATTING

                _matchState.update {
                    it.copy(
                        currentInnings = 2,
                        playerRuns = newPlayerRuns,
                        playerWickets = newPlayerWickets,
                        aiRuns = newAiRuns,
                        aiWickets = newAiWickets,
                        ballsBowled = 0,
                        playerRole = newRole,
                        target = targetScore,
                        commentText = "Innings Break! Target is: $targetScore runs off $maxBalls balls!",
                        recentBalls = emptyList() // clear history on innings transition
                    )
                }
                startCountdownTimer()
                return@launch
            }

            // If Innings 2, check for Chase Result standard conditions
            if (currentState.currentInnings == 2) {
                val target = currentState.target ?: 1
                val isChaseSuccessful = if (currentState.playerRole == PlayerRole.BATTING) {
                    newPlayerRuns >= target
                } else {
                    newAiRuns >= target
                }

                val isInnings2Ended = if (currentState.playerRole == PlayerRole.BATTING) {
                    newPlayerWickets >= currentState.matchWicketsLimit || newBallsBowled >= maxBalls
                } else {
                    newAiWickets >= currentState.matchWicketsLimit || newBallsBowled >= maxBalls
                }

                if (isChaseSuccessful) {
                    // Match Finished (Chase Complete)
                    val playerWon = currentState.playerRole == PlayerRole.BATTING
                    completeMatchFinish(
                        newPlayerRuns, newPlayerWickets, newAiRuns, newAiWickets, playerWon,
                        currentState, currentState.dotBallsBowledThisMatch + dotBallsThisBall,
                        currentState.sixesHitThisMatch + sixesHitThisBall
                    )
                    return@launch
                } else if (isInnings2Ended) {
                    // Batsman failed to chase
                    val playerWon = currentState.playerRole == PlayerRole.BOWLING
                    completeMatchFinish(
                        newPlayerRuns, newPlayerWickets, newAiRuns, newAiWickets, playerWon,
                        currentState, currentState.dotBallsBowledThisMatch + dotBallsThisBall,
                        currentState.sixesHitThisMatch + sixesHitThisBall
                    )
                    return@launch
                }
            }

            _matchState.update {
                it.copy(
                    playerRuns = newPlayerRuns,
                    playerWickets = newPlayerWickets,
                    aiRuns = newAiRuns,
                    aiWickets = newAiWickets,
                    ballsBowled = newBallsBowled,
                    dotBallsBowledThisMatch = it.dotBallsBowledThisMatch + dotBallsThisBall,
                    sixesHitThisMatch = it.sixesHitThisMatch + sixesHitThisBall,
                    recentBalls = updatedRecentBalls
                )
            }
            startCountdownTimer()
        }
    }

    private fun resolveMultiplayerBall(p1Move: Int, p2Move: Int) {
        timerJob?.cancel()

        viewModelScope.launch {
            // Shake phase
            _matchState.update {
                it.copy(
                    isActionShaking = true,
                    lastPlayerMove = 0,
                    lastAiMove = 0,
                    commentText = "Shaking hands... 1... 2... 3..."
                )
            }
            delay(1000)

            val currentState = _matchState.value
            val myNum = currentState.myPlayerNum
            val myMove = if (myNum == 1) p1Move else p2Move
            val opponentMove = if (myNum == 1) p2Move else p1Move

            var newPlayerRuns = currentState.playerRuns
            var newPlayerWickets = currentState.playerWickets
            var newAiRuns = currentState.aiRuns
            var newAiWickets = currentState.aiWickets
            var newBallsBowled = currentState.ballsBowled + 1
            var dotBallsThisBall = 0
            var sixesHitThisBall = 0

            val isOut = p1Move == p2Move
            val runsVal = if (currentState.currentInnings == 1) p1Move else p2Move
            
            var commentary = ""

            if (isOut) {
                commentary = generateOutCommentary(runsVal, currentState.playerRole)
                if (myNum == 1) {
                    if (currentState.currentInnings == 1) newPlayerWickets++ else newAiWickets++
                } else {
                    if (currentState.currentInnings == 1) newAiWickets++ else newPlayerWickets++
                }
            } else {
                if (myNum == 1) {
                    if (currentState.currentInnings == 1) newPlayerRuns += runsVal else newAiRuns += runsVal
                } else {
                    if (currentState.currentInnings == 1) newAiRuns += runsVal else newPlayerRuns += runsVal
                }
                
                commentary = if (currentState.playerRole == PlayerRole.BATTING) {
                    if (runsVal == 0) "Timeout! Dot ball!" else generateBattingCommentary(runsVal)
                } else {
                    if (runsVal == 0) "Timeout! Opponent batsman scored 0!" else generateBowlingCommentary(runsVal)
                }

                if (runsVal == 6) sixesHitThisBall++
                if (runsVal == 0) dotBallsThisBall++
            }

            // Timeline result chip
            val ballResult = if (isOut) "W" else runsVal.toString()
            val updatedRecentBalls = currentState.recentBalls + ballResult

            // Reveal result
            _matchState.update {
                it.copy(
                    isActionShaking = false,
                    lastPlayerMove = myMove,
                    lastAiMove = opponentMove,
                    isOutEvent = isOut,
                    isScoreEvent = !isOut,
                    commentText = commentary
                )
            }

            delay(1800)

            // Clear visual screens
            _matchState.update {
                it.copy(
                    isOutEvent = false,
                    isScoreEvent = false
                )
            }

            val maxBalls = currentState.matchOversLimit * 6

            // Determine if Innings 1 ended
            val batsmanWickets = if (currentState.currentInnings == 1) {
                if (currentState.playerRole == PlayerRole.BATTING) newPlayerWickets else newAiWickets
            } else 0

            val isInnings1Ended = currentState.currentInnings == 1 && (
                batsmanWickets >= currentState.matchWicketsLimit || newBallsBowled >= maxBalls
            )

            if (isInnings1Ended) {
                val currentInnings1Score = if (currentState.playerRole == PlayerRole.BATTING) newPlayerRuns else newAiRuns
                val targetScore = currentInnings1Score + 1
                val newRole = if (currentState.playerRole == PlayerRole.BATTING) PlayerRole.BOWLING else PlayerRole.BATTING

                _matchState.update {
                    it.copy(
                        currentInnings = 2,
                        playerRuns = newPlayerRuns,
                        playerWickets = newPlayerWickets,
                        aiRuns = newAiRuns,
                        aiWickets = newAiWickets,
                        ballsBowled = 0,
                        playerRole = newRole,
                        target = targetScore,
                        commentText = "Innings Break! Target is: $targetScore runs off $maxBalls balls!",
                        recentBalls = emptyList()
                    )
                }

                if (myNum == 1) {
                    multiplayerManager.updateInningsTransition(PlayerRole.BOWLING, PlayerRole.BATTING)
                }
                isResolvingMpBall = false
                startCountdownTimer()
                return@launch
            }

            // Check Innings 2 chase logic
            if (currentState.currentInnings == 2) {
                val target = currentState.target ?: 1
                val chaseScore = if (currentState.playerRole == PlayerRole.BATTING) newPlayerRuns else newAiRuns
                val chaseWickets = if (currentState.playerRole == PlayerRole.BATTING) newPlayerWickets else newAiWickets

                val isChaseSuccessful = chaseScore >= target
                val isInnings2Ended = chaseWickets >= currentState.matchWicketsLimit || newBallsBowled >= maxBalls

                if (isChaseSuccessful) {
                    val playerWon = currentState.playerRole == PlayerRole.BATTING
                    completeMatchFinish(
                        newPlayerRuns, newPlayerWickets, newAiRuns, newAiWickets, playerWon,
                        currentState, currentState.dotBallsBowledThisMatch + dotBallsThisBall,
                        currentState.sixesHitThisMatch + sixesHitThisBall
                    )
                    if (myNum == 1) {
                        multiplayerManager.resetRoomMoves(newBallsBowled)
                    }
                    isResolvingMpBall = false
                    return@launch
                } else if (isInnings2Ended) {
                    val playerWon = currentState.playerRole == PlayerRole.BOWLING
                    completeMatchFinish(
                        newPlayerRuns, newPlayerWickets, newAiRuns, newAiWickets, playerWon,
                        currentState, currentState.dotBallsBowledThisMatch + dotBallsThisBall,
                        currentState.sixesHitThisMatch + sixesHitThisBall
                    )
                    if (myNum == 1) {
                        multiplayerManager.resetRoomMoves(newBallsBowled)
                    }
                    isResolvingMpBall = false
                    return@launch
                }
            }

            // Normal ball update
            _matchState.update {
                it.copy(
                    playerRuns = newPlayerRuns,
                    playerWickets = newPlayerWickets,
                    aiRuns = newAiRuns,
                    aiWickets = newAiWickets,
                    ballsBowled = newBallsBowled,
                    dotBallsBowledThisMatch = it.dotBallsBowledThisMatch + dotBallsThisBall,
                    sixesHitThisMatch = it.sixesHitThisMatch + sixesHitThisBall,
                    recentBalls = updatedRecentBalls
                )
            }

            if (myNum == 1) {
                multiplayerManager.resetRoomMoves(newBallsBowled)
            }
            isResolvingMpBall = false
            startCountdownTimer()
        }
    }


    private suspend fun completeMatchFinish(
        playerRuns: Int,
        playerWickets: Int,
        aiRuns: Int,
        aiWickets: Int,
        playerWon: Boolean,
        currentState: MatchState,
        finalDots: Int,
        finalSixes: Int
    ) {
        val difficultyStr = currentState.difficulty.name
        val winText = if (playerWon) {
            "Match Completed: Victory! You successfully defeated AI Master!"
        } else {
            "Match Completed: Defeat! Better luck next time!"
        }

        val xpGainedValue = if (playerWon) 150 else 45
        val finalXp = xpGainedValue + playerRuns + (playerWickets * 10) + (finalDots * 2)

        _matchState.update {
            it.copy(
                phase = MatchPhase.COMPLETED,
                playerRuns = playerRuns,
                playerWickets = playerWickets,
                aiRuns = aiRuns,
                aiWickets = aiWickets,
                winMessage = winText,
                commentText = "Game Over! Congratulations on completing the tournament.",
                xpGained = finalXp
            )
        }

        // Write statistics inside Room database
        repository.recordCompletedMatch(
            playerRuns = playerRuns,
            playerWickets = playerWickets,
            aiRuns = aiRuns,
            aiWickets = aiWickets,
            isWin = playerWon,
            difficulty = difficultyStr,
            overs = currentState.matchOversLimit,
            wicketsLimit = currentState.matchWicketsLimit,
            dotBallsBowled = finalDots,
            sixesHit = finalSixes
        )
    }

    fun restartSetup() {
        timerJob?.cancel()
        if (_matchState.value.isMultiplayer) {
            multiplayerManager.disconnect()
        }
        _matchState.value = MatchState()
        navigateTo(Screen.MENU)
    }


    // Commentary generators
    private fun generateBattingCommentary(runs: Int): String {
        return when (runs) {
            6 -> listOf(
                "SHOT! High, high into the stellar sunset crowd for a massive SIX!",
                "UNBELIEVABLE! Cleared the stadium roof! That ball is out of the park!",
                "Glorious! Stand and deliver, an absolute monster of a six!",
                "Incredible timing! He lofts it straight over long-on for a huge maximum!"
            ).random()
            4 -> listOf(
                "Sizzling boundary! Pulled away beautifully between cover and point!",
                "Cracking shot! Raced across the outfield like a tracer bullet!",
                "Flawlessly timed! Pierces the gap through extra-cover for a premium four!",
                "Delicate touch! Sliced past third man, the fielder had no chance!"
            ).random()
            3 -> "Sensational running! Squeezed 3 runs off a crisp forward defense."
            2 -> "Classic double! Pushed gently into deep mid-on to work the field."
            1 -> "Sustained single. Gently rotated strike down to third man."
            else -> "Clean block! Solid defense but no run conceded."
        }
    }

    private fun generateBowlingCommentary(runs: Int): String {
        return when (runs) {
            6 -> listOf(
                "Smashed by AI! High above long-on for a cracking SIX!",
                "Oh no! AI connects perfectly and launches it over the midwicket boundary!",
                "Dispatched! AI moves across and scoops it over fine leg for six runs!"
            ).random()
            4 -> listOf(
                "AI pulls it away! Beautiful boundary through deep square leg.",
                "Shot! AI finds the gap at cover-point and it speeds away for four.",
                "Thumped! AI steps down the track and hits it hard through mid-off for boundary!"
            ).random()
            3 -> "Smart running by AI. Picked up three runs in deep cover."
            2 -> "AI slices it down. Runs hard to grab a comfortable couple."
            1 -> "AI taps it into the gap for a quick single."
            else -> "Superb dot ball! Perfect length beats the batsman's outside edge."
        }
    }

    private fun generateOutCommentary(runs: Int, role: PlayerRole): String {
        return if (role == PlayerRole.BATTING) {
            when (runs) {
                6 -> "OUT! Caught right on the boundary cushion trying to clear the ropes!"
                4 -> "OUT! Sharp catch at gully! Gilded edge cut off in style."
                3 -> "OUT! Run out! AI defender executes a lightning-fast throw!"
                else -> listOf(
                    "OUT! Stumps shattered! Clean bowled by a masterful delivery!",
                    "OUT! Plumb LBW! The batsman was caught dead in front of the stumps!",
                    "OUT! Big appeal, and it's caught behind by the wicketkeeper!"
                ).random()
            }
        } else {
            when (runs) {
                6 -> "WICKETS FLYING! AI mistimed it, cleanly caught at deep long-off!"
                4 -> "OUT! Brilliant diving catch by the keeper! AI is stunned."
                else -> listOf(
                    "BOWLED HIM! Absolute peach of a ball, the bails are dismantled!",
                    "OUT! AI gets a leading edge and is caught easily at mid-on!",
                    "OUT! Trapped in front! Absolute beauty of a delivery wickets the AI!"
                ).random()
            }
        }
    }
}
