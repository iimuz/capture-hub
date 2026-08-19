package dev.iimuz.capturehub.sync

import dev.iimuz.capturehub.core.database.CaptureEntity
import dev.iimuz.capturehub.core.datastore.dailyNoteFileName
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter

interface VaultFiles {
    fun readOrNull(fileName: String): String?

    fun create(fileName: String)

    fun append(
        fileName: String,
        content: String,
    )
}

sealed interface WriteResult {
    data object Written : WriteResult

    data object AlreadyWritten : WriteResult

    data class Failed(
        val cause: Exception,
    ) : WriteResult
}

class DailyNoteWriter(
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun append(
        files: VaultFiles,
        fileNamePattern: String,
        capture: CaptureEntity,
    ): WriteResult {
        return try {
            // 対象ファイルと見出しの双方を createdAt から導出する。リトライが日付を
            // またいだ場合でも生成日のノートへ追記するため
            val createdAt =
                Instant
                    .ofEpochMilli(capture.createdAt)
                    .atZone(clock.zone)
            val fileName = dailyNoteFileName(createdAt.toLocalDate(), fileNamePattern)
            val existing = files.readOrNull(fileName)
            if (existing != null && existing.contains("capture-id: ${capture.id} -->")) {
                return WriteResult.AlreadyWritten
            }
            if (existing == null) files.create(fileName)
            val block = renderBlock(createdAt.toLocalTime(), capture.text, capture.id)
            val payload =
                when {
                    existing.isNullOrEmpty() -> block
                    existing.endsWith("\n") -> "\n" + block
                    else -> "\n\n" + block
                }
            files.append(fileName, payload)
            WriteResult.Written
        } catch (e: Exception) {
            WriteResult.Failed(e)
        }
    }
}

private val headingFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun renderBlock(
    time: LocalTime,
    text: String,
    captureId: String,
): String =
    "## " + time.format(headingFormatter) + "\n\n" + text +
        "\n\n<!-- capture-id: " + captureId + " -->\n"
