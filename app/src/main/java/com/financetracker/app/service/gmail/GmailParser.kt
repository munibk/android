package com.financetracker.app.service.gmail

import com.financetracker.app.service.sms.SmsParser
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Re-uses the same regex patterns from SmsParser to extract transaction
 * details from Gmail email bodies.
 */
@Singleton
class GmailParser @Inject constructor() {

    fun parse(email: FetchedEmail): SmsParser.ParsedTransaction? {
        val text = "${email.subject}\n${email.body}"
        val amount   = SmsParser.extractAmount(text) ?: return null
        val isCredit = SmsParser.determineTransactionType(text)
        val merchant = SmsParser.extractMerchant(text)
        val last4    = SmsParser.extractLast4(text)
        val upiRef   = SmsParser.extractUpiRef(text)

        return SmsParser.ParsedTransaction(
            amount      = amount,
            isCredit    = isCredit,
            merchant    = merchant,
            dateMs      = email.receivedMs,
            last4Digits = last4,
            upiRef      = upiRef,
            rawText     = text.take(500) // cap stored raw text
        )
    }
}
