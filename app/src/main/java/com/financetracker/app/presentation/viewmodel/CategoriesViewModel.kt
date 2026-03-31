package com.financetracker.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financetracker.app.data.db.entity.CategoryEntity
import com.financetracker.app.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repo: CategoryRepository
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = repo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCategory(name: String, icon: String, colorHex: String, budget: Double) {
        viewModelScope.launch {
            repo.insert(CategoryEntity(name = name, icon = icon, colorHex = colorHex, monthlyBudget = budget))
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch { repo.update(category) }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch { repo.delete(category) }
    }
}
