package com.financetracker.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.financetracker.app.data.db.entity.CategoryEntity
import com.financetracker.app.data.db.entity.TransactionEntity
import com.financetracker.app.data.repository.CategoryRepository
import com.financetracker.app.data.repository.TransactionRepository
import com.financetracker.app.service.classifier.CategoryClassifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class TransactionFilter(
    val query: String = "",
    val startMs: Long = 0L,
    val endMs: Long = Long.MAX_VALUE,
    val category: String? = null,
    val source: String? = null,
    val minAmount: Double = 0.0,
    val maxAmount: Double = Double.MAX_VALUE
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val classifier: CategoryClassifier
) : ViewModel() {

    private val _filter = MutableStateFlow(TransactionFilter())
    val filter: StateFlow<TransactionFilter> = _filter.asStateFlow()

    val transactions: Flow<PagingData<TransactionEntity>> = _filter
        .flatMapLatest { f ->
            when {
                f.query.isNotBlank() -> transactionRepo.search(f.query)
                f.category != null || f.source != null ->
                    transactionRepo.getFiltered(f.startMs, f.endMs, f.category, f.source, f.minAmount, f.maxAmount)
                else -> transactionRepo.getAllPaged()
            }
        }
        .cachedIn(viewModelScope)

    val categories: StateFlow<List<CategoryEntity>> = categoryRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateFilter(filter: TransactionFilter) {
        _filter.value = filter
    }

    fun updateCategory(transaction: TransactionEntity, newCategory: String) {
        viewModelScope.launch {
            transactionRepo.updateTransaction(transaction.copy(category = newCategory))
            classifier.saveOverride(transaction.merchant, newCategory)
        }
    }

    fun updateNotes(transaction: TransactionEntity, notes: String) {
        viewModelScope.launch {
            transactionRepo.updateTransaction(transaction.copy(notes = notes))
        }
    }

    fun toggleRecurring(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepo.updateTransaction(
                transaction.copy(isRecurring = !transaction.isRecurring)
            )
        }
    }

    fun addManual(
        merchant: String,
        amount: Double,
        category: String,
        dateMs: Long,
        isCredit: Boolean,
        notes: String
    ) {
        viewModelScope.launch {
            transactionRepo.insertTransaction(
                TransactionEntity(
                    amount   = amount,
                    merchant = merchant,
                    category = category,
                    date     = dateMs,
                    source   = "Manual",
                    rawText  = "",
                    isCredit = isCredit,
                    notes    = notes
                )
            )
        }
    }

    fun delete(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepo.deleteTransaction(transaction)
        }
    }
}
