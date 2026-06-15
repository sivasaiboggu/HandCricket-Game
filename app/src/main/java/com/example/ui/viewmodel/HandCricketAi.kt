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
    
    // First-order transitions: (previous move -> next move -> count)
    private val battingTransitionFrequencies = mutableMapOf<Int, MutableMap<Int, Int>>()
    private val bowlingTransitionFrequencies = mutableMapOf<Int, MutableMap<Int, Int>>()
    
    // Second-order transitions: (previous two moves -> next move -> count)
    private val battingSecondOrderTransitions = mutableMapOf<Pair<Int, Int>, MutableMap<Int, Int>>()
    private val bowlingSecondOrderTransitions = mutableMapOf<Pair<Int, Int>, MutableMap<Int, Int>>()
    
    private var lastPlayerBattingMove: Int? = null
    private var lastPlayerBowlingMove: Int? = null
    private var secondLastPlayerBattingMove: Int? = null
    private var secondLastPlayerBowlingMove: Int? = null

    /**
     * Record the player's choice to train the AI's adaptive model in real-time,
     * separating their behaviors as a batsman vs. a bowler, and capturing second-order transitions.
     */
    fun recordPlayerMove(move: Int, isPlayerBatting: Boolean) {
        if (move !in 1..6) return

        if (isPlayerBatting) {
            playerBattingFrequencies[move] = (playerBattingFrequencies[move] ?: 0) + 1
            
            val lastMove = lastPlayerBattingMove
            if (lastMove != null) {
                // First-order transition
                val transitions = battingTransitionFrequencies.getOrPut(lastMove) { mutableMapOf() }
                transitions[move] = (transitions[move] ?: 0) + 1
                
                // Second-order transition
                val secondLast = secondLastPlayerBattingMove
                if (secondLast != null) {
                    val key = Pair(secondLast, lastMove)
                    val secondTransitions = battingSecondOrderTransitions.getOrPut(key) { mutableMapOf() }
                    secondTransitions[move] = (secondTransitions[move] ?: 0) + 1
                }
            }
            secondLastPlayerBattingMove = lastPlayerBattingMove
            lastPlayerBattingMove = move
        } else {
            playerBowlingFrequencies[move] = (playerBowlingFrequencies[move] ?: 0) + 1
            
            val lastMove = lastPlayerBowlingMove
            if (lastMove != null) {
                // First-order transition
                val transitions = bowlingTransitionFrequencies.getOrPut(lastMove) { mutableMapOf() }
                transitions[move] = (transitions[move] ?: 0) + 1
                
                // Second-order transition
                val secondLast = secondLastPlayerBowlingMove
                if (secondLast != null) {
                    val key = Pair(secondLast, lastMove)
                    val secondTransitions = bowlingSecondOrderTransitions.getOrPut(key) { mutableMapOf() }
                    secondTransitions[move] = (secondTransitions[move] ?: 0) + 1
                }
            }
            secondLastPlayerBowlingMove = lastPlayerBowlingMove
            lastPlayerBowlingMove = move
        }
    }

    /**
     * Resets sequence files at the start of a match to keep predictions fresh.
     */
    fun resetSession() {
        lastPlayerBattingMove = null
        lastPlayerBowlingMove = null
        secondLastPlayerBattingMove = null
        secondLastPlayerBowlingMove = null
        playerBattingFrequencies.clear()
        playerBowlingFrequencies.clear()
        battingTransitionFrequencies.clear()
        bowlingTransitionFrequencies.clear()
        battingSecondOrderTransitions.clear()
        bowlingSecondOrderTransitions.clear()
    }

    /**
     * Calculates the AI's move based on role (Batting/Bowling), Difficulty Level, and Player's career Level.
     */
    fun generateAiMove(role: ChoiceRole, difficulty: Difficulty, playerLevel: Int): Int {
        return when (difficulty) {
            Difficulty.EASY -> {
                // Rookie AI: Completely random choices, very easy to beat.
                Random.nextInt(1, 7)
            }
            Difficulty.MEDIUM -> {
                // Professional AI: prediction rate scales from 40% up to 75% capped based on playerLevel
                val predictionRate = minOf(75, 40 + (playerLevel * 3))
                val randomPercent = Random.nextInt(100)
                if (randomPercent >= predictionRate) {
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
                // Genius AI: 3% random, 97% adaptive hybrid prediction (First-order & Second-order Markov).
                val randomPercent = Random.nextInt(100)
                if (randomPercent < 3) {
                    return Random.nextInt(1, 7)
                }

                var predictedPlayerMove: Int? = null

                if (role == ChoiceRole.BOWLER) {
                    // AI is bowling -> predicts player's batting move to bowl them out
                    val last = lastPlayerBattingMove
                    val secondLast = secondLastPlayerBattingMove
                    
                    // 1. Try Second-Order Markov prediction (unlocked at Level 3+)
                    if (playerLevel >= 3 && secondLast != null && last != null) {
                        val key = Pair(secondLast, last)
                        val secondTransitions = battingSecondOrderTransitions[key]
                        if (!secondTransitions.isNullOrEmpty()) {
                            predictedPlayerMove = secondTransitions.maxByOrNull { it.value }?.key
                        }
                    }
                    
                    // 2. Fall back to First-Order Markov prediction
                    if (predictedPlayerMove == null && last != null) {
                        val transitions = battingTransitionFrequencies[last]
                        if (!transitions.isNullOrEmpty()) {
                            predictedPlayerMove = transitions.maxByOrNull { it.value }?.key
                        }
                    }
                    
                    // 3. Fall back to General overall frequency prediction
                    if (predictedPlayerMove == null && playerBattingFrequencies.isNotEmpty()) {
                        predictedPlayerMove = playerBattingFrequencies.maxByOrNull { it.value }?.key
                    }
                    
                    predictedPlayerMove ?: Random.nextInt(1, 7)
                } else {
                    // AI is batting -> predicts player's bowling move to avoid it
                    val last = lastPlayerBowlingMove
                    val secondLast = secondLastPlayerBowlingMove
                    
                    // 1. Try Second-Order Markov prediction (unlocked at Level 3+)
                    if (playerLevel >= 3 && secondLast != null && last != null) {
                        val key = Pair(secondLast, last)
                        val secondTransitions = bowlingSecondOrderTransitions[key]
                        if (!secondTransitions.isNullOrEmpty()) {
                            predictedPlayerMove = secondTransitions.maxByOrNull { it.value }?.key
                        }
                    }
                    
                    // 2. Fall back to First-Order Markov prediction
                    if (predictedPlayerMove == null && last != null) {
                        val transitions = bowlingTransitionFrequencies[last]
                        if (!transitions.isNullOrEmpty()) {
                            predictedPlayerMove = transitions.maxByOrNull { it.value }?.key
                        }
                    }
                    
                    // 3. Fall back to General overall frequency prediction
                    if (predictedPlayerMove == null && playerBowlingFrequencies.isNotEmpty()) {
                        predictedPlayerMove = playerBowlingFrequencies.maxByOrNull { it.value }?.key
                    }
                    
                    val avoidMove = predictedPlayerMove ?: Random.nextInt(1, 7)
                    
                    // Pick a safe move that is not predicted, prioritizing higher runs dynamically based on playerLevel
                    val safeChoices = (1..6).filter { it != avoidMove }
                    if (safeChoices.isNotEmpty()) {
                        val weights = safeChoices.map { move ->
                            when (move) {
                                6 -> 5 + playerLevel
                                4 -> 4 + playerLevel
                                5 -> 3 + (playerLevel / 2)
                                3 -> 3
                                2 -> 2
                                1 -> 1
                                else -> 1
                            }
                        }
                        val totalWeight = weights.sum()
                        var randomWeight = Random.nextInt(totalWeight)
                        var selectedMove = safeChoices.first()
                        for (i in safeChoices.indices) {
                            randomWeight -= weights[i]
                            if (randomWeight < 0) {
                                selectedMove = safeChoices[i]
                                break
                            }
                        }
                        selectedMove
                    } else {
                        Random.nextInt(1, 7)
                    }
                }
            }
        }
    }
}
