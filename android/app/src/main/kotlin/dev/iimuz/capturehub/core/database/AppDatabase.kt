package dev.iimuz.capturehub.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CaptureEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
}
