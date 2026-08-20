package dev.iimuz.capturehub.core.datastore

import java.time.LocalDate

// YYYY を YY+YY のように短いトークンへ分割してしまわないよう、
// 長いトークンから先に一致を試す (longest-match-first)。
private val DATE_TOKENS: List<Pair<String, (LocalDate) -> String>> =
    listOf(
        "YYYY" to { date -> "%04d".format(date.year) },
        "YY" to { date -> "%02d".format(date.year % 100) },
        "MM" to { date -> "%02d".format(date.monthValue) },
        "DD" to { date -> "%02d".format(date.dayOfMonth) },
        "M" to { date -> date.monthValue.toString() },
        "D" to { date -> date.dayOfMonth.toString() },
    )

private fun isAsciiLetter(c: Char): Boolean = c in 'a'..'z' || c in 'A'..'Z'

// フォーマットと検証で解釈がずれないよう、パターン走査を 1 箇所に集約する。
// 日付トークンを 1 つも含まないパターンは全ての日で同じファイルになってしまうため、
// 呼び出し側が検出できるよう出現有無も返す。
private fun scanDatePart(
    date: LocalDate,
    datePart: String,
): Pair<String, Boolean> {
    val result = StringBuilder()
    var sawToken = false
    var i = 0
    while (i < datePart.length) {
        val c = datePart[i]
        if (c == '[') {
            val end = datePart.indexOf(']', i + 1)
            require(end >= 0) { "unclosed bracket in pattern: $datePart" }
            result.append(datePart, i + 1, end)
            i = end + 1
            continue
        }
        val token = DATE_TOKENS.firstOrNull { (key, _) -> datePart.startsWith(key, i) }
        if (token != null) {
            result.append(token.second(date))
            sawToken = true
            i += token.first.length
            continue
        }
        // 対応外の英字を許すと moment のトークンと意味が食い違う可能性があるため、
        // 未対応のトークンとして無効化する
        require(!isAsciiLetter(c)) { "unsupported token letter '$c' in pattern: $datePart" }
        result.append(c)
        i++
    }
    return result.toString() to sawToken
}

// 最後のドット以降を拡張子リテラルとして除外する。YYYY-MM-DD.md の md を
// 日付トークンとして誤解釈しないため。
fun dailyNoteFileName(
    date: LocalDate,
    pattern: String,
): String {
    val dotIndex = pattern.lastIndexOf('.')
    val (datePart, extension) =
        if (dotIndex <= 0) pattern to "" else pattern.substring(0, dotIndex) to pattern.substring(dotIndex)
    val (formatted, _) = scanDatePart(date, datePart)
    return formatted + extension
}

fun isValidFileNamePattern(pattern: String): Boolean {
    if (pattern.isBlank() || pattern.contains('/')) return false
    val dotIndex = pattern.lastIndexOf('.')
    val datePart = if (dotIndex <= 0) pattern else pattern.substring(0, dotIndex)
    return try {
        val (_, sawToken) = scanDatePart(LocalDate.of(2000, 1, 1), datePart)
        sawToken
    } catch (_: IllegalArgumentException) {
        false
    }
}
