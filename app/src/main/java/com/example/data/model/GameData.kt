package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_stats")
data class PlayerStats(
    @PrimaryKey val id: Int = 1,
    val level: Int = 1,
    val exp: Int = 0,
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,
    val matchesLost: Int = 0,
    val runsScored: Int = 0,
    val wicketsTaken: Int = 0,
    val highestScore: Int = 0,
    val dotBallsBowled: Int = 0,
    val sixesHit: Int = 0,
    val themeUnlockedSunset: Boolean = false,
    val themeUnlockedCyber: Boolean = false
) {
    val winRate: Float
        get() = if (matchesPlayed > 0) (matchesWon.toFloat() / matchesPlayed) * 100f else 0f

    val averageScore: Float
        get() = if (matchesPlayed > 0) runsScored.toFloat() / matchesPlayed else 0f
}

@Entity(tableName = "match_history")
data class MatchHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playerRuns: Int,
    val playerWickets: Int,
    val aiRuns: Int,
    val aiWickets: Int,
    val isWin: Boolean,
    val difficulty: String,
    val oversLimit: Int,
    val wicketsLimit: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "achievement")
data class Achievement(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val requiredProgress: Int = 1,
    val currentProgress: Int = 0,
    val type: String // "RUNS", "WINS", "STRIKE", "WICKETS"
)
