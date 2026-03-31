package com.financetracker.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankName: String,
    val last4Digits: String,
    val creditLimit: Double,
    val billingCycleDay: Int,   // day of month billing cycle starts
    val dueDate: Long,          // epoch millis of next payment due date
    val currentBalance: Double = 0.0
)
