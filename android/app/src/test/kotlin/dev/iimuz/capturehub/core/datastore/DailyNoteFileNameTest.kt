package dev.iimuz.capturehub.core.datastore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyNoteFileNameTest {
    private val date = LocalDate.of(2026, 8, 19)

    @Test
    fun `formats date part and keeps extension literal`() {
        assertEquals("2026-08-19.md", dailyNoteFileName(date, "yyyy-MM-dd.md"))
    }

    @Test
    fun `formats whole pattern when no extension`() {
        assertEquals("20260819", dailyNoteFileName(date, "yyyyMMdd"))
    }

    @Test
    fun `keeps custom prefix inside date pattern`() {
        assertEquals("2026-08-19-daily.md", dailyNoteFileName(date, "yyyy-MM-dd-'daily'.md"))
    }

    @Test
    fun `validates patterns`() {
        assertTrue(isValidFileNamePattern("yyyy-MM-dd.md"))
        assertFalse(isValidFileNamePattern(""))
        assertFalse(isValidFileNamePattern("  "))
        assertFalse(isValidFileNamePattern("daily/yyyy-MM-dd.md"))
        assertFalse(isValidFileNamePattern("[invalid.md"))
    }

    @Test
    fun `rejects patterns without date field`() {
        assertFalse(isValidFileNamePattern(".md"))
        assertFalse(isValidFileNamePattern("HH"))
        assertFalse(isValidFileNamePattern("yyyy-MM-dd_HHmm.md"))
    }
}
