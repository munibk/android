package com.financetracker.app.di

import com.financetracker.app.data.db.dao.CategoryDao
import com.financetracker.app.data.db.dao.CreditCardDao
import com.financetracker.app.data.db.dao.TransactionDao
import com.financetracker.app.data.repository.CategoryRepository
import com.financetracker.app.data.repository.CreditCardRepository
import com.financetracker.app.data.repository.TransactionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideTransactionRepository(dao: TransactionDao): TransactionRepository =
        TransactionRepository(dao)

    @Provides
    @Singleton
    fun provideCategoryRepository(dao: CategoryDao): CategoryRepository =
        CategoryRepository(dao)

    @Provides
    @Singleton
    fun provideCreditCardRepository(dao: CreditCardDao): CreditCardRepository =
        CreditCardRepository(dao)
}
