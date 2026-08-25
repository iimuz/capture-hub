package dev.iimuz.capturehub.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CaptureStatus {
    RECEIVED,
    WRITTEN,
    FAILED_WRITE,
}

@Entity(tableName = "captures")
data class CaptureEntity(
    @PrimaryKey val id: String,
    val text: String,
    val createdAt: Long,
    val status: CaptureStatus,
)
