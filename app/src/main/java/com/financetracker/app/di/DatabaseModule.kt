package com.financetracker.app.di

import android.content.Context
import com.financetracker.app.data.db.FinanceDatabase
import com.financetracker.app.data.db.dao.CategoryDao
import com.financetracker.app.data.db.dao.CreditCardDao
import com.financetracker.app.data.db.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FinanceDatabase =
        FinanceDatabase.buildDatabase(context)

    @Provides
    fun provideTransactionDao(db: FinanceDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: FinanceDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideCreditCardDao(db: FinanceDatabase): CreditCardDao = db.creditCardDao()
}
