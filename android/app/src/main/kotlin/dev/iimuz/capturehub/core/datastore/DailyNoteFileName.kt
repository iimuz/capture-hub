package dev.iimuz.capturehub.core.datastore

import java.time.DateTimeException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// 最後のドット以降を拡張子リテラルとして除外する。yyyy-MM-dd.md の md を
// 分・日の日付パターンとして誤解釈しないため。
fun dailyNoteFileName(
    date: LocalDate,
    pattern: String,
): String {
    val dotIndex = pattern.lastIndexOf('.')
    if (dotIndex <= 0) return date.format(DateTimeFormatter.ofPattern(pattern))
    val datePart = pattern.substring(0, dotIndex)
    return date.format(DateTimeFormatter.ofPattern(datePart)) + pattern.substring(dotIndex)
}

fun isValidFileNamePattern(pattern: String): Boolean {
    if (pattern.isBlank() || pattern.contains('/')) return false
    return try {
        dailyNoteFileName(LocalDate.of(2000, 1, 1), pattern)
        true
    } catch (_: IllegalArgumentException) {
        false
    } catch (_: DateTimeException) {
        // 日付フィールドを含まないパターンは format 時に DateTimeException となり、
        // IllegalArgumentException では捕捉できない
        false
    }
}
