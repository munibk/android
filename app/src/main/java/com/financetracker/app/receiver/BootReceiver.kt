package com.financetracker.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.financetracker.app.workers.GmailFetchWorker
import com.financetracker.app.workers.SyncWorker

/**
 * Re-registers WorkManager jobs and ensures background sync resumes
 * after device restart or app update.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                SyncWorker.enqueue(context)
                GmailFetchWorker.enqueue(context)
            }
        }
    }
}
