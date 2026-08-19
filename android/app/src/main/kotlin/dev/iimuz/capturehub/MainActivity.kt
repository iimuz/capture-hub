package dev.iimuz.capturehub

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.iimuz.capturehub.core.datastore.hasPersistedWritePermission
import dev.iimuz.capturehub.core.designsystem.CaptureHubTheme
import dev.iimuz.capturehub.feature.capture.CaptureScreen
import dev.iimuz.capturehub.feature.capture.CaptureViewModel
import dev.iimuz.capturehub.feature.settings.SettingsScreen
import dev.iimuz.capturehub.feature.settings.SettingsViewModel
import dev.iimuz.capturehub.sync.enqueueDailyNoteWrite

private enum class Screen { CAPTURE, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as CaptureHubApp
        val takePermission: (Uri) -> Unit = { uri ->
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        enqueueDailyNoteWrite(applicationContext)
        setContent {
            CaptureHubTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.CAPTURE) }
                val captureViewModel: CaptureViewModel =
                    viewModel(
                        factory =
                            viewModelFactory {
                                initializer {
                                    CaptureViewModel(
                                        dao = app.database.captureDao(),
                                        settings = app.settingsRepository.settings,
                                        hasPermission = { uri ->
                                            hasPersistedWritePermission(app, uri)
                                        },
                                        onSaved = { enqueueDailyNoteWrite(applicationContext) },
                                    )
                                }
                            },
                    )
                val settingsViewModel: SettingsViewModel =
                    viewModel(
                        factory =
                            viewModelFactory {
                                initializer {
                                    SettingsViewModel(
                                        repository = app.settingsRepository,
                                        takePersistablePermission = takePermission,
                                        hasPermission = { uri ->
                                            hasPersistedWritePermission(app, uri)
                                        },
                                        onVaultConfigured = {
                                            enqueueDailyNoteWrite(applicationContext)
                                        },
                                    )
                                }
                            },
                    )
                BackHandler(enabled = screen == Screen.SETTINGS) {
                    screen = Screen.CAPTURE
                }
                when (screen) {
                    Screen.CAPTURE -> {
                        CaptureScreen(
                            viewModel = captureViewModel,
                            onOpenSettings = { screen = Screen.SETTINGS },
                        )
                    }

                    Screen.SETTINGS -> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { screen = Screen.CAPTURE },
                        )
                    }
                }
            }
        }
    }
}
