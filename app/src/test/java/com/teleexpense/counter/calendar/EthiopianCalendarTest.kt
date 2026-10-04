package com.teleexpense.counter.calendar

import com.google.common.truth.Truth.assertThat
import com.teleexpense.counter.domain.calendar.EthiopianCalendarConverter
import org.junit.Test

class EthiopianCalendarTest {
    @Test
    fun monthNames() {
        assertThat(EthiopianCalendarConverter.monthName(1)).isEqualTo("Meskerem")
        assertThat(EthiopianCalendarConverter.monthName(13)).isEqualTo("Pagume")
    }

    @Test
    fun daysInMonth() {
        for (m in 1..12) assertThat(EthiopianCalendarConverter.daysInMonth(2019, m)).isEqualTo(30)
        assertThat(EthiopianCalendarConverter.daysInMonth(2015, 13)).isEqualTo(6)
        assertThat(EthiopianCalendarConverter.daysInMonth(2019, 13)).isEqualTo(5)
    }
}
