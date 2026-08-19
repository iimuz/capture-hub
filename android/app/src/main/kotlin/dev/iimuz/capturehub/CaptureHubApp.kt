package dev.iimuz.capturehub

import android.app.Application
import androidx.room.Room
import dev.iimuz.capturehub.core.database.AppDatabase

class CaptureHubApp : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "capture-hub").build()
    }
}
