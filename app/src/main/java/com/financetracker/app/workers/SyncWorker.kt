package com.financetracker.app.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.financetracker.app.R
import com.financetracker.app.data.repository.CategoryRepository
import com.financetracker.app.data.repository.CreditCardRepository
import com.financetracker.app.data.repository.SyncStatusRepository
import com.financetracker.app.data.repository.TransactionRepository
import com.financetracker.app.service.classifier.CategoryClassifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import java.time.ZoneId
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val creditCardRepo: CreditCardRepository,
    private val classifier: CategoryClassifier,
    private val syncStatusRepo: SyncStatusRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "finance_sync_periodic"
        private const val CHANNEL_SYNC  = "finance_sync"
        private const val CHANNEL_ALERT = "finance_alerts"
        private const val NOTIF_SYNC_ID = 1001
        private const val NOTIF_BUDGET_BASE = 2000
        private const val NOTIF_DUE_BASE    = 3000

        fun enqueue(context: Context, intervalHours: Long = 6) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(
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

        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_SYNC, "Background Sync", NotificationManager.IMPORTANCE_LOW)
                        .apply { description = "Finance data sync notifications" }
                )
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ALERT, "Finance Alerts", NotificationManager.IMPORTANCE_DEFAULT)
                        .apply { description = "Budget and bill due date alerts" }
                )
            }
        }
    }

    override suspend fun doWork(): Result {
        syncStatusRepo.setRunning()
        createNotificationChannels(applicationContext)

        // SMS inbox scanning removed — RECEIVE_SMS captures new messages live via SmsBroadcastReceiver

        // 1. Re-classify any "Other" category transactions
        reclassifyOtherTransactions()

        // 2. Check budget alerts
        checkBudgetAlerts()

        // 3. Check credit card due date alerts
        checkCreditCardDueAlerts()

        syncStatusRepo.setIdle()
        return Result.success()
    }

    private suspend fun reclassifyOtherTransactions() {
        val zone  = ZoneId.systemDefault()
        val month = YearMonth.now()
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end   = month.atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        val transactions = transactionRepo.getByDateRange(start, end).first()
        transactions.filter { it.category == "Other" }.forEach { tx ->
            val category = classifier.classify(tx.rawText, tx.merchant)
            if (category != "Other") {
                transactionRepo.updateTransaction(tx.copy(category = category))
            }
        }
    }

    private suspend fun checkBudgetAlerts() {
        val zone  = ZoneId.systemDefault()
        val month = YearMonth.now()
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end   = month.atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        val categories = categoryRepo.getAll().first()
        val spends     = transactionRepo.getSpendByCategory(start, end).first()

        categories.filter { it.monthlyBudget > 0 }.forEach { cat ->
            val spent = spends.find { it.category == cat.name }?.total ?: 0.0
            val pct   = if (cat.monthlyBudget > 0) spent / cat.monthlyBudget else 0.0
            if (pct >= 0.80) {
                showNotification(
                    channelId = CHANNEL_ALERT,
                    id        = (NOTIF_BUDGET_BASE + cat.id).toInt(),
                    title     = "Budget Alert: ${cat.name}",
                    text      = "You've used ${(pct * 100).toInt()}% of your ${cat.name} budget."
                )
            }
        }
    }

    private suspend fun checkCreditCardDueAlerts() {
        val now    = System.currentTimeMillis()
        val threeDaysMs = 3L * 24 * 60 * 60 * 1000

        creditCardRepo.getAllCards().first().forEach { card ->
            val daysLeft = (card.dueDate - now) / (24 * 60 * 60 * 1000)
            if (daysLeft in 0..3) {
                showNotification(
                    channelId = CHANNEL_ALERT,
                    id        = (NOTIF_DUE_BASE + card.id).toInt(),
                    title     = "Credit Card Due Soon",
                    text      = "${card.bankName} •••• ${card.last4Digits} payment due in $daysLeft day(s)."
                )
            }
        }
    }

    private fun showNotification(channelId: String, id: Int, title: String, text: String) {
        val notif = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(id, notif)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted; silently skip
        }
    }
}
