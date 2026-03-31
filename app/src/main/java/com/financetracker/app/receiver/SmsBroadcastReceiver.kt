package com.financetracker.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.financetracker.app.data.db.entity.TransactionEntity
import com.financetracker.app.data.repository.TransactionRepository
import com.financetracker.app.service.sms.SmsParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: TransactionRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val receivedMs = System.currentTimeMillis()

        messages.forEach { smsMessage ->
            val sender = smsMessage.originatingAddress ?: return@forEach
            val body   = smsMessage.messageBody ?: return@forEach

            if (!SmsParser.isBankSms(sender)) return@forEach

            val parsed = SmsParser.parse(body, receivedMs) ?: return@forEach

            CoroutineScope(Dispatchers.IO).launch {
                val exists = repository.getByDedupHash(parsed.dedupHash)
                if (exists == null) {
                    repository.insertTransaction(
                        TransactionEntity(
                            amount    = parsed.amount,
                            merchant  = parsed.merchant,
                            category  = "Other",
                            date      = parsed.dateMs,
                            source    = "SMS",
                            rawText   = parsed.rawText,
                            isCredit  = parsed.isCredit,
                            dedupHash = parsed.dedupHash
                        )
                    )
                }
            }
        }
    }
}
