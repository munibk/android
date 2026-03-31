package com.financetracker.app.data.repository

import androidx.paging.PagingData
import com.financetracker.app.data.db.dao.CreditCardDao
import com.financetracker.app.data.db.entity.CreditCardEntity
import com.financetracker.app.data.db.entity.CreditCardTransactionEntity
import com.financetracker.app.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditCardRepository @Inject constructor(
    private val dao: CreditCardDao
) {
    fun getAllCards(): Flow<List<CreditCardEntity>> = dao.getAllCards()

    suspend fun getCardById(id: Long): CreditCardEntity? = dao.getCardById(id)

    suspend fun getCardByLast4(last4: String): CreditCardEntity? = dao.getCardByLast4(last4)

    suspend fun insertCard(card: CreditCardEntity): Long = dao.insertCard(card)

    suspend fun updateCard(card: CreditCardEntity) = dao.updateCard(card)

    suspend fun deleteCard(card: CreditCardEntity) = dao.deleteCard(card)

    suspend fun linkTransaction(cardId: Long, transactionId: Long) =
        dao.linkTransaction(CreditCardTransactionEntity(cardId = cardId, transactionId = transactionId))

    fun getTransactionsForCard(
        cardId: Long,
        startMs: Long,
        endMs: Long
    ): Flow<List<TransactionEntity>> = dao.getTransactionsForCard(cardId, startMs, endMs)

    fun getTotalSpendForCard(cardId: Long, startMs: Long, endMs: Long): Flow<Double?> =
        dao.getTotalSpendForCard(cardId, startMs, endMs)
}
