package dev.iimuz.capturehub.sync

import dev.iimuz.capturehub.core.database.CaptureEntity
import dev.iimuz.capturehub.core.datastore.dailyNoteFileName
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime
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
            val block = renderBlock(createdAt, capture.text)
            // ブロック全体の一致を見ることで、見出しのみで本文が欠けた不完全な書き込み
            // (書き込み中断など) を「済」と誤判定しないようにする
            if (existing != null && existing.contains(block)) {
                return WriteResult.AlreadyWritten
            }
            if (existing == null) files.create(fileName)
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

// ISO_OFFSET_DATE_TIME は端数 0 のミリ秒を省略してしまうため使わない
private val headingFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

fun renderBlock(
    createdAt: ZonedDateTime,
    text: String,
): String = "### " + createdAt.format(headingFormatter) + "\n\n" + text + "\n"
