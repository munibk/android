package com.financetracker.app.service.sms

import java.security.MessageDigest

/**
 * Parses bank SMS messages to extract transaction details.
 * Supports HDFC, ICICI, SBI, Axis, Kotak, and other Indian banks.
 */
object SmsParser {

    /** Known bank sender IDs (short codes & alpha-numeric) */
    val BANK_SENDERS = setOf(
        // HDFC
        "HDFCBK", "HDFCBN", "HDFC",
        // ICICI
        "ICICIB", "ICICI",
        // SBI
        "SBIBNK", "SBICRD", "SBI",
        // Axis
        "AXISBK", "AXIS",
        // Kotak
        "KOTAKB", "KOTAK",
        // PNB
        "PNBSMS",
        // Yes Bank
        "YESBKL",
        // IndusInd
        "INDBNK",
        // IDFC
        "IDFCBK",
        // Paytm
        "PYTMBK",
        // Generic
        "BKOFBR", "CANBNK"
    )

    data class ParsedTransaction(
        val amount: Double,
        val isCredit: Boolean,
        val merchant: String,
        val dateMs: Long,
        val last4Digits: String?,
        val upiRef: String?,
        val rawText: String
    ) {
        val dedupHash: String
            get() = MessageDigest.getInstance("MD5")
                .digest("$amount|$merchant|${dateMs / (60_000 * 5)}".toByteArray())
                .joinToString("") { "%02x".format(it) }
    }

    // ── regex patterns ──────────────────────────────────────────────────────

    private val AMOUNT_PATTERN = Regex(
        """(?:Rs\.?|INR|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val DEBIT_PATTERN = Regex(
        """(?:debited|debit|spent|paid|payment of|purchase|withdrawn|dr\b)""",
        RegexOption.IGNORE_CASE
    )

    private val CREDIT_PATTERN = Regex(
        """(?:credited|credit|received|refund|cashback|cr\b)""",
        RegexOption.IGNORE_CASE
    )

    private val MERCHANT_PATTERN = Regex(
        """(?:at|to|from|merchant[:\s]+|towards\s+)([A-Za-z0-9\s\-&.,']+?)(?:\s+on|\s+for|\s+via|\s+ref|\.|\n|$)""",
        RegexOption.IGNORE_CASE
    )

    private val CARD_LAST4_PATTERN = Regex(
        """(?:card|a/c|account|ac)\s*(?:no\.?|number|ending|xx+)[-\s]*([0-9]{4})\b""",
        RegexOption.IGNORE_CASE
    )

    private val UPI_REF_PATTERN = Regex(
        """(?:UPI|ref\.?|txn\.?|transaction|refno)[:\s]*([A-Za-z0-9]{8,})""",
        RegexOption.IGNORE_CASE
    )

    private val DATE_PATTERN = Regex(
        """(\d{1,2}[-/]\d{1,2}[-/]\d{2,4})"""
    )

    // ── public API ──────────────────────────────────────────────────────────

    fun isBankSms(sender: String): Boolean =
        BANK_SENDERS.any { sender.uppercase().contains(it) }

    fun parse(body: String, receivedMs: Long = System.currentTimeMillis()): ParsedTransaction? {
        val amount = extractAmount(body) ?: return null
        val isCredit = determineTransactionType(body)
        val merchant = extractMerchant(body)
        val last4 = extractLast4(body)
        val upiRef = extractUpiRef(body)

        return ParsedTransaction(
            amount = amount,
            isCredit = isCredit,
            merchant = merchant,
            dateMs = receivedMs,
            last4Digits = last4,
            upiRef = upiRef,
            rawText = body
        )
    }

    fun extractAmount(text: String): Double? {
        val match = AMOUNT_PATTERN.find(text) ?: return null
        return match.groupValues[1].replace(",", "").toDoubleOrNull()
    }

    fun determineTransactionType(text: String): Boolean {
        val hasCreditWord = CREDIT_PATTERN.containsMatchIn(text)
        val hasDebitWord = DEBIT_PATTERN.containsMatchIn(text)
        return when {
            hasCreditWord && !hasDebitWord -> true
            else -> false  // default to debit
        }
    }

    fun extractMerchant(text: String): String {
        val match = MERCHANT_PATTERN.find(text)
        return match?.groupValues?.getOrNull(1)?.trim()?.take(50) ?: "Unknown"
    }

    fun extractLast4(text: String): String? {
        return CARD_LAST4_PATTERN.find(text)?.groupValues?.getOrNull(1)
    }

    fun extractUpiRef(text: String): String? {
        return UPI_REF_PATTERN.find(text)?.groupValues?.getOrNull(1)
    }
}
