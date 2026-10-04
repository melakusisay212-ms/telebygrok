package com.teleexpense.counter.domain.parser

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import java.util.regex.Pattern

object DateExtractor {

    private val PATTERNS = listOf(
        Pattern.compile("""(\d{1,2})/(\d{1,2})/(\d{4})\s+(\d{1,2}):(\d{2}):(\d{2})"""),
        Pattern.compile("""(\d{1,2})/(\d{1,2})/(\d{2})(?:\s+(\d{1,2}):(\d{2}):(\d{2}))?"""),
        Pattern.compile(
            """(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+(\d{1,2}),?\s+(\d{4})\s+(\d{1,2}):(\d{2}):(\d{2})\s*(AM|PM)?""",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile("""(\d{4})-(\d{1,2})-(\d{1,2})(?:\s+(\d{1,2}):(\d{2}):(\d{2}))?""")
    )

    private val MONTH_MAP = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
        "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
    )

    fun extract(text: String, tz: TimeZone = TimeZone.getDefault()): Long? {
        for (p in PATTERNS) {
            val m = p.matcher(text)
            if (m.find()) {
                try {
                    return parse(m, tz)
                } catch (_: Exception) { }
            }
        }
        return null
    }

    private fun parse(m: java.util.regex.Matcher, tz: TimeZone): Long? {
        val g1 = m.group(1) ?: return null
        if (g1.matches(Regex("[A-Za-z]+"))) {
            val month = MONTH_MAP[g1.lowercase().take(3)] ?: return null
            val day = m.group(2)?.toIntOrNull() ?: return null
            val year = m.group(3)?.toIntOrNull() ?: return null
            var hour = m.group(4)?.toIntOrNull() ?: 0
            val min = m.group(5)?.toIntOrNull() ?: 0
            val sec = m.group(6)?.toIntOrNull() ?: 0
            val ampm = m.group(7)?.uppercase()
            if (ampm == "PM" && hour < 12) hour += 12
            if (ampm == "AM" && hour == 12) hour = 0
            return millis(year, month, day, hour, min, sec, tz)
        }
        if (g1.length == 4) {
            val year = g1.toIntOrNull() ?: return null
            val month = m.group(2)?.toIntOrNull() ?: return null
            val day = m.group(3)?.toIntOrNull() ?: return null
            val hour = m.group(4)?.toIntOrNull() ?: 0
            val min = m.group(5)?.toIntOrNull() ?: 0
            val sec = m.group(6)?.toIntOrNull() ?: 0
            return millis(year, month, day, hour, min, sec, tz)
        }
        val day = g1.toIntOrNull() ?: return null
        val month = m.group(2)?.toIntOrNull() ?: return null
        var year = m.group(3)?.toIntOrNull() ?: return null
        if (year < 100) year += 2000
        val hour = m.group(4)?.toIntOrNull() ?: 0
        val min = m.group(5)?.toIntOrNull() ?: 0
        val sec = m.group(6)?.toIntOrNull() ?: 0
        return millis(year, month, day, hour, min, sec, tz)
    }

    private fun millis(y: Int, mo: Int, d: Int, h: Int, mi: Int, s: Int, tz: TimeZone): Long {
        val cal = GregorianCalendar(tz)
        cal.set(y, mo - 1, d, h, mi, s)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
