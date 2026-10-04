package com.teleexpense.counter.domain.calendar

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * Gregorian ↔ Ethiopian calendar.
 * Handles leap years and Pagume; not a fixed-day offset.
 */
object EthiopianCalendarConverter {

    data class EthiopianDate(val year: Int, val month: Int, val day: Int) {
        fun formatted(): String = "${monthName(month)} $day, $year"
    }

    private val MONTH_NAMES = arrayOf(
        "", "Meskerem", "Tikimt", "Hidar", "Tahsas",
        "Tir", "Yekatit", "Megabit", "Miazia",
        "Ginbot", "Sene", "Hamle", "Nehase", "Pagume"
    )

    fun monthName(month: Int): String =
        if (month in 1..13) MONTH_NAMES[month] else "?"

    fun isEthiopianLeapYear(year: Int): Boolean = year % 4 == 3

    fun daysInMonth(year: Int, month: Int): Int = when {
        month in 1..12 -> 30
        month == 13 -> if (isEthiopianLeapYear(year)) 6 else 5
        else -> 0
    }

    fun toEthiopian(gy: Int, gm: Int, gd: Int): EthiopianDate {
        val a = (14 - gm) / 12
        val y = gy + 4800 - a
        val m = gm + 12 * a - 3
        val jdn = gd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
        val r = (jdn - 1723856) % 1461
        val n = r % 365 + 365 * (r / 1460)
        val year = 4 * ((jdn - 1723856) / 1461) + r / 365 - r / 1460
        val month = n / 30 + 1
        val day = n % 30 + 1
        return EthiopianDate(year, month, day)
    }

    fun toEthiopian(millis: Long, tz: TimeZone = TimeZone.getDefault()): EthiopianDate {
        val cal = GregorianCalendar(tz).apply { timeInMillis = millis }
        return toEthiopian(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun toGregorian(ethYear: Int, ethMonth: Int, ethDay: Int): Triple<Int, Int, Int> {
        val jdn = 1723856 + 365 * ethYear + (ethYear / 4) + 30 * (ethMonth - 1) + ethDay - 1
        val a = jdn + 32044
        val b = (4 * a + 3) / 146097
        val c = a - (146097 * b) / 4
        val d = (4 * c + 3) / 1461
        val e = c - (1461 * d) / 4
        val m = (5 * e + 2) / 153
        val day = e - (153 * m + 2) / 5 + 1
        val month = m + 3 - 12 * (m / 10)
        val year = 100 * b + d - 4800 + m / 10
        return Triple(year, month, day)
    }

    fun startOfEthiopianMonthMillis(ethYear: Int, ethMonth: Int, tz: TimeZone = TimeZone.getDefault()): Long {
        val (gy, gm, gd) = toGregorian(ethYear, ethMonth, 1)
        return GregorianCalendar(tz).apply {
            set(Calendar.YEAR, gy)
            set(Calendar.MONTH, gm - 1)
            set(Calendar.DAY_OF_MONTH, gd)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun today(tz: TimeZone = TimeZone.getDefault()) = toEthiopian(System.currentTimeMillis(), tz)

    fun monthLabel(year: Int, month: Int) = "${monthName(month)} $year"
}
