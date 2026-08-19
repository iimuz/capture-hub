package dev.iimuz.capturehub.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.iimuz.capturehub.core.common.Ulid
import dev.iimuz.capturehub.core.database.CaptureDao
import dev.iimuz.capturehub.core.database.CaptureEntity
import dev.iimuz.capturehub.core.database.CaptureStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CaptureViewModel(
    private val dao: CaptureDao,
    private val onSaved: () -> Unit,
    private val newId: () -> String = { Ulid.generate() },
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _saveEvents = Channel<Unit>(Channel.BUFFERED)
    val saveEvents: Flow<Unit> = _saveEvents.receiveAsFlow()

    val latest: StateFlow<CaptureEntity?> =
        dao
            .latest()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onTextChange(value: String) {
        _text.value = value
    }

    fun save() {
        val body = _text.value.trim()
        if (body.isEmpty()) return
        viewModelScope.launch {
            dao.insert(
                CaptureEntity(
                    id = newId(),
                    text = body,
                    createdAt = now(),
                    status = CaptureStatus.RECEIVED,
                ),
            )
            _text.value = ""
            _saveEvents.send(Unit)
            onSaved()
        }
    }
}
