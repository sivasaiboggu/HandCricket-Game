package com.example.ui.viewmodel

import kotlin.random.Random

enum class Difficulty {
    EASY, MEDIUM, MASTERY
}

enum class ChoiceRole {
    BATSMAN, BOWLER
}

class HandCricketAi {
    
    // Separate frequencies of player moves when batting vs when bowling
    private val playerBattingFrequencies = mutableMapOf<Int, Int>()
    private val playerBowlingFrequencies = mutableMapOf<Int, Int>()
    
    // Context-sensitive transitions for Markov predictions (previous move -> next move -> count)
    private val battingTransitionFrequencies = mutableMapOf<Int, MutableMap<Int, Int>>()
    private val bowlingTransitionFrequencies = mutableMapOf<Int, MutableMap<Int, Int>>()
    
    private var lastPlayerBattingMove: Int? = null
    private var lastPlayerBowlingMove: Int? = null

    /**
     * Record the player's choice to train the AI's adaptive model in real-time,
     * separating their behaviors as a batsman vs. a bowler.
     */
    fun recordPlayerMove(move: Int, isPlayerBatting: Boolean) {
        if (move !in 1..6) return

        if (isPlayerBatting) {
            playerBattingFrequencies[move] = (playerBattingFrequencies[move] ?: 0) + 1
            val lastMove = lastPlayerBattingMove
            if (lastMove != null) {
                val transitions = battingTransitionFrequencies.getOrPut(lastMove) { mutableMapOf() }
                transitions[move] = (transitions[move] ?: 0) + 1
            }
            lastPlayerBattingMove = move
        } else {
            playerBowlingFrequencies[move] = (playerBowlingFrequencies[move] ?: 0) + 1
            val lastMove = lastPlayerBowlingMove
            if (lastMove != null) {
                val transitions = bowlingTransitionFrequencies.getOrPut(lastMove) { mutableMapOf() }
                transitions[move] = (transitions[move] ?: 0) + 1
            }
            lastPlayerBowlingMove = move
        }
    }

    /**
     * Resets sequence files at the start of a match to keep predictions fresh.
     */
    fun resetSession() {
        lastPlayerBattingMove = null
        lastPlayerBowlingMove = null
        playerBattingFrequencies.clear()
        playerBowlingFrequencies.clear()
        battingTransitionFrequencies.clear()
        bowlingTransitionFrequencies.clear()
    }

    /**
     * Calculates the AI's move based on role (Batting/Bowling) and Difficulty Level.
     */
    fun generateAiMove(role: ChoiceRole, difficulty: Difficulty): Int {
        return when (difficulty) {
            Difficulty.EASY -> {
                // Rookie AI: Completely random choices, very easy to beat.
                Random.nextInt(1, 7)
            }
            Difficulty.MEDIUM -> {
                // Professional AI: 60% random, 40% smart prediction using overall move frequencies.
                val randomPercent = Random.nextInt(100)
                if (randomPercent < 60) {
                    Random.nextInt(1, 7)
                } else {
                    if (role == ChoiceRole.BOWLER) {
                        // AI is bowling -> wants to guess player's batting choice.
                        val predictedMove = playerBattingFrequencies.maxByOrNull { it.value }?.key
                        predictedMove ?: Random.nextInt(1, 7)
                    } else {
                        // AI is batting -> wants to avoid player's bowling guess.
                        val avoidMove = playerBowlingFrequencies.maxByOrNull { it.value }?.key
                        if (avoidMove != null) {
                            val safeChoices = (1..6).filter { it != avoidMove }
                            if (safeChoices.isNotEmpty()) safeChoices.random() else Random.nextInt(1, 7)
                        } else {
                            Random.nextInt(1, 7)
                        }
                    }
                }
            }
            Difficulty.MASTERY -> {
                // Genius AI: 12% random, 88% adaptive Markov prediction.
                val randomPercent = Random.nextInt(100)
                if (randomPercent < 12) {
                    return Random.nextInt(1, 7)
                }

                var predictedPlayerMove: Int? = null

                if (role == ChoiceRole.BOWLER) {
                    // AI is bowling -> predicts player's batting move to bowl them out
                    val lastMove = lastPlayerBattingMove
                    if (lastMove != null) {
                        val transitions = battingTransitionFrequencies[lastMove]
                        if (!transitions.isNullOrEmpty()) {
                            predictedPlayerMove = transitions.maxByOrNull { it.value }?.key
                        }
                    }
                    if (predictedPlayerMove == null && playerBattingFrequencies.isNotEmpty()) {
                        predictedPlayerMove = playerBattingFrequencies.maxByOrNull { it.value }?.key
                    }
                    
                    // Predict and match player's move
                    predictedPlayerMove ?: Random.nextInt(1, 7)
                } else {
                    // AI is batting -> predicts player's bowling move to avoid it
                    val lastMove = lastPlayerBowlingMove
                    if (lastMove != null) {
                        val transitions = bowlingTransitionFrequencies[lastMove]
                        if (!transitions.isNullOrEmpty()) {
                            predictedPlayerMove = transitions.maxByOrNull { it.value }?.key
                        }
                    }
                    if (predictedPlayerMove == null && playerBowlingFrequencies.isNotEmpty()) {
                        predictedPlayerMove = playerBowlingFrequencies.maxByOrNull { it.value }?.key
                    }
                    
                    val avoidMove = predictedPlayerMove ?: Random.nextInt(1, 7)
                    // Pick a safe move that is not predicted, prioritizing higher scoring options.
                    val safeHighRuns = listOf(4, 6, 3, 5, 2, 1).filter { it != avoidMove }
                    if (safeHighRuns.isNotEmpty()) safeHighRuns.first() else Random.nextInt(1, 7)
                }
            }
        }
    }
}
