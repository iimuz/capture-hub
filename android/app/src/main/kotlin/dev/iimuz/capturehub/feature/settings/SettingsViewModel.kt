package dev.iimuz.capturehub.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.iimuz.capturehub.core.datastore.DEFAULT_FILE_NAME_PATTERN
import dev.iimuz.capturehub.core.datastore.VaultSettingsRepository
import dev.iimuz.capturehub.core.datastore.isValidFileNamePattern
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: VaultSettingsRepository,
    private val takePersistablePermission: (Uri) -> Unit,
    private val hasPermission: (String) -> Boolean,
    private val onWriteSettingsChanged: () -> Unit,
) : ViewModel() {
    data class UiState(
        val vaultUri: String? = null,
        val fileNamePattern: String = DEFAULT_FILE_NAME_PATTERN,
        val permissionLost: Boolean = false,
        val patternInvalid: Boolean = false,
    )

    private val patternInvalid = MutableStateFlow(false)

    val uiState: StateFlow<UiState> =
        combine(repository.settings, patternInvalid) { settings, invalid ->
            UiState(
                vaultUri = settings?.vaultUri,
                fileNamePattern = settings?.fileNamePattern ?: DEFAULT_FILE_NAME_PATTERN,
                permissionLost = settings != null && !hasPermission(settings.vaultUri),
                patternInvalid = invalid,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun onFolderPicked(uri: Uri) {
        takePersistablePermission(uri)
        viewModelScope.launch {
            repository.saveVaultUri(uri.toString())
            onWriteSettingsChanged()
        }
    }

    fun onFileNamePatternChange(value: String) {
        val valid = isValidFileNamePattern(value)
        patternInvalid.value = !valid
        if (valid) {
            viewModelScope.launch {
                repository.saveFileNamePattern(value)
                onWriteSettingsChanged()
            }
        }
    }
}
