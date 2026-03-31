package com.financetracker.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financetracker.app.data.db.entity.CreditCardEntity
import com.financetracker.app.data.db.entity.TransactionEntity
import com.financetracker.app.data.repository.CreditCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class CreditCardUiState(
    val cards: List<CreditCardEntity> = emptyList(),
    val selectedCardId: Long? = null,
    val cardTransactions: List<TransactionEntity> = emptyList(),
    val cardSpend: Double = 0.0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CreditCardsViewModel @Inject constructor(
    private val repo: CreditCardRepository
) : ViewModel() {

    private val _selectedCardId = MutableStateFlow<Long?>(null)
    private val month = YearMonth.now()
    private val zone  = ZoneId.systemDefault()
    private val startMs = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    private val endMs   = month.atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

    val uiState: StateFlow<CreditCardUiState> = combine(
        repo.getAllCards(),
        _selectedCardId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repo.getTransactionsForCard(id, startMs, endMs)
        },
        _selectedCardId.flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repo.getTotalSpendForCard(id, startMs, endMs)
        },
        _selectedCardId
    ) { cards, txns, spend, selectedId ->
        CreditCardUiState(
            @Suppress("UNCHECKED_CAST")
            cards            = cards as List<CreditCardEntity>,
            selectedCardId   = selectedId as? Long,
            @Suppress("UNCHECKED_CAST")
            cardTransactions = txns as List<TransactionEntity>,
            cardSpend        = (spend as? Double) ?: 0.0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CreditCardUiState())

    fun selectCard(id: Long?) { _selectedCardId.value = id }

    fun addCard(
        bankName: String,
        last4: String,
        creditLimit: Double,
        billingCycleDay: Int,
        dueDate: Long
    ) {
        viewModelScope.launch {
            repo.insertCard(
                CreditCardEntity(
                    bankName        = bankName,
                    last4Digits     = last4,
                    creditLimit     = creditLimit,
                    billingCycleDay = billingCycleDay,
                    dueDate         = dueDate
                )
            )
        }
    }

    fun updateCard(card: CreditCardEntity) {
        viewModelScope.launch { repo.updateCard(card) }
    }

    fun deleteCard(card: CreditCardEntity) {
        viewModelScope.launch { repo.deleteCard(card) }
    }
}
