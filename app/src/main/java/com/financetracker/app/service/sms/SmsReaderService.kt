package com.financetracker.app.service.sms

import android.content.Context
import android.net.Uri
import com.financetracker.app.data.db.entity.TransactionEntity
import com.financetracker.app.data.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsReaderService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TransactionRepository
) {
    companion object {
        private val SMS_URI = Uri.parse("content://sms/inbox")
        private val PROJECTION = arrayOf("_id", "address", "body", "date")
    }

    /** Reads the SMS inbox and imports unprocessed bank messages. */
    suspend fun readAndImportInbox(): Int = withContext(Dispatchers.IO) {
        var imported = 0
        val existingHashes = repository.getAllDedupHashes().toHashSet()

        val cursor = context.contentResolver.query(SMS_URI, PROJECTION, null, null, "date DESC")
            ?: return@withContext 0

        cursor.use { c ->
            val idxAddress = c.getColumnIndexOrThrow("address")
            val idxBody    = c.getColumnIndexOrThrow("body")
            val idxDate    = c.getColumnIndexOrThrow("date")

            while (c.moveToNext()) {
                val sender = c.getString(idxAddress) ?: continue
                val body   = c.getString(idxBody) ?: continue
                val dateMs = c.getLong(idxDate)

                if (!SmsParser.isBankSms(sender)) continue

                val parsed = SmsParser.parse(body, dateMs) ?: continue
                if (existingHashes.contains(parsed.dedupHash)) continue

                repository.insertTransaction(
                    TransactionEntity(
                        amount     = parsed.amount,
                        merchant   = parsed.merchant,
                        category   = "Other",
                        date       = parsed.dateMs,
                        source     = "SMS",
                        rawText    = parsed.rawText,
                        isCredit   = parsed.isCredit,
                        dedupHash  = parsed.dedupHash
                    )
                )
                existingHashes.add(parsed.dedupHash)
                imported++
            }
        }
        imported
    }
}
