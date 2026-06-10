package com.example.data.repository

import com.example.data.dao.GameDao
import com.example.data.model.Achievement
import com.example.data.model.MatchHistory
import com.example.data.model.PlayerStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class GameRepository(private val gameDao: GameDao) {

    val playerStats: Flow<PlayerStats> = gameDao.getPlayerStatsFlow().map { it ?: PlayerStats() }
    val matchHistory: Flow<List<MatchHistory>> = gameDao.getMatchHistoryFlow()
    val achievements: Flow<List<Achievement>> = gameDao.getAchievementsFlow()

    suspend fun initializeDataIfNeeded() {
        // Run checks to ensure default player stats and achievements exist
        val stats = gameDao.getPlayerStatsDirect()
        if (stats == null) {
            gameDao.insertOrUpdatePlayerStats(PlayerStats())
        }

        val existingAchievements = gameDao.getAchievementsFlow().firstOrNull() ?: emptyList()
        if (existingAchievements.isEmpty()) {
            val defaultAchievements = listOf(
                Achievement("CH_01_FIRST_STEPS", "First Steps", "Play your first hand cricket match", false, 1, 0, "MATCH"),
                Achievement("CH_02_CENTURY_RUNS", "Run Machine", "Score 100 runs in your career", false, 100, 0, "RUNS"),
                Achievement("CH_03_BOWLED_OVER", "Spin Wizard", "Take 15 AI wickets in total", false, 15, 0, "WICKETS"),
                Achievement("CH_04_MASTERY", "Supreme Master", "Win a match on Mastery difficulty", false, 1, 0, "MASTERY"),
                Achievement("CH_05_HALF_CENTURY", "Unstoppable Force", "Score 30+ runs in a single innings", false, 1, 0, "STRIKE"),
                Achievement("CH_06_PERFECT_DOT", "Golden Bowl", "Bowl 10 dot balls in total", false, 10, 0, "DOT"),
                Achievement("CH_07_LEVEL_3", "Rising Star", "Reach Level 3 (needs 500 XP total)", false, 3, 0, "LEVEL")
            )
            gameDao.insertAchievements(defaultAchievements)
        }
    }

    suspend fun recordCompletedMatch(
        playerRuns: Int,
        playerWickets: Int,
        aiRuns: Int,
        aiWickets: Int,
        isWin: Boolean,
        difficulty: String,
        overs: Int,
        wicketsLimit: Int,
        dotBallsBowled: Int,
        sixesHit: Int
    ) {
        // Insert history record
        val history = MatchHistory(
            playerRuns = playerRuns,
            playerWickets = playerWickets,
            aiRuns = aiRuns,
            aiWickets = aiWickets,
            isWin = isWin,
            difficulty = difficulty,
            oversLimit = overs,
            wicketsLimit = wicketsLimit
        )
        gameDao.insertMatchHistory(history)

        // Calculate XP
        // Win gets 150XP, loss gets 40XP
        // Plus 1 XP per run, 10 XP per wicket, 2 XP per dot ball
        val baseXp = if (isWin) 150 else 40
        val earnedXp = baseXp + playerRuns + (playerWickets * 10) + (dotBallsBowled * 2)

        // Fetch current stats to modify
        val currentStats = gameDao.getPlayerStatsDirect() ?: PlayerStats()
        var newExp = currentStats.exp + earnedXp
        var newLevel = currentStats.level
        
        // Level equation: level * 250 XP required for next level
        // Level 1: 0 to 250 XP
        // Level 2: 251 to 500 XP (needs +500)
        // Level 3: 501 to 1000 XP (needs +1000)
        // Let's do simple level threshold: lvl * 200 XP
        while (newExp >= newLevel * 250) {
            newExp -= newLevel * 250
            newLevel++
        }

        // Check theme unlocks based on Level
        val unlockSunset = currentStats.themeUnlockedSunset || (newLevel >= 2)
        val unlockCyber = currentStats.themeUnlockedCyber || (newLevel >= 3)

        val updatedStats = currentStats.copy(
            level = newLevel,
            exp = newExp,
            matchesPlayed = currentStats.matchesPlayed + 1,
            matchesWon = currentStats.matchesWon + (if (isWin) 1 else 0),
            matchesLost = currentStats.matchesLost + (if (isWin) 0 else 1),
            runsScored = currentStats.runsScored + playerRuns,
            wicketsTaken = currentStats.wicketsTaken + playerWickets,
            highestScore = maxOf(currentStats.highestScore, playerRuns),
            dotBallsBowled = currentStats.dotBallsBowled + dotBallsBowled,
            sixesHit = currentStats.sixesHit + sixesHit,
            themeUnlockedSunset = unlockSunset,
            themeUnlockedCyber = unlockCyber
        )
        gameDao.insertOrUpdatePlayerStats(updatedStats)

        // Evaluate and update Achievements
        val currentAchievements = gameDao.getAchievementsFlow().firstOrNull() ?: emptyList()
        currentAchievements.forEach { ach ->
            if (!ach.isUnlocked) {
                var progress = ach.currentProgress
                var unlocked = false

                when (ach.id) {
                    "CH_01_FIRST_STEPS" -> {
                        progress = updatedStats.matchesPlayed
                        if (progress >= ach.requiredProgress) unlocked = true
                    }
                    "CH_02_CENTURY_RUNS" -> {
                        progress = updatedStats.runsScored
                        if (progress >= ach.requiredProgress) unlocked = true
                    }
                    "CH_03_BOWLED_OVER" -> {
                        progress = updatedStats.wicketsTaken
                        if (progress >= ach.requiredProgress) unlocked = true
                    }
                    "CH_04_MASTERY" -> {
                        if (isWin && difficulty.uppercase() == "MASTERY") {
                            progress = 1
                            unlocked = true
                        }
                    }
                    "CH_05_HALF_CENTURY" -> {
                        if (playerRuns >= 30) {
                            progress = 1
                            unlocked = true
                        }
                    }
                    "CH_06_PERFECT_DOT" -> {
                        progress = updatedStats.dotBallsBowled
                        if (progress >= ach.requiredProgress) unlocked = true
                    }
                    "CH_07_LEVEL_3" -> {
                        progress = updatedStats.level
                        if (progress >= ach.requiredProgress) unlocked = true
                    }
                }

                gameDao.updateAchievement(
                    ach.copy(
                        currentProgress = minOf(progress, ach.requiredProgress),
                        isUnlocked = unlocked
                    )
                )
            }
        }
    }
}
