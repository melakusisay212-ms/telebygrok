package com.teleexpense.counter.data.repository

import android.content.Context
import android.database.Cursor
import android.provider.Telephony
import com.teleexpense.counter.data.AppDatabase
import com.teleexpense.counter.data.entity.*
import com.teleexpense.counter.domain.grouping.TransactionGrouper
import com.teleexpense.counter.domain.model.*
import com.teleexpense.counter.domain.parser.SmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.GregorianCalendar

class ExpenseRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val txDao = db.transactionDao()
    private val smsDao = db.processedSmsDao()
    private val metaDao = db.appMetaDao()

    companion object {
        const val KEY_TRACKER_STARTED = "trackerStartedAt"
        const val KEY_LAST_PROCESSED = "lastProcessedAt"
        const val KEY_ONBOARDING_DONE = "onboardingDone"
    }

    fun observeTransactions(): Flow<List<TelecomTransaction>> =
        txDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAllTransactions() = txDao.getAll().map { it.toDomain() }
    suspend fun getTransaction(id: String) = txDao.getById(id)?.toDomain()
    suspend fun getByEthiopianMonth(y: Int, m: Int) = txDao.getByEthiopianMonth(y, m).map { it.toDomain() }
    suspend fun getByEthiopianDay(y: Int, m: Int, d: Int) = txDao.getByEthiopianDay(y, m, d).map { it.toDomain() }

    suspend fun isOnboardingDone() = metaDao.get(KEY_ONBOARDING_DONE) == "true"
    suspend fun setOnboardingDone() = metaDao.put(AppMetaEntity(KEY_ONBOARDING_DONE, "true"))
    suspend fun getTrackerStartedAt() = metaDao.get(KEY_TRACKER_STARTED)?.toLongOrNull()
    suspend fun setTrackerStartedAt(millis: Long) {
        metaDao.put(AppMetaEntity(KEY_TRACKER_STARTED, millis.toString()))
        metaDao.put(AppMetaEntity(KEY_LAST_PROCESSED, millis.toString()))
    }
    suspend fun getLastProcessedAt() = metaDao.get(KEY_LAST_PROCESSED)?.toLongOrNull()
    suspend fun setLastProcessedAt(millis: Long) =
        metaDao.put(AppMetaEntity(KEY_LAST_PROCESSED, millis.toString()))

    suspend fun scanCurrentMonth(): ScanResult = withContext(Dispatchers.IO) {
        val cal = GregorianCalendar().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        processMessages(readSmsSince(cal.timeInMillis), markStart = true)
    }

    suspend fun scanNewMessages(): ScanResult = withContext(Dispatchers.IO) {
        val last = getLastProcessedAt() ?: getTrackerStartedAt() ?: System.currentTimeMillis()
        processMessages(readSmsSince(last), markStart = false)
    }

    suspend fun startFromToday(): ScanResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        setTrackerStartedAt(now)
        setOnboardingDone()
        ScanResult(0, 0, 0, emptyList())
    }

    private suspend fun processMessages(messages: List<RawSms>, markStart: Boolean): ScanResult {
        if (messages.isEmpty()) {
            if (markStart) {
                setTrackerStartedAt(System.currentTimeMillis())
                setOnboardingDone()
            }
            return ScanResult(0, 0, 0, emptyList())
        }
        val done = smsDao.getAllIds().toSet()
        val toProcess = messages.filter { it.id !in done }
        val parsed = toProcess.map { SmsParser.parse(it.id, it.address, it.body, it.date) }
        smsDao.insertAll(parsed.map {
            ProcessedSmsEntity(it.sourceSmsId, System.currentTimeMillis(), it.classification.name)
        })
        val newTx = TransactionGrouper.group(parsed)
        val existing = txDao.getAll().map { it.toDomain() }
        val filtered = newTx.filter { c ->
            existing.none { e ->
                (!c.transferId.isNullOrBlank() && c.transferId == e.transferId) ||
                    (!c.transactionId.isNullOrBlank() && c.transactionId == e.transactionId)
            }
        }
        txDao.insertAll(filtered.map { it.toEntity() })
        val maxDate = messages.maxOfOrNull { it.date } ?: System.currentTimeMillis()
        if (markStart) {
            setTrackerStartedAt(System.currentTimeMillis())
            setOnboardingDone()
        }
        setLastProcessedAt(maxDate)
        return ScanResult(
            relevantMessages = parsed.size,
            transactionEvents = parsed.count { it.classification != Classification.IGNORE },
            expensesCreated = filtered.size,
            transactions = filtered
        )
    }

    private fun readSmsSince(sinceMillis: Long): List<RawSms> {
        val results = mutableListOf<RawSms>()
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                "${Telephony.Sms.DATE} >= ?",
                arrayOf(sinceMillis.toString()),
                "${Telephony.Sms.DATE} ASC"
            )
            cursor?.let {
                val idI = it.getColumnIndex(Telephony.Sms._ID)
                val aI = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bI = it.getColumnIndex(Telephony.Sms.BODY)
                val dI = it.getColumnIndex(Telephony.Sms.DATE)
                while (it.moveToNext()) {
                    val id = it.getString(idI) ?: continue
                    val addr = it.getString(aI) ?: ""
                    val body = it.getString(bI) ?: continue
                    val date = it.getLong(dI)
                    if (looksTelecom(addr, body)) results.add(RawSms(id, addr, body, date))
                }
            }
        } catch (_: SecurityException) {
        } finally {
            cursor?.close()
        }
        return results
    }

    private fun looksTelecom(address: String, body: String): Boolean {
        val lower = body.lowercase()
        val addr = address.lowercase()
        val keys = listOf(
            "etb", "birr", "airtime", "package", "telebirr", "ebirr", "recharged",
            "prepaid", "data", "top-up", "topup", "balance", "transaction", "ethio", "paid"
        )
        return keys.any { lower.contains(it) } ||
            addr.contains("telebirr") || addr.contains("ebirr") ||
            addr.contains("ethio") || addr.contains("telecom")
    }

    suspend fun updateTransaction(tx: TelecomTransaction) =
        txDao.update(tx.copy(isManuallyEdited = true).toEntity())

    suspend fun deleteTransaction(id: String) = txDao.deleteById(id)

    suspend fun excludeTransaction(id: String) {
        val e = txDao.getById(id) ?: return
        txDao.update(e.copy(isExcluded = true))
    }

    data class RawSms(val id: String, val address: String, val body: String, val date: Long)
    data class ScanResult(
        val relevantMessages: Int,
        val transactionEvents: Int,
        val expensesCreated: Int,
        val transactions: List<TelecomTransaction>
    )
}
