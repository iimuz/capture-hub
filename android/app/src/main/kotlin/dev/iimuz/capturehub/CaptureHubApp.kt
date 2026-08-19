package dev.iimuz.capturehub

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.work.Configuration
import dev.iimuz.capturehub.core.database.AppDatabase
import dev.iimuz.capturehub.core.datastore.VaultSettingsRepository
import dev.iimuz.capturehub.sync.CaptureHubWorkerFactory
import dev.iimuz.capturehub.sync.DailyNoteWriter
import dev.iimuz.capturehub.sync.SafVaultFiles

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class CaptureHubApp :
    Application(),
    Configuration.Provider {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "capture-hub").build()
    }

    val settingsRepository: VaultSettingsRepository by lazy {
        VaultSettingsRepository(settingsDataStore)
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(
                    CaptureHubWorkerFactory(
                        dao = database.captureDao(),
                        settingsRepository = settingsRepository,
                        writer = DailyNoteWriter(),
                        vaultFiles = { context, uri ->
                            SafVaultFiles(context, Uri.parse(uri))
                        },
                    ),
                ).build()
}
