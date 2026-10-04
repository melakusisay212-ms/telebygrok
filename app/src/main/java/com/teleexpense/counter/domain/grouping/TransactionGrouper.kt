package com.teleexpense.counter.domain.grouping

import com.teleexpense.counter.domain.model.*
import java.util.UUID
import kotlin.math.abs

object TransactionGrouper {

    private const val WINDOW_MS = 15 * 60 * 1000L

    fun group(results: List<SmsParseResult>): List<TelecomTransaction> {
        if (results.isEmpty()) return emptyList()
        val counts = results.filter { it.classification == Classification.COUNT }
        val refs = results.filter { it.classification == Classification.REFERENCE }
        val groups = mutableListOf<MutableList<SmsParseResult>>()

        for (c in counts) {
            val existing = groups.find { g -> g.any { matches(it, c) } }
            if (existing != null) existing.add(c) else groups.add(mutableListOf(c))
        }
        for (r in refs) {
            groups.find { g -> g.any { matches(it, r) } }?.add(r)
        }

        return groups.mapNotNull { group ->
            val primary = group
                .filter { it.classification == Classification.COUNT }
                .maxByOrNull {
                    it.confidence +
                        (if (it.amount != null) 0.1f else 0f) +
                        (if (it.transactionId != null) 0.1f else 0f)
                } ?: return@mapNotNull null
            val amount = primary.amount ?: return@mapNotNull null
            if (amount <= 0.0) return@mapNotNull null
            val ethY = primary.ethiopianYear ?: return@mapNotNull null
            val ethM = primary.ethiopianMonth ?: return@mapNotNull null
            val ethD = primary.ethiopianDay ?: return@mapNotNull null
            val gMillis = primary.gregorianDateTimeMillis ?: primary.smsReceivedAtMillis
            TelecomTransaction(
                id = UUID.randomUUID().toString(),
                provider = primary.provider,
                category = primary.category,
                amount = amount,
                direction = primary.direction,
                status = primary.status,
                gregorianDateTimeMillis = gMillis,
                ethiopianYear = ethY,
                ethiopianMonth = ethM,
                ethiopianDay = ethD,
                packageName = primary.packageName ?: group.mapNotNull { it.packageName }.firstOrNull(),
                recipient = primary.recipient ?: group.mapNotNull { it.recipient }.firstOrNull(),
                transactionId = primary.transactionId ?: group.mapNotNull { it.transactionId }.firstOrNull(),
                transferId = primary.transferId ?: group.mapNotNull { it.transferId }.firstOrNull(),
                sourceEventIds = group.map { it.sourceSmsId },
                classification = Classification.COUNT,
                confidence = group.map { it.confidence }.average().toFloat(),
                createdAtMillis = System.currentTimeMillis()
            )
        }
    }

    private fun matches(a: SmsParseResult, b: SmsParseResult): Boolean {
        if (!a.transferId.isNullOrBlank() && a.transferId == b.transferId) return true
        if (!a.transactionId.isNullOrBlank() && a.transactionId == b.transactionId) return true
        val aa = a.amount
        val ba = b.amount
        if (aa != null && ba != null && aa != ba) return false
        val ta = a.gregorianDateTimeMillis ?: a.smsReceivedAtMillis
        val tb = b.gregorianDateTimeMillis ?: b.smsReceivedAtMillis
        if (abs(ta - tb) > WINDOW_MS) return false
        if (!a.recipient.isNullOrBlank() && a.recipient == b.recipient && aa != null && aa == ba) return true
        if (!a.packageName.isNullOrBlank() && a.packageName.equals(b.packageName, true) && aa != null && aa == ba) return true
        if (a.provider == b.provider && aa != null && aa == ba && abs(ta - tb) <= 5 * 60 * 1000L) return true
        return false
    }
}
