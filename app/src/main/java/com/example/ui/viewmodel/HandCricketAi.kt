package com.example.ui.viewmodel

import kotlin.random.Random

enum class Difficulty {
    EASY, MEDIUM, MASTERY
}

enum class ChoiceRole {
    BATSMAN, BOWLER
}

class HandCricketAi {
    
    // Overall frequencies of player inputs (1 to 6)
    private val playerMoveFrequencies = mutableMapOf<Int, Int>()
    
    // Context-sensitive transition counts for Markov predictive scoring
    // Key is previous move (1-6), value is map of subsequent moves and their counts
    private val transitionFrequencies = mutableMapOf<Int, MutableMap<Int, Int>>()
    
    private var lastPlayerMove: Int? = null

    /**
     * Record the player's choice to train the AI's adaptive model in real-time.
     */
    fun recordPlayerMove(move: Int) {
        if (move !in 1..6) return

        playerMoveFrequencies[move] = (playerMoveFrequencies[move] ?: 0) + 1

        val lastMove = lastPlayerMove
        if (lastMove != null) {
            val transitions = transitionFrequencies.getOrPut(lastMove) { mutableMapOf() }
            transitions[move] = (transitions[move] ?: 0) + 1
        }
        
        lastPlayerMove = move
    }

    /**
     * Resets sequence files at the start of a match to keep prediction fresh.
     */
    fun resetSession() {
        lastPlayerMove = null
        playerMoveFrequencies.clear()
        transitionFrequencies.clear()
    }

    /**
     * Calculates the AI's move based on role (Batting/Bowling) and Difficulty Level.
     */
    fun generateAiMove(role: ChoiceRole, difficulty: Difficulty): Int {
        return when (difficulty) {
            Difficulty.EASY -> {
                // Completely random 1..6
                Random.nextInt(1, 7)
            }
            Difficulty.MEDIUM -> {
                // 70% random, 30% smart bias towards player's overall favorite move (when bowling)
                // or avoiding player's bowling hot targets (when batting)
                val randomPercent = Random.nextInt(100)
                if (randomPercent < 70 || playerMoveFrequencies.isEmpty()) {
                    Random.nextInt(1, 7)
                } else {
                    val favoriteMove = playerMoveFrequencies.maxByOrNull { it.value }?.key ?: Random.nextInt(1, 7)
                    if (role == ChoiceRole.BOWLER) {
                        // AI is bowling, wants to guess the batsman's move to get them out
                        favoriteMove
                    } else {
                        // AI is batting, wants to avoid being matched by player's bowler tendencies
                        // Pick something that is NOT the player's favorite bowling guess
                        val safeChoices = (1..6).filter { it != favoriteMove }
                        if (safeChoices.isNotEmpty()) safeChoices.random() else Random.nextInt(1, 7)
                    }
                }
            }
            Difficulty.MASTERY -> {
                // Adaptive Mastery Mode: use 1st-level Markov prediction block or base frequencies
                val randomPercent = Random.nextInt(100)
                
                // 40% of the time, make a classic random choice to avoid being entirely deterministic
                if (randomPercent < 40) {
                    return Random.nextInt(1, 7)
                }

                val lastMove = lastPlayerMove
                var predictedMove: Int? = null

                if (lastMove != null) {
                    // Look up transition context (what player usually plays after lastMove)
                    val transitions = transitionFrequencies[lastMove]
                    if (!transitions.isNullOrEmpty()) {
                        predictedMove = transitions.maxByOrNull { it.value }?.key
                    }
                }

                // If transition forecast is null, fall back to global favorite move
                if (predictedMove == null && playerMoveFrequencies.isNotEmpty()) {
                    predictedMove = playerMoveFrequencies.maxByOrNull { it.value }?.key
                }

                // Default fallback if no data recorded yet
                val prediction = predictedMove ?: Random.nextInt(1, 7)

                if (role == ChoiceRole.BOWLER) {
                    // AI is bowling -> wants to match player's batting choice to bowl them OUT
                    prediction
                } else {
                    // AI is batting -> wants to avoid the player's bowling choice
                    // AI tries to predict what player will throw as bowler, and plays something else
                    // Also batsmen try to score high run values like 4 and 6
                    val avoidMove = prediction
                    val highRunsWeighted = listOf(4, 6, 3, 5, 2, 1)
                    val candidate = highRunsWeighted.firstOrNull { it != avoidMove } ?: Random.nextInt(1, 7)
                    candidate
                }
            }
        }
    }
}
