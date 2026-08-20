package dev.iimuz.capturehub.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCaptureDao : CaptureDao {
    val captures = mutableListOf<CaptureEntity>()
    private val latestFlow = MutableStateFlow<CaptureEntity?>(null)

    override suspend fun insert(capture: CaptureEntity): Long {
        if (captures.any { it.id == capture.id }) return -1L
        captures += capture
        latestFlow.value = captures.maxByOrNull { it.createdAt }
        return 1L
    }

    override suspend fun updateStatus(
        id: String,
        status: CaptureStatus,
    ) {
        val index = captures.indexOfFirst { it.id == id }
        if (index >= 0) captures[index] = captures[index].copy(status = status)
    }

    override suspend fun pending(): List<CaptureEntity> =
        captures
            .filter { it.status != CaptureStatus.WRITTEN }
            .sortedBy { it.createdAt }

    override suspend fun findById(id: String): CaptureEntity? = captures.find { it.id == id }

    override fun latest(): Flow<CaptureEntity?> = latestFlow
}
