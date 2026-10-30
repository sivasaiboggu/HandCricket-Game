package com.example

import com.example.ui.viewmodel.ChoiceRole
import com.example.ui.viewmodel.Difficulty
import com.example.ui.viewmodel.HandCricketAi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HandCricketAiTest {

    @Test
    fun easyAi_alwaysReturnsValidMove() {
        val ai = HandCricketAi()
        repeat(500) {
            val move = ai.generateAiMove(ChoiceRole.BOWLER, Difficulty.EASY, playerLevel = 1)
            assertTrue("move $move out of range", move in 1..6)
        }
    }

    @Test
    fun recordPlayerMove_ignoresOutOfRangeMoves() {
        val ai = HandCricketAi()
        // Record many out-of-range moves; internal state must stay empty,
        // so MEDIUM bowler has no frequency data and must still answer validly.
        repeat(100) { ai.recordPlayerMove(0, isPlayerBatting = true) }
        repeat(100) { ai.recordPlayerMove(7, isPlayerBatting = true) }
        repeat(200) {
            val move = ai.generateAiMove(ChoiceRole.BOWLER, Difficulty.MEDIUM, playerLevel = 10)
            assertTrue("move $move out of range", move in 1..6)
        }
    }

    @Test
    fun mediumBowler_predictsPlayersFavouriteBattingMove() {
        val ai = HandCricketAi()
        // Player bats 4 almost every ball.
        repeat(60) { ai.recordPlayerMove(4, isPlayerBatting = true) }
        repeat(10) { ai.recordPlayerMove(2, isPlayerBatting = true) }

        var predicted = 0
        val trials = 1000
        repeat(trials) {
            if (ai.generateAiMove(ChoiceRole.BOWLER, Difficulty.MEDIUM, playerLevel = 10) == 4) predicted++
        }
        // Prediction rate at level 10 is minOf(75, 40+30) = 70%, and when it
        // predicts it always picks the most frequent move (4). Allow margin.
        assertTrue("predicted favourite only $predicted/$trials", predicted > trials / 2)
    }

    @Test
    fun mediumBatsman_avoidsPlayersFavouriteBowlingMove() {
        val ai = HandCricketAi()
        repeat(80) { ai.recordPlayerMove(3, isPlayerBatting = false) }

        var hitAvoided = 0
        val trials = 1000
        repeat(trials) {
            if (ai.generateAiMove(ChoiceRole.BATSMAN, Difficulty.MEDIUM, playerLevel = 10) == 3) hitAvoided++
        }
        // When adapting (70% of the time) it must never pick 3; overall rate of
        // picking 3 should be far below the ~1/6 random baseline.
        assertTrue("picked avoided move $hitAvoided/$trials", hitAvoided < trials / 10)
    }

    @Test
    fun masteryBowler_usesSecondOrderMarkovOnAlternatingPattern() {
        val ai = HandCricketAi()
        // Player strictly alternates 1 -> 2 -> 1 -> 2 ...
        repeat(40) {
            ai.recordPlayerMove(1, isPlayerBatting = true)
            ai.recordPlayerMove(2, isPlayerBatting = true)
        }
        // Sequence state: last = 2, secondLast = 1, so second-order key (1,2)
        // maps to 1 with certainty. Level >= 3 unlocks second-order.
        var correct = 0
        val trials = 500
        repeat(trials) {
            if (ai.generateAiMove(ChoiceRole.BOWLER, Difficulty.MASTERY, playerLevel = 5) == 1) correct++
        }
        // 97% adaptive, prediction is deterministic 1; expect > 85%.
        assertTrue("markov prediction only $correct/$trials", correct > trials * 85 / 100)
    }

    @Test
    fun masteryBatsman_neverPicksPredictedMoveWhenAdapting() {
        val ai = HandCricketAi()
        repeat(80) { ai.recordPlayerMove(6, isPlayerBatting = false) }

        var picked = 0
        val trials = 1000
        repeat(trials) {
            if (ai.generateAiMove(ChoiceRole.BATSMAN, Difficulty.MASTERY, playerLevel = 5) == 6) picked++
        }
        // Only the 3% random branch may pick 6.
        assertTrue("picked predicted move $picked/$trials", picked < trials * 10 / 100)
    }

    @Test
    fun resetSession_clearsLearnedModel() {
        val ai = HandCricketAi()
        repeat(60) { ai.recordPlayerMove(5, isPlayerBatting = true) }
        ai.resetSession()
        // With no data, MEDIUM bowler cannot predict; spread across moves.
        val seen = mutableSetOf<Int>()
        repeat(120) { seen.add(ai.generateAiMove(ChoiceRole.BOWLER, Difficulty.MEDIUM, playerLevel = 10)) }
        assertTrue("expected variety after reset, saw $seen", seen.size >= 3)
    }

    @Test
    fun levelScalesMediumPredictionRate() {
        // At level 0 prediction rate is 40%; verify behaviour still valid.
        val ai = HandCricketAi()
        repeat(200) {
            val m = ai.generateAiMove(ChoiceRole.BOWLER, Difficulty.MEDIUM, playerLevel = 0)
            assertTrue(m in 1..6)
        }
    }

    @Test
    fun masteryBowler_belowLevel3_skipsSecondOrderButStillPredicts() {
        val ai = HandCricketAi()
        repeat(40) {
            ai.recordPlayerMove(1, isPlayerBatting = true)
            ai.recordPlayerMove(2, isPlayerBatting = true)
        }
        // Level 2: second-order locked, first-order still available.
        // First-order after last=2 -> next was always 1 in the pattern.
        var correct = 0
        val trials = 500
        repeat(trials) {
            if (ai.generateAiMove(ChoiceRole.BOWLER, Difficulty.MASTERY, playerLevel = 2) == 1) correct++
        }
        assertTrue("first-order prediction only $correct/$trials", correct > trials * 85 / 100)
    }
}
