package dev.iimuz.capturehub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.iimuz.capturehub.core.designsystem.CaptureHubTheme
import dev.iimuz.capturehub.feature.capture.CaptureScreen
import dev.iimuz.capturehub.feature.capture.CaptureViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as CaptureHubApp
        setContent {
            CaptureHubTheme {
                val captureViewModel: CaptureViewModel =
                    viewModel(
                        factory =
                            viewModelFactory {
                                initializer {
                                    CaptureViewModel(
                                        dao = app.database.captureDao(),
                                        onSaved = {},
                                    )
                                }
                            },
                    )
                CaptureScreen(viewModel = captureViewModel, onOpenSettings = {})
            }
        }
    }
}
