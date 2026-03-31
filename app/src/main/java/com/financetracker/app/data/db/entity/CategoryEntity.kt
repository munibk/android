package com.financetracker.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,           // icon name / emoji
    val colorHex: String,       // e.g. "#FF5722"
    val monthlyBudget: Double = 0.0
)
