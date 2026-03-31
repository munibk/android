package com.financetracker.app.service.classifier

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps merchant names to categories using a keyword dictionary.
 * User overrides always take priority.
 */
@Singleton
class CategoryClassifier @Inject constructor() {

    // User-saved overrides: merchant name (lowercase) -> category
    private val userOverrides = mutableMapOf<String, String>()

    private val keywordMap: Map<String, String> = mapOf(
        // Food & Dining
        "swiggy"       to "Food",
        "zomato"       to "Food",
        "domino"       to "Food",
        "pizza"        to "Food",
        "mcdonald"     to "Food",
        "kfc"          to "Food",
        "burger"       to "Food",
        "subway"       to "Food",
        "starbucks"    to "Food",
        "cafe"         to "Food",
        "restaurant"   to "Food",
        "hotel"        to "Food",
        "food"         to "Food",
        "eating"       to "Food",
        "dining"       to "Food",
        "dunzo"        to "Food",

        // Transport
        "uber"         to "Transport",
        "ola"          to "Transport",
        "rapido"       to "Transport",
        "metro"        to "Transport",
        "petrol"       to "Transport",
        "fuel"         to "Transport",
        "parking"      to "Transport",
        "irctc"        to "Transport",
        "redbus"       to "Transport",
        "makemytrip"   to "Transport",
        "indigo"       to "Transport",
        "spicejet"     to "Transport",
        "air india"    to "Transport",
        "goibibo"      to "Transport",

        // Shopping
        "amazon"       to "Shopping",
        "flipkart"     to "Shopping",
        "myntra"       to "Shopping",
        "ajio"         to "Shopping",
        "nykaa"        to "Shopping",
        "meesho"       to "Shopping",
        "snapdeal"     to "Shopping",
        "reliance"     to "Shopping",
        "d mart"       to "Shopping",
        "big bazaar"   to "Shopping",
        "croma"        to "Shopping",
        "apple"        to "Shopping",
        "samsung"      to "Shopping",

        // Bills & Utilities
        "airtel"       to "Bills",
        "jio"          to "Bills",
        "vodafone"     to "Bills",
        "bsnl"         to "Bills",
        "electricity"  to "Bills",
        "bescom"       to "Bills",
        "msedcl"       to "Bills",
        "gas"          to "Bills",
        "water"        to "Bills",
        "broadband"    to "Bills",

        // Entertainment
        "netflix"      to "Entertainment",
        "hotstar"      to "Entertainment",
        "amazon prime" to "Entertainment",
        "sonyliv"      to "Entertainment",
        "zee5"         to "Entertainment",
        "bookmyshow"   to "Entertainment",
        "spotify"      to "Entertainment",
        "youtube"      to "Entertainment",
        "gaming"       to "Entertainment",
        "pvr"          to "Entertainment",
        "inox"         to "Entertainment",

        // Health
        "pharmeasy"    to "Health",
        "1mg"          to "Health",
        "netmeds"      to "Health",
        "apollo"       to "Health",
        "medplus"      to "Health",
        "hospital"     to "Health",
        "clinic"       to "Health",
        "doctor"       to "Health",
        "lab"          to "Health",
        "pharmacy"     to "Health",

        // Salary / Income
        "salary"       to "Salary",
        "payroll"      to "Salary",
        "wages"        to "Salary",
        "neft"         to "Salary",  // common for salary transfers

        // Savings / Investment
        "zerodha"      to "Savings",
        "groww"        to "Savings",
        "mutual fund"  to "Savings",
        "ppf"          to "Savings",
        "sip"          to "Savings",
        "fd"           to "Savings",
        "fixed deposit" to "Savings"
    )

    /**
     * Save a user-defined override for a merchant.
     * Future calls to [classify] will always use this mapping.
     */
    fun saveOverride(merchant: String, category: String) {
        userOverrides[merchant.lowercase().trim()] = category
    }

    fun getOverride(merchant: String): String? =
        userOverrides[merchant.lowercase().trim()]

    /**
     * Classify a transaction given its merchant name and raw text.
     * Priority: user override > keyword match > raw text scan > "Other"
     */
    suspend fun classify(rawText: String, merchant: String): String {
        val merchantLower = merchant.lowercase().trim()

        // 1. User override
        userOverrides[merchantLower]?.let { return it }

        // 2. Keyword match on merchant name
        keywordMap.entries.firstOrNull { merchantLower.contains(it.key) }
            ?.let { return it.value }

        // 3. Scan raw text for category keywords
        val rawLower = rawText.lowercase()
        keywordMap.entries.firstOrNull { rawLower.contains(it.key) }
            ?.let { return it.value }

        return "Other"
    }
}
