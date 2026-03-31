package com.financetracker.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.financetracker.app.data.db.dao.CategoryDao
import com.financetracker.app.data.db.dao.CreditCardDao
import com.financetracker.app.data.db.dao.TransactionDao
import com.financetracker.app.data.db.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        CreditCardEntity::class,
        CreditCardTransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun creditCardDao(): CreditCardDao

    companion object {
        const val DATABASE_NAME = "finance_tracker.db"

        private val defaultCategories = listOf(
            CategoryEntity(name = "Food",          icon = "🍔", colorHex = "#FF5722"),
            CategoryEntity(name = "Transport",     icon = "🚗", colorHex = "#2196F3"),
            CategoryEntity(name = "Shopping",      icon = "🛍️",  colorHex = "#9C27B0"),
            CategoryEntity(name = "Bills",         icon = "📄", colorHex = "#607D8B"),
            CategoryEntity(name = "Entertainment", icon = "🎬", colorHex = "#FF9800"),
            CategoryEntity(name = "Health",        icon = "💊", colorHex = "#F44336"),
            CategoryEntity(name = "Salary",        icon = "💰", colorHex = "#4CAF50"),
            CategoryEntity(name = "Savings",       icon = "🏦", colorHex = "#009688"),
            CategoryEntity(name = "Other",         icon = "📦", colorHex = "#9E9E9E")
        )

        fun buildDatabase(context: Context): FinanceDatabase =
            Room.databaseBuilder(context, FinanceDatabase::class.java, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate default categories on first create
                        CoroutineScope(Dispatchers.IO).launch {
                            buildDatabase(context).categoryDao()
                                .insertAll(defaultCategories)
                        }
                    }
                })
                .build()
    }
}
