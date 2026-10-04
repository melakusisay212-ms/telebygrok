package com.teleexpense.counter.parser

import com.google.common.truth.Truth.assertThat
import com.teleexpense.counter.domain.grouping.TransactionGrouper
import com.teleexpense.counter.domain.model.Category
import com.teleexpense.counter.domain.model.Classification
import com.teleexpense.counter.domain.model.Direction
import com.teleexpense.counter.domain.parser.SmsParser
import org.junit.Test

class SmsParserTest {

    private val t = 1_726_550_000_000L

    @Test
    fun threeEtb5_groupToOne() {
        val a = SmsParser.parse("1", "EBIRR",
            "[-EBIRR-KAAFI-] Transfer ID: 802520883630, You have successfuly sent airtime top-up of ETB5 to 251981801919", t)
        val b = SmsParser.parse("2", "EBIRR",
            "[-EBIRR-KAAFI-] Transfer ID: 802520883630, You have received airtime top-up of ETB5 from 251981801919", t + 1000)
        val c = SmsParser.parse("3", "EBIRR",
            "[-EBIRR-KAAFI-] You have recharged ETB5 to 251981801919, your balance is ETB3.6", t + 2000)
        assertThat(a.classification).isEqualTo(Classification.COUNT)
        assertThat(b.classification).isEqualTo(Classification.IGNORE)
        assertThat(c.classification).isEqualTo(Classification.REFERENCE)
        val txs = TransactionGrouper.group(listOf(a, b, c))
        assertThat(txs).hasSize(1)
        assertThat(txs[0].amount).isEqualTo(5.0)
    }

    @Test
    fun twoPackages_twoExpenses() {
        val r1 = SmsParser.parse("11", "telebirr",
            "You have paid ETB 35.00 for package Monthly student pack 1.2GB purchase made for 981801919 on 11/09/2026 15:52:15. Your transaction number is DIB1N45Z9Z.", t)
        val r2 = SmsParser.parse("12", "telebirr",
            "You have paid ETB 35.00 for package Monthly student pack 1.2GB purchase made for 981801919 on 17/09/2026 08:38:04. Your transaction number is DIH5SLOKQ1.", t + 6L * 24 * 3600_000)
        assertThat(TransactionGrouper.group(listOf(r1, r2))).hasSize(2)
    }

    @Test
    fun p2p_zero() {
        val r = SmsParser.parse("20", "telebirr",
            "You have transferred ETB 10.00 to RUTA TAKELE (2519****3230) on 11/09/2026 21:21:46. The service fee is ETB 0.87.", t)
        assertThat(r.classification).isEqualTo(Classification.IGNORE)
        assertThat(TransactionGrouper.group(listOf(r))).isEmpty()
    }

    @Test
    fun freePackage_zero() {
        val r = SmsParser.parse("40", "ethio",
            "Ethio telecom wishes you a Happy birthday! Please enjoy 1GB internet package Free gift.", t)
        assertThat(r.classification).isEqualTo(Classification.IGNORE)
        assertThat(r.category).isEqualTo(Category.FREE_PACKAGE)
    }

    @Test
    fun prepaidRecharge_notBalance() {
        val r = SmsParser.parse("30", "ethio",
            "Your prepaid account has been recharged successfully. Your Recharged balance is 100.00 Birr. Your balance is 100.00 Birr.", t)
        assertThat(r.classification).isEqualTo(Classification.COUNT)
        assertThat(r.amount).isEqualTo(100.0)
        assertThat(r.direction).isEqualTo(Direction.OUTGOING)
    }
}
