package com.financetracker.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val category: String,
    val date: Long,             // epoch millis
    val source: String,         // "SMS" | "Gmail" | "Manual"
    val rawText: String,
    val isCredit: Boolean,
    val notes: String = "",
    val isRecurring: Boolean = false,
    val dedupHash: String = "" // hash(amount+merchant+date) for deduplication
)
