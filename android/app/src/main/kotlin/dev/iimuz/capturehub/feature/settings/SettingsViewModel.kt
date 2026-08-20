package dev.iimuz.capturehub.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.iimuz.capturehub.core.datastore.DEFAULT_FILE_NAME_PATTERN
import dev.iimuz.capturehub.core.datastore.VaultSettingsRepository
import dev.iimuz.capturehub.core.datastore.isValidFileNamePattern
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private var saveJob: Job? = null

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
        // 直前の入力に紐づく保存を必ず打ち切る。無効な値へ変わった場合も、
        // 古い有効値を 500ms 後に保存してしまわないよう破棄する
        saveJob?.cancel()
        if (valid) {
            saveJob =
                viewModelScope.launch {
                    delay(PATTERN_SAVE_DEBOUNCE_MILLIS)
                    repository.saveFileNamePattern(value)
                    onWriteSettingsChanged()
                }
        }
    }

    private companion object {
        // キー入力のたびに DataStore への保存と WorkManager への enqueue が
        // 走るのを避けるためのデバウンス時間
        const val PATTERN_SAVE_DEBOUNCE_MILLIS = 500L
    }
}
