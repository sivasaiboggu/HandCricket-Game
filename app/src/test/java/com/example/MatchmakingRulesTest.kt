package com.example

import com.example.data.OnlineRoom
import com.example.data.WAITING_ROOM_TTL_MS
import com.example.data.isRoomJoinable
import com.example.data.isRoomStale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchmakingRulesTest {

    private val now = 1_000_000L

    @Test
    fun roomWithNoTimestampYet_isNotStale() {
        assertFalse(isRoomStale(createdAt = 0L, now = now))
    }

    @Test
    fun freshRoom_isNotStale() {
        assertFalse(isRoomStale(createdAt = now - WAITING_ROOM_TTL_MS + 1, now = now))
    }

    @Test
    fun oldRoom_isStale() {
        assertTrue(isRoomStale(createdAt = now - WAITING_ROOM_TTL_MS - 1, now = now))
    }

    @Test
    fun waitingEmptyFreshRoom_isJoinable() {
        val room = OnlineRoom(status = "WAITING", player1Id = "a", createdAt = now - 1000)
        assertTrue(isRoomJoinable(room, now))
    }

    @Test
    fun staleWaitingRoom_isNotJoinable() {
        val room = OnlineRoom(status = "WAITING", player1Id = "a", createdAt = now - WAITING_ROOM_TTL_MS - 1)
        assertFalse(isRoomJoinable(room, now))
    }

    @Test
    fun occupiedRoom_isNotJoinable() {
        val room = OnlineRoom(status = "WAITING", player1Id = "a", player2Id = "b", createdAt = now - 1000)
        assertFalse(isRoomJoinable(room, now))
    }

    @Test
    fun playingRoom_isNotJoinable() {
        val room = OnlineRoom(status = "PLAYING", player1Id = "a", createdAt = now - 1000)
        assertFalse(isRoomJoinable(room, now))
    }

    @Test
    fun abandonedRoom_isNotJoinable() {
        val room = OnlineRoom(status = "ABANDONED", player1Id = "a", createdAt = now - 1000)
        assertFalse(isRoomJoinable(room, now))
    }
}
