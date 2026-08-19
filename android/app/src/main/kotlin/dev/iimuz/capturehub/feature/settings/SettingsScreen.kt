package dev.iimuz.capturehub.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.iimuz.capturehub.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            if (uri != null) viewModel.onFolderPicked(uri)
        }
    // 編集開始後はローカル値を優先し、DataStore 反映待ちでカーソルが飛ぶのを防ぐ
    var editedPattern by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.vault_section),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = state.vaultUri ?: stringResource(R.string.vault_not_selected),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.permissionLost) {
                Text(
                    text = stringResource(R.string.vault_permission_lost),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(onClick = { launcher.launch(null) }) {
                Text(stringResource(R.string.choose_folder))
            }
            OutlinedTextField(
                value = editedPattern ?: state.fileNamePattern,
                onValueChange = {
                    editedPattern = it
                    viewModel.onFileNamePatternChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.file_name_pattern_label)) },
                isError = state.patternInvalid,
                supportingText = {
                    if (state.patternInvalid) {
                        Text(stringResource(R.string.file_name_pattern_invalid))
                    }
                },
                singleLine = true,
            )
        }
    }
}
