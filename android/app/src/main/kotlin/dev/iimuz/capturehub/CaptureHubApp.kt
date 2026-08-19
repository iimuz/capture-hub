package dev.iimuz.capturehub

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import dev.iimuz.capturehub.core.database.AppDatabase
import dev.iimuz.capturehub.core.datastore.VaultSettingsRepository

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class CaptureHubApp : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "capture-hub").build()
    }

    val settingsRepository: VaultSettingsRepository by lazy {
        VaultSettingsRepository(settingsDataStore)
    }
}
