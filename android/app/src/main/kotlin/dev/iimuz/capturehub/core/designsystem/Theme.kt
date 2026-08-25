package dev.iimuz.capturehub.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun CaptureHubTheme(content: @Composable () -> Unit) {
    // minSdk 31 のため dynamic color を常に利用できる
    val context = LocalContext.current
    val colorScheme =
        if (isSystemInDarkTheme()) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
