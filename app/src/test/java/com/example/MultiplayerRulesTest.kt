package com.example

import com.example.data.OnlineRoom
import com.example.ui.viewmodel.PlayerRole
import com.example.ui.viewmodel.classifyMultiplayerBall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiplayerRulesTest {

    @Test
    fun roomDefaults_movesUseUnsubmittedSentinel() {
        // -1 means "not submitted"; 0 is a real submitted timeout and must resolve.
        val room = OnlineRoom()
        assertEquals(-1, room.player1Move)
        assertEquals(-1, room.player2Move)
    }

    @Test
    fun equalNonZeroMoves_areOut() {
        val o = classifyMultiplayerBall(4, 4, currentInnings = 1, myRole = PlayerRole.BATTING)
        assertTrue(o.isOut)
        assertEquals(4, o.runs)
    }

    @Test
    fun doubleTimeout_isDotNotWicket() {
        val o = classifyMultiplayerBall(0, 0, currentInnings = 1, myRole = PlayerRole.BATTING)
        assertFalse(o.isOut)
        assertEquals(0, o.runs)
    }

    @Test
    fun batsmanTimeout_isNotOut() {
        val o = classifyMultiplayerBall(0, 3, currentInnings = 1, myRole = PlayerRole.BOWLING)
        assertFalse(o.isOut)
        assertEquals(0, o.runs)
    }

    @Test
    fun innings1_runsComeFromPlayer1() {
        val o = classifyMultiplayerBall(5, 2, currentInnings = 1, myRole = PlayerRole.BOWLING)
        assertEquals(5, o.runs)
    }

    @Test
    fun innings2_runsComeFromPlayer2() {
        val o = classifyMultiplayerBall(5, 2, currentInnings = 2, myRole = PlayerRole.BATTING)
        assertEquals(2, o.runs)
    }

    @Test
    fun six_creditedOnlyWhenIBatted() {
        val mine = classifyMultiplayerBall(6, 3, currentInnings = 1, myRole = PlayerRole.BATTING)
        assertTrue(mine.isSixForMe)
        val opponents = classifyMultiplayerBall(6, 3, currentInnings = 1, myRole = PlayerRole.BOWLING)
        assertFalse(opponents.isSixForMe)
    }

    @Test
    fun dot_creditedOnlyWhenIBowled() {
        val mine = classifyMultiplayerBall(0, 2, currentInnings = 1, myRole = PlayerRole.BOWLING)
        assertTrue(mine.isDotForMe)
        val notMine = classifyMultiplayerBall(0, 2, currentInnings = 1, myRole = PlayerRole.BATTING)
        assertFalse(notMine.isDotForMe)
    }

    @Test
    fun wicket_creditsNoSixOrDot() {
        val o = classifyMultiplayerBall(6, 6, currentInnings = 2, myRole = PlayerRole.BATTING)
        assertTrue(o.isOut)
        assertFalse(o.isSixForMe)
        assertFalse(o.isDotForMe)
    }
}
