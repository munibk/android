package com.financetracker.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Dashboard    : BottomNavItem("dashboard",    "Dashboard",    Icons.Default.Home)
    object Transactions : BottomNavItem("transactions", "Transactions", Icons.Default.List)
    object Charts       : BottomNavItem("charts",       "Charts",       Icons.Default.BarChart)
    object Categories   : BottomNavItem("categories",   "Categories",   Icons.Default.Category)
    object CreditCards  : BottomNavItem("credit_cards", "Cards",        Icons.Default.CreditCard)

    companion object {
        val all = listOf(Dashboard, Transactions, Charts, Categories, CreditCards)
    }
}
