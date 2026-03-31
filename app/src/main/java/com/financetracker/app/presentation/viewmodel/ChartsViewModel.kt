package com.financetracker.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financetracker.app.data.db.dao.TransactionDao.CategorySpend
import com.financetracker.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

enum class ChartPeriod { WEEKLY, MONTHLY, YEARLY }

data class MonthlyTotal(val month: YearMonth, val expenses: Double, val income: Double)

data class ChartsUiState(
    val period: ChartPeriod = ChartPeriod.MONTHLY,
    val selectedCategories: Set<String> = emptySet(),
    val categorySpends: List<CategorySpend> = emptyList(),
    val monthlyTotals: List<MonthlyTotal> = emptyList(),
    val selectedMonth: YearMonth = YearMonth.now()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChartsViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    private val _period = MutableStateFlow(ChartPeriod.MONTHLY)
    private val _selectedCategories = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<ChartsUiState> = combine(
        _period,
        _selectedCategories,
        _selectedMonth.flatMapLatest { month ->
            val zone  = ZoneId.systemDefault()
            val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end   = month.atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
            transactionRepo.getSpendByCategory(start, end)
        },
        _selectedMonth
    ) { period, cats, spends, month ->
        ChartsUiState(
            period             = period,
            selectedCategories = cats,
            categorySpends     = spends,
            selectedMonth      = month
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChartsUiState())

    fun setPeriod(period: ChartPeriod) { _period.value = period }
    fun setMonth(month: YearMonth)     { _selectedMonth.value = month }
    fun toggleCategory(name: String) {
        _selectedCategories.update { set ->
            if (name in set) set - name else set + name
        }
    }
}
