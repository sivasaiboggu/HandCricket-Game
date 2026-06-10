package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
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
    MENU, CONFIG, TOSS, PLAY, STATS, ACHIEVEMENTS, THEMES
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
    val xpGained: Int = 0
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    val playerStats: StateFlow<PlayerStats>
    val matchHistory: StateFlow<List<MatchHistory>>
    val achievements: StateFlow<List<Achievement>>

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
    }

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
                phase = MatchPhase.NOT_STARTED
            )
        }
        aiEngine.resetSession()
        _tossWinner.value = null
        _tossResult.value = ""
        navigateTo(Screen.TOSS)
    }

    // Stadium selection
    fun selectStadiumTheme(theme: StadiumTheme) {
        _matchState.update { it.copy(stadiumTheme = theme) }
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
                        commentText = "AI won the toss and elected to ${aiTossChoice.lowercase()} first!",
                        phase = MatchPhase.INNINGS_1
                    )
                }
                delay(1200)
                navigateTo(Screen.PLAY)
            }
        }
    }

    fun makeTossDecision(roleChoice: PlayerRole) {
        _matchState.update {
            it.copy(
                playerRole = roleChoice,
                commentText = "You won the toss and elected to ${roleChoice.name.lowercase()} first!",
                phase = MatchPhase.INNINGS_1
            )
        }
        navigateTo(Screen.PLAY)
    }

    // Play Ball action
    fun playBall(playerMove: Int) {
        if (_matchState.value.phase == MatchPhase.COMPLETED) return
        
        viewModelScope.launch {
            // Record player move in smart AI memory
            aiEngine.recordPlayerMove(playerMove)

            val currentState = _matchState.value
            val role = if (currentState.playerRole == PlayerRole.BATTING) ChoiceRole.BATSMAN else ChoiceRole.BOWLER
            val aiMove = aiEngine.generateAiMove(role, currentState.difficulty)

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
                    commentary = generateBattingCommentary(playerMove)
                    if (playerMove == 6) sixesHitThisBall++
                } else {
                    newAiRuns += aiMove
                    commentary = generateBowlingCommentary(aiMove)
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

            // Temporarily flag animation variables
            _matchState.update {
                it.copy(
                    lastPlayerMove = playerMove,
                    lastAiMove = aiMove,
                    isOutEvent = isOut,
                    isScoreEvent = isScored,
                    commentText = commentary
                )
            }

            delay(1200) // Wait for action shake-up and dynamic visuals

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
                        commentText = "Innings Break! Target is: $targetScore runs off $maxBalls balls!"
                    )
                }
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

            // Normal ball updates if innings goes on
            _matchState.update {
                it.copy(
                    playerRuns = newPlayerRuns,
                    playerWickets = newPlayerWickets,
                    aiRuns = newAiRuns,
                    aiWickets = newAiWickets,
                    ballsBowled = newBallsBowled,
                    dotBallsBowledThisMatch = it.dotBallsBowledThisMatch + dotBallsThisBall,
                    sixesHitThisMatch = it.sixesHitThisMatch + sixesHitThisBall
                )
            }
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
        _matchState.value = MatchState()
        navigateTo(Screen.MENU)
    }

    // Commentary generators
    private fun generateBattingCommentary(runs: Int): String {
        return when (runs) {
            6 -> "SHOT! High, high into the stellar sunset crowd for a massive SIX!"
            4 -> "Sizzling boundary! Pulled away beautifully between cover and point!"
            3 -> "Sensational running! Squeezed 3 runs off a crisp forward defense."
            2 -> "Classic double! Pushed gently into deep mid-on to work the field."
            1 -> "Sustained single. Gently rotated strike down to third man."
            else -> "Clean block! Solid defense but no run conceded."
        }
    }

    private fun generateBowlingCommentary(runs: Int): String {
        return when (runs) {
            6 -> "Smashed by AI! High above long-on for a cracking SIX!"
            4 -> "AI pulls it away! Beautiful boundary through deep square leg."
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
                3 -> "OUT! Mindplay match! Stunned right down the track."
                else -> "OUT! Stumps shattered! Clean bowled by a masterful delivery!"
            }
        } else {
            when (runs) {
                6 -> "WICKETS FLYING! AI mistimed it, cleanly caught at deep long-off!"
                4 -> "OUT! Brilliant diving catch by the keeper! AI is stunned."
                else -> "BOWLED HIM! Absolute peach of a ball, the bails are dismantled!"
            }
        }
    }
}
