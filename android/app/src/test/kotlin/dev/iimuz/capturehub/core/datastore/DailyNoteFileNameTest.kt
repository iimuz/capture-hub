package dev.iimuz.capturehub.core.datastore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyNoteFileNameTest {
    private val date = LocalDate.of(2026, 8, 5)

    @Test
    fun `formats date part and keeps extension literal`() {
        assertEquals("2026-08-05.md", dailyNoteFileName(date, "YYYY-MM-DD.md"))
    }

    @Test
    fun `formats bracketed literal containing letters`() {
        assertEquals("test-2026-08-05.md", dailyNoteFileName(date, "[test-]YYYY-MM-DD.md"))
    }

    @Test
    fun `formats short tokens without padding`() {
        assertEquals("26-8-5.md", dailyNoteFileName(date, "YY-M-D.md"))
    }

    @Test
    fun `formats whole pattern when no extension`() {
        assertEquals("20260805", dailyNoteFileName(date, "YYYYMMDD"))
    }

    @Test
    fun `validates patterns`() {
        assertTrue(isValidFileNamePattern("YYYY-MM-DD.md"))
        assertFalse(isValidFileNamePattern(""))
        assertFalse(isValidFileNamePattern("  "))
        assertFalse(isValidFileNamePattern("daily/YYYY-MM-DD.md"))
        assertFalse(isValidFileNamePattern("[invalid.md"))
    }

    @Test
    fun `rejects lowercase tokens from the old java-time pattern`() {
        assertFalse(isValidFileNamePattern("yyyy-MM-dd.md"))
    }

    @Test
    fun `rejects unbracketed letters that are not tokens`() {
        assertFalse(isValidFileNamePattern("test-YYYY-MM-DD.md"))
    }

    @Test
    fun `rejects patterns without a date token`() {
        assertFalse(isValidFileNamePattern(".md"))
        assertFalse(isValidFileNamePattern("[note].md"))
        assertFalse(isValidFileNamePattern("[YYYY].md"))
    }

    @Test
    fun `rejects unclosed bracket`() {
        assertFalse(isValidFileNamePattern("[YYYY"))
    }
}
