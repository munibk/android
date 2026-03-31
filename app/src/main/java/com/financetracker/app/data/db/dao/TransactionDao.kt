package com.financetracker.app.data.db.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.financetracker.app.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllPaged(): PagingSource<Int, TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT 5")
    fun getRecent(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE date BETWEEN :startMs AND :endMs
        ORDER BY date DESC
    """)
    fun getByDateRange(startMs: Long, endMs: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE date BETWEEN :startMs AND :endMs
          AND (:category IS NULL OR category = :category)
          AND (:source IS NULL OR source = :source)
          AND amount BETWEEN :minAmount AND :maxAmount
        ORDER BY date DESC
    """)
    fun getFiltered(
        startMs: Long,
        endMs: Long,
        category: String?,
        source: String?,
        minAmount: Double,
        maxAmount: Double
    ): PagingSource<Int, TransactionEntity>

    @Query("""
        SELECT * FROM transactions
        WHERE (merchant LIKE '%' || :query || '%'
               OR rawText LIKE '%' || :query || '%')
        ORDER BY date DESC
    """)
    fun search(query: String): PagingSource<Int, TransactionEntity>

    @Query("""
        SELECT category, SUM(amount) as total
        FROM transactions
        WHERE date BETWEEN :startMs AND :endMs AND isCredit = 0
        GROUP BY category
        ORDER BY total DESC
    """)
    fun getSpendByCategory(startMs: Long, endMs: Long): Flow<List<CategorySpend>>

    @Query("""
        SELECT SUM(amount) FROM transactions
        WHERE date BETWEEN :startMs AND :endMs AND isCredit = 0
    """)
    fun getTotalExpenses(startMs: Long, endMs: Long): Flow<Double?>

    @Query("""
        SELECT SUM(amount) FROM transactions
        WHERE date BETWEEN :startMs AND :endMs AND isCredit = 1
    """)
    fun getTotalIncome(startMs: Long, endMs: Long): Flow<Double?>

    @Query("SELECT dedupHash FROM transactions WHERE dedupHash != ''")
    suspend fun getAllDedupHashes(): List<String>

    @Query("SELECT * FROM transactions WHERE dedupHash = :hash LIMIT 1")
    suspend fun getByDedupHash(hash: String): TransactionEntity?

    data class CategorySpend(val category: String, val total: Double)
}
