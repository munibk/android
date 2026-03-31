package com.financetracker.app.data.db.dao

import androidx.room.*
import com.financetracker.app.data.db.entity.CreditCardEntity
import com.financetracker.app.data.db.entity.CreditCardTransactionEntity
import com.financetracker.app.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CreditCardEntity): Long

    @Update
    suspend fun updateCard(card: CreditCardEntity)

    @Delete
    suspend fun deleteCard(card: CreditCardEntity)

    @Query("SELECT * FROM credit_cards ORDER BY bankName ASC")
    fun getAllCards(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    suspend fun getCardById(id: Long): CreditCardEntity?

    @Query("SELECT * FROM credit_cards WHERE last4Digits = :last4")
    suspend fun getCardByLast4(last4: String): CreditCardEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkTransaction(link: CreditCardTransactionEntity)

    @Query("""
        SELECT t.* FROM transactions t
        INNER JOIN credit_card_transactions cct ON cct.transactionId = t.id
        WHERE cct.cardId = :cardId
          AND t.date BETWEEN :startMs AND :endMs
        ORDER BY t.date DESC
    """)
    fun getTransactionsForCard(
        cardId: Long,
        startMs: Long,
        endMs: Long
    ): Flow<List<TransactionEntity>>

    @Query("""
        SELECT SUM(t.amount) FROM transactions t
        INNER JOIN credit_card_transactions cct ON cct.transactionId = t.id
        WHERE cct.cardId = :cardId AND t.isCredit = 0
          AND t.date BETWEEN :startMs AND :endMs
    """)
    fun getTotalSpendForCard(cardId: Long, startMs: Long, endMs: Long): Flow<Double?>

    data class CardSpend(val cardId: Long, val total: Double)

    @Query("""
        SELECT cct.cardId, SUM(t.amount) as total
        FROM transactions t
        INNER JOIN credit_card_transactions cct ON cct.transactionId = t.id
        WHERE t.isCredit = 0 AND t.date BETWEEN :startMs AND :endMs
        GROUP BY cct.cardId
    """)
    fun getSpendGroupedByCard(startMs: Long, endMs: Long): Flow<List<CardSpend>>
}
