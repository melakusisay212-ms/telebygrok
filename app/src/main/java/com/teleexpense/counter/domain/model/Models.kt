package com.teleexpense.counter.domain.model

enum class Classification { COUNT, IGNORE, REFERENCE }
enum class Direction { OUTGOING, INCOMING, UNKNOWN }
enum class TransactionStatus { SUCCESSFUL, FAILED, PENDING, UNKNOWN }
enum class Category {
    AIRTIME, AIRTIME_RECHARGE, DATA_PACKAGE, SMS_PACKAGE, VOICE_PACKAGE,
    MIXED_PACKAGE, FREE_PACKAGE, PACKAGE_ACTIVATION,
    PERSON_TO_PERSON_TRANSFER, BANK_TRANSFER, OTHER
}
enum class Provider { ETHIO_TELECOM, TELEBIRR, EBIRR, BANK, UNKNOWN }

data class SmsParseResult(
    val sourceSmsId: String,
    val rawSender: String,
    val smsReceivedAtMillis: Long,
    val classification: Classification,
    val confidence: Float,
    val provider: Provider,
    val category: Category,
    val amount: Double?,
    val currency: String = "ETB",
    val direction: Direction,
    val status: TransactionStatus,
    val packageName: String?,
    val recipient: String?,
    val transactionId: String?,
    val transferId: String?,
    val gregorianDateTimeMillis: Long?,
    val ethiopianYear: Int?,
    val ethiopianMonth: Int?,
    val ethiopianDay: Int?
)

data class TelecomTransaction(
    val id: String,
    val provider: Provider,
    val category: Category,
    val amount: Double,
    val currency: String = "ETB",
    val direction: Direction,
    val status: TransactionStatus,
    val gregorianDateTimeMillis: Long,
    val ethiopianYear: Int,
    val ethiopianMonth: Int,
    val ethiopianDay: Int,
    val packageName: String?,
    val recipient: String?,
    val transactionId: String?,
    val transferId: String?,
    val sourceEventIds: List<String>,
    val classification: Classification,
    val confidence: Float,
    val createdAtMillis: Long,
    val isManuallyEdited: Boolean = false,
    val isExcluded: Boolean = false
)

data class CategoryBreakdown(
    val category: Category,
    val amount: Double,
    val percentage: Float,
    val count: Int
)

data class PeriodSummary(
    val label: String,
    val ethiopianLabel: String,
    val totalAmount: Double,
    val transactionCount: Int,
    val averagePerDay: Double,
    val averagePerTransaction: Double,
    val breakdown: List<CategoryBreakdown>
)
