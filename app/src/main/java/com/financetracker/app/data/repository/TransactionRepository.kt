package com.financetracker.app.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.financetracker.app.data.db.dao.TransactionDao
import com.financetracker.app.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val dao: TransactionDao
) {
    companion object {
        private const val PAGE_SIZE = 30
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long =
        dao.insert(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        dao.update(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        dao.delete(transaction)

    suspend fun getById(id: Long): TransactionEntity? =
        dao.getById(id)

    fun getAllPaged(): Flow<PagingData<TransactionEntity>> =
        Pager(PagingConfig(pageSize = PAGE_SIZE)) { dao.getAllPaged() }.flow

    fun getRecent(): Flow<List<TransactionEntity>> =
        dao.getRecent()

    fun getByDateRange(startMs: Long, endMs: Long): Flow<List<TransactionEntity>> =
        dao.getByDateRange(startMs, endMs)

    fun getFiltered(
        startMs: Long,
        endMs: Long,
        category: String?,
        source: String?,
        minAmount: Double = 0.0,
        maxAmount: Double = Double.MAX_VALUE
    ): Flow<PagingData<TransactionEntity>> =
        Pager(PagingConfig(pageSize = PAGE_SIZE)) {
            dao.getFiltered(startMs, endMs, category, source, minAmount, maxAmount)
        }.flow

    fun search(query: String): Flow<PagingData<TransactionEntity>> =
        Pager(PagingConfig(pageSize = PAGE_SIZE)) { dao.search(query) }.flow

    fun getSpendByCategory(
        startMs: Long,
        endMs: Long
    ): Flow<List<TransactionDao.CategorySpend>> =
        dao.getSpendByCategory(startMs, endMs)

    fun getTotalExpenses(startMs: Long, endMs: Long): Flow<Double?> =
        dao.getTotalExpenses(startMs, endMs)

    fun getTotalIncome(startMs: Long, endMs: Long): Flow<Double?> =
        dao.getTotalIncome(startMs, endMs)

    suspend fun getAllDedupHashes(): List<String> =
        dao.getAllDedupHashes()

    suspend fun getByDedupHash(hash: String): TransactionEntity? =
        dao.getByDedupHash(hash)
}
