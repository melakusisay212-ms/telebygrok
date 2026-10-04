package com.teleexpense.counter.domain.parser

import java.util.regex.Pattern

object AmountExtractor {

    private val PAID = listOf(
        Pattern.compile(
            """(?:paid|payment of|purchase of|top-?up of|recharged?|sent airtime top-?up of|transferred)\s+(?:ETB|Birr|Br\.?)\s*([\d,]+\.?\d*)""",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile(
            """(?:ETB|Birr|Br\.?)\s*([\d,]+\.?\d*)\s+(?:for package|for|airtime|top-?up|recharge)""",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile(
            """(?:You have paid|You have successfully sent airtime top-up of|You have recharged)\s+(?:ETB|Birr)?\s*([\d,]+\.?\d*)""",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile(
            """Recharged balance is\s+([\d,]+\.?\d*)\s*(?:Birr|ETB)?""",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile(
            """airtime top-up of\s+(?:ETB|Birr)?\s*([\d,]+\.?\d*)""",
            Pattern.CASE_INSENSITIVE
        )
    )

    private val BALANCE = listOf(
        Pattern.compile("""(?:your\s+)?balance\s+is\s+(?:ETB|Birr)?\s*([\d,]+\.?\d*)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""Your balance is\s+(?:ETB|Birr)?\s*([\d,]+\.?\d*)""", Pattern.CASE_INSENSITIVE)
    )

    fun extractExpenseAmount(text: String): Double? {
        val balances = mutableSetOf<Double>()
        for (p in BALANCE) {
            val m = p.matcher(text)
            while (m.find()) parseNumber(m.group(1))?.let { balances.add(it) }
        }
        for (p in PAID) {
            val m = p.matcher(text)
            if (m.find()) {
                val amount = parseNumber(m.group(1)) ?: continue
                if (isPostBalanceOnly(text, amount, balances)) continue
                return amount
            }
        }
        val etb = Pattern.compile("""(?:ETB|Birr|Br\.?)\s*([\d,]+\.?\d*)""", Pattern.CASE_INSENSITIVE)
        val keywords = listOf("paid", "purchase", "recharged", "top-up", "topup", "package", "airtime")
        val lower = text.lowercase()
        val m = etb.matcher(text)
        while (m.find()) {
            val amount = parseNumber(m.group(1)) ?: continue
            if (balances.contains(amount) && !lower.contains("recharged balance")) continue
            val start = (m.start() - 40).coerceAtLeast(0)
            val end = (m.end() + 40).coerceAtMost(text.length)
            if (keywords.any { lower.substring(start, end).contains(it) }) return amount
        }
        return null
    }

    private fun isPostBalanceOnly(text: String, amount: Double, balances: Set<Double>): Boolean {
        if (!balances.contains(amount)) return false
        val lower = text.lowercase()
        if (lower.contains("recharged balance")) return false
        return lower.contains("your balance is") || lower.contains("balance is")
    }

    fun parseNumber(raw: String?): Double? =
        raw?.replace(",", "")?.toDoubleOrNull()

    fun isFreeOrBonus(text: String): Boolean {
        val lower = text.lowercase()
        val words = listOf(
            "free gift", "free package", "complimentary", "bonus package",
            "promotional", "promotion", "free of charge", "happy birthday", "enjoy free"
        )
        return words.any { lower.contains(it) } ||
            (lower.contains("free") && (lower.contains("package") || lower.contains("gift") || lower.contains("bonus")))
    }
}
