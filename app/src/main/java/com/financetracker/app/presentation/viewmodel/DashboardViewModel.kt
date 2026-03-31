package com.financetracker.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financetracker.app.data.db.dao.TransactionDao.CategorySpend
import com.financetracker.app.data.db.entity.CategoryEntity
import com.financetracker.app.data.db.entity.TransactionEntity
import com.financetracker.app.data.repository.CategoryRepository
import com.financetracker.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class DashboardUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val topCategories: List<CategorySpend> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = false
) {
    val saved: Double get() = totalIncome - totalExpenses
    val savedPercent: Float
        get() = if (totalIncome > 0) (saved / totalIncome * 100).toFloat().coerceIn(0f, 100f) else 0f
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())

    private val monthBounds: Flow<Pair<Long, Long>> = _selectedMonth.map { month ->
        val zone = ZoneId.systemDefault()
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end   = month.atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
        start to end
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        _selectedMonth,
        monthBounds.flatMapLatest { (s, e) -> transactionRepo.getTotalIncome(s, e) },
        monthBounds.flatMapLatest { (s, e) -> transactionRepo.getTotalExpenses(s, e) },
        monthBounds.flatMapLatest { (s, e) -> transactionRepo.getSpendByCategory(s, e) },
        transactionRepo.getRecent(),
        categoryRepo.getAll()
    ) { values ->
        val month      = values[0] as YearMonth
        val income     = (values[1] as? Double) ?: 0.0
        val expenses   = (values[2] as? Double) ?: 0.0
        @Suppress("UNCHECKED_CAST")
        val categories = values[3] as List<CategorySpend>
        @Suppress("UNCHECKED_CAST")
        val recent     = values[4] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val catEntities = values[5] as List<CategoryEntity>

        DashboardUiState(
            selectedMonth      = month,
            totalIncome        = income,
            totalExpenses      = expenses,
            topCategories      = categories.take(5),
            recentTransactions = recent,
            categories         = catEntities
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun selectMonth(month: YearMonth) {
        _selectedMonth.value = month
    }
}
