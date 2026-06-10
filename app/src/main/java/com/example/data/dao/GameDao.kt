package com.example.data.dao

import androidx.room.*
import com.example.data.model.Achievement
import com.example.data.model.MatchHistory
import com.example.data.model.PlayerStats
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM player_stats WHERE id = 1 LIMIT 1")
    fun getPlayerStatsFlow(): Flow<PlayerStats?>

    @Query("SELECT * FROM player_stats WHERE id = 1 LIMIT 1")
    suspend fun getPlayerStatsDirect(): PlayerStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePlayerStats(stats: PlayerStats)

    @Query("SELECT * FROM match_history ORDER BY timestamp DESC")
    fun getMatchHistoryFlow(): Flow<List<MatchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchHistory(match: MatchHistory)

    @Query("SELECT * FROM achievement")
    fun getAchievementsFlow(): Flow<List<Achievement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<Achievement>)

    @Update
    suspend fun updateAchievement(achievement: Achievement)
}
