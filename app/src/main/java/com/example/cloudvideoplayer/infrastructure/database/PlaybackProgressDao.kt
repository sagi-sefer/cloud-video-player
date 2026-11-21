package com.example.cloudvideoplayer.infrastructure.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackProgressDao {
    @Query("SELECT * FROM playback_progress WHERE videoId = :videoId")
    suspend fun getProgress(videoId: String): PlaybackProgressEntity?

    @Query("SELECT * FROM playback_progress WHERE videoId = :videoId")
    fun observeProgress(videoId: String): Flow<PlaybackProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: PlaybackProgressEntity)
}
