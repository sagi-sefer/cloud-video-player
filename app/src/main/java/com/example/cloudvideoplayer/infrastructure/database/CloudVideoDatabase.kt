package com.example.cloudvideoplayer.infrastructure.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PlaybackProgressEntity::class], version = 1, exportSchema = false)
abstract class CloudVideoDatabase : RoomDatabase() {
    abstract fun playbackProgressDao(): PlaybackProgressDao

    companion object {
        const val DATABASE_NAME = "cloud_video_database"
    }
}
