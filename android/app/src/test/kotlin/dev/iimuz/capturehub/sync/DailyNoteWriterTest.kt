package dev.iimuz.capturehub.sync

import dev.iimuz.capturehub.core.database.CaptureEntity
import dev.iimuz.capturehub.core.database.CaptureStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class DailyNoteWriterTest {
    // 2026-08-19 15:42 JST
    private val clock =
        Clock.fixed(
            Instant.parse("2026-08-19T06:42:00Z"),
            ZoneId.of("Asia/Tokyo"),
        )
    private val writer = DailyNoteWriter(clock)
    private val capture =
        CaptureEntity(
            id = "01TESTID0000000000000000AA",
            text = "test note",
            createdAt = Instant.parse("2026-08-19T06:42:00Z").toEpochMilli(),
            status = CaptureStatus.RECEIVED,
        )
    private val expectedBlock =
        "## 15:42\n\ntest note\n\n<!-- capture-id: 01TESTID0000000000000000AA -->\n"

    @Test
    fun `creates file and writes block when file is missing`() {
        val files = InMemoryVaultFiles()
        val result = writer.append(files, "yyyy-MM-dd.md", capture)
        assertEquals(WriteResult.Written, result)
        assertEquals(expectedBlock, files.files["2026-08-19.md"])
    }

    @Test
    fun `appends with blank line separator to existing content`() {
        val files = InMemoryVaultFiles()
        files.files["2026-08-19.md"] = "## 09:00\n\nmorning\n\n<!-- capture-id: OTHER -->\n"
        writer.append(files, "yyyy-MM-dd.md", capture)
        assertEquals(
            "## 09:00\n\nmorning\n\n<!-- capture-id: OTHER -->\n\n" + expectedBlock,
            files.files["2026-08-19.md"],
        )
    }

    @Test
    fun `restores trailing newline before appending`() {
        val files = InMemoryVaultFiles()
        files.files["2026-08-19.md"] = "no trailing newline"
        writer.append(files, "yyyy-MM-dd.md", capture)
        assertEquals(
            "no trailing newline\n\n" + expectedBlock,
            files.files["2026-08-19.md"],
        )
    }

    @Test
    fun `skips when capture id already exists`() {
        val files = InMemoryVaultFiles()
        val existing = "## 09:00\n\nold\n\n<!-- capture-id: 01TESTID0000000000000000AA -->\n"
        files.files["2026-08-19.md"] = existing
        val result = writer.append(files, "yyyy-MM-dd.md", capture)
        assertEquals(WriteResult.AlreadyWritten, result)
        assertEquals(existing, files.files["2026-08-19.md"])
    }

    @Test
    fun `returns failed when vault is unreachable`() {
        val result = writer.append(FailingVaultFiles(), "yyyy-MM-dd.md", capture)
        assertTrue(result is WriteResult.Failed)
    }

    @Test
    fun `heading uses capture created time not current time`() {
        val files = InMemoryVaultFiles()
        val earlier =
            capture.copy(
                createdAt = Instant.parse("2026-08-19T00:05:00Z").toEpochMilli(),
            )
        writer.append(files, "yyyy-MM-dd.md", earlier)
        assertTrue(files.files.getValue("2026-08-19.md").startsWith("## 09:05\n"))
    }

    @Test
    fun `writes to created date file when retry crosses midnight`() {
        // 現在時刻は 2026-08-20 09:30 JST、capture の作成は 2026-08-19 23:58 JST
        val nextDayClock =
            Clock.fixed(
                Instant.parse("2026-08-20T00:30:00Z"),
                ZoneId.of("Asia/Tokyo"),
            )
        val files = InMemoryVaultFiles()
        val lateNight =
            capture.copy(
                createdAt = Instant.parse("2026-08-19T14:58:00Z").toEpochMilli(),
            )
        val result = DailyNoteWriter(nextDayClock).append(files, "yyyy-MM-dd.md", lateNight)
        assertEquals(WriteResult.Written, result)
        assertFalse(files.files.containsKey("2026-08-20.md"))
        assertTrue(files.files.getValue("2026-08-19.md").startsWith("## 23:58\n"))
    }
}
