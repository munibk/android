package com.financetracker.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.financetracker.app.data.db.entity.TransactionEntity
import com.financetracker.app.data.repository.CreditCardRepository
import com.financetracker.app.data.repository.SyncStatusRepository
import com.financetracker.app.data.repository.TransactionRepository
import com.financetracker.app.service.classifier.CategoryClassifier
import com.financetracker.app.service.gmail.AuthMethod
import com.financetracker.app.service.gmail.GmailParser
import com.financetracker.app.service.gmail.GmailRestFetcher
import com.financetracker.app.service.gmail.ImapCredentialsManager
import com.financetracker.app.service.gmail.ImapGmailFetcher
import com.financetracker.app.service.gmail.ImapResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class GmailFetchWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val imapFetcher: ImapGmailFetcher,
    private val restFetcher: GmailRestFetcher,
    private val credentialsManager: ImapCredentialsManager,
    private val parser: GmailParser,
    private val transactionRepo: TransactionRepository,
    private val creditCardRepo: CreditCardRepository,
    private val classifier: CategoryClassifier,
    private val syncStatusRepo: SyncStatusRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "gmail_fetch_periodic"

        fun enqueue(context: Context, intervalHours: Long = 6) {
            val request = PeriodicWorkRequestBuilder<GmailFetchWorker>(
                intervalHours, TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun enqueueOneTime(context: Context) {
            val request = OneTimeWorkRequestBuilder<GmailFetchWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }

    override suspend fun doWork(): Result {
        syncStatusRepo.setRunning()

        val lastSync = inputData.getLong("last_sync_ms", 0L)
        val existingHashes = transactionRepo.getAllDedupHashes().toHashSet()

        // Route to IMAP or Gmail REST API based on saved auth method
        val fetchResult = if (credentialsManager.getAuthMethod() == AuthMethod.OAUTH) {
            restFetcher.fetchEmails(lastSync)
        } else {
            imapFetcher.fetchEmails(lastSync)
        }

        return when (val result = fetchResult) {
            is ImapResult.Success -> {
                val total = result.emails.size
                var processed = 0
                var newCount = 0

                syncStatusRepo.setProgress(
                    emailsFound = total,
                    emailsProcessed = 0,
                    newTransactions = 0
                )

                result.emails.forEach { email ->
                    val parsed = parser.parse(email)
                    processed++

                    if (parsed != null && !existingHashes.contains(parsed.dedupHash)) {
                        val category = classifier.classify(parsed.rawText, parsed.merchant)
                        val txId = transactionRepo.insertTransaction(
                            TransactionEntity(
                                amount    = parsed.amount,
                                merchant  = parsed.merchant,
                                category  = category,
                                date      = parsed.dateMs,
                                source    = "Gmail",
                                rawText   = parsed.rawText,
                                isCredit  = parsed.isCredit,
                                dedupHash = parsed.dedupHash
                            )
                        )
                        existingHashes.add(parsed.dedupHash)
                        newCount++

                        parsed.last4Digits?.let { last4 ->
                            val card = creditCardRepo.getCardByLast4(last4)
                            if (card != null && txId > 0) {
                                creditCardRepo.linkTransaction(card.id, txId)
                            }
                        }
                    }

                    syncStatusRepo.setProgress(
                        emailsFound = total,
                        emailsProcessed = processed,
                        newTransactions = newCount
                    )
                }
                syncStatusRepo.setIdle()
                Result.success()
            }
            is ImapResult.AuthError -> {
                syncStatusRepo.setError(result.message)
                Result.failure()
            }
            is ImapResult.NetworkError -> {
                syncStatusRepo.setError(result.message)
                Result.retry()
            }
        }
    }
}
