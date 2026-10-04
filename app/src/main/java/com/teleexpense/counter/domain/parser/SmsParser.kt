package com.teleexpense.counter.domain.parser

import com.teleexpense.counter.domain.calendar.EthiopianCalendarConverter
import com.teleexpense.counter.domain.model.*
import java.util.regex.Pattern

object SmsParser {

    private val TRANSFER_ID = Pattern.compile("""Transfer ID:\s*(\d+)""", Pattern.CASE_INSENSITIVE)
    private val TXN_ID = Pattern.compile(
        """(?:transaction number is|transaction id[:\s]|Txn[:\s]|Trx[:\s])\s*([A-Z0-9]+)""",
        Pattern.CASE_INSENSITIVE
    )
    private val RECIPIENT = Pattern.compile("""(?:to|for)\s+(?:251)?(\d{9,12})""", Pattern.CASE_INSENSITIVE)
    private val PKG = listOf(
        Pattern.compile(
            """(?:for package|package)\s+([A-Za-z0-9\s.+\-]+?)(?:\s+purchase|\s+from|\s+to expire|\s+is added|\.|$)""",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile(
            """sent\s+([A-Za-z0-9\s.+\-]*?(?:Package|pack|MB|GB)[A-Za-z0-9\s.+\-]*)\s+to""",
            Pattern.CASE_INSENSITIVE
        )
    )

    fun parse(smsId: String, sender: String, body: String, receivedAtMillis: Long): SmsParseResult {
        val text = body.trim().replace(Regex("\\s+"), " ")
        val provider = ProviderDetector.detect(sender, text)
        val cls = CategoryClassifier.classify(sender, text)
        val amount = when (cls.classification) {
            Classification.COUNT, Classification.REFERENCE -> AmountExtractor.extractExpenseAmount(text)
            Classification.IGNORE ->
                if (cls.category == Category.FREE_PACKAGE) 0.0
                else AmountExtractor.extractExpenseAmount(text)
        }
        val explicit = DateExtractor.extract(text)
        val effective = explicit ?: receivedAtMillis
        val eth = EthiopianCalendarConverter.toEthiopian(effective)
        val transferId = first(TRANSFER_ID, text)
        val transactionId = first(TXN_ID, text)
        val recipient = recipient(text)
        val packageName = packageName(text)
        val confidence = cls.confidence *
            (if (amount != null || cls.classification != Classification.COUNT) 1f else 0.7f)

        return SmsParseResult(
            sourceSmsId = smsId,
            rawSender = sender,
            smsReceivedAtMillis = receivedAtMillis,
            classification = cls.classification,
            confidence = confidence.coerceIn(0f, 1f),
            provider = provider,
            category = cls.category,
            amount = amount,
            direction = cls.direction,
            status = TransactionStatus.SUCCESSFUL,
            packageName = packageName,
            recipient = recipient,
            transactionId = transactionId,
            transferId = transferId,
            gregorianDateTimeMillis = effective,
            ethiopianYear = eth.year,
            ethiopianMonth = eth.month,
            ethiopianDay = eth.day
        )
    }

    private fun first(p: Pattern, t: String): String? {
        val m = p.matcher(t)
        return if (m.find()) m.group(1)?.trim() else null
    }

    private fun recipient(t: String): String? {
        val m = RECIPIENT.matcher(t)
        if (!m.find()) return null
        val n = m.group(1) ?: return null
        return if (n.length == 9) "251$n" else n
    }

    private fun packageName(t: String): String? {
        for (p in PKG) {
            val m = p.matcher(t)
            if (m.find()) {
                val name = m.group(1)?.trim()?.take(120)
                if (!name.isNullOrBlank() && name.length > 3) return name
            }
        }
        return null
    }
}
