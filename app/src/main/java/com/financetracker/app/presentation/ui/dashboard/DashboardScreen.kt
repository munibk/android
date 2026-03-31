package com.financetracker.app.presentation.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financetracker.app.data.db.entity.TransactionEntity
import com.financetracker.app.presentation.viewmodel.DashboardViewModel
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

private val rupeeFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
private val monthFmt    = DateTimeFormatter.ofPattern("MMMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    vm: DashboardViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finance Tracker", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month selector
            item { MonthSelector(state.selectedMonth, vm::selectMonth) }

            // Income vs Expense header card
            item {
                IncomeExpenseCard(
                    income   = state.totalIncome,
                    expenses = state.totalExpenses,
                    saved    = state.saved
                )
            }

            // Savings ring
            item {
                SavingsRingCard(savedPercent = state.savedPercent, saved = state.saved)
            }

            // Top 5 categories
            if (state.topCategories.isNotEmpty()) {
                item {
                    Text(
                        "Top Spending Categories",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.topCategories) { catSpend ->
                            val entity = state.categories.find { it.name == catSpend.category }
                            CategoryChip(
                                name   = catSpend.category,
                                amount = catSpend.total,
                                color  = parseCategoryColor(entity?.colorHex),
                                icon   = entity?.icon ?: "📦"
                            )
                        }
                    }
                }
            }

            // Budget status
            if (state.categories.any { it.monthlyBudget > 0 }) {
                item { Text("Budget Status", style = MaterialTheme.typography.titleMedium) }
                items(state.categories.filter { it.monthlyBudget > 0 }) { cat ->
                    val spent = state.topCategories
                        .find { it.category == cat.name }?.total ?: 0.0
                    BudgetRow(category = cat.name, spent = spent, budget = cat.monthlyBudget)
                }
            }

            // Recent transactions
            item { Text("Recent Transactions", style = MaterialTheme.typography.titleMedium) }
            items(state.recentTransactions) { tx ->
                TransactionRow(tx)
            }
        }
    }
}

@Composable
private fun MonthSelector(current: YearMonth, onSelect: (YearMonth) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onSelect(current.minusMonths(1)) }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
        }
        Text(
            text  = current.format(monthFmt),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = { onSelect(current.plusMonths(1)) }) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
        }
    }
}

@Composable
private fun IncomeExpenseCard(income: Double, expenses: Double, saved: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AmountColumn("Income",   income,  Color(0xFF43A047))
            Divider(modifier = Modifier.height(60.dp).width(1.dp))
            AmountColumn("Expenses", expenses, Color(0xFFE53935))
            Divider(modifier = Modifier.height(60.dp).width(1.dp))
            AmountColumn("Saved",    saved,  Color(0xFF00897B))
        }
    }
}

@Composable
private fun AmountColumn(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(
            rupeeFormat.format(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun SavingsRingCard(savedPercent: Float, saved: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ring visual using LinearProgressIndicator as fallback
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { savedPercent / 100f },
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 6.dp,
                    color = Color(0xFF00897B),
                    trackColor = Color(0xFF004D40)
                )
                Text(
                    "${savedPercent.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Savings Rate", style = MaterialTheme.typography.bodyMedium)
                Text(
                    rupeeFormat.format(saved),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF00897B)
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(name: String, amount: Double, color: Color, icon: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(name, style = MaterialTheme.typography.labelSmall)
            Text(
                rupeeFormat.format(amount),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun BudgetRow(category: String, spent: Double, budget: Double) {
    val progress = if (budget > 0) (spent / budget).coerceIn(0.0, 1.0).toFloat() else 0f
    val color = when {
        progress >= 0.9f -> Color(0xFFE53935)
        progress >= 0.7f -> Color(0xFFFF9800)
        else             -> Color(0xFF43A047)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(category, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${rupeeFormat.format(spent)} / ${rupeeFormat.format(budget)}",
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun TransactionRow(tx: TransactionEntity) {
    val amtColor = if (tx.isCredit) Color(0xFF43A047) else Color(0xFFE53935)
    val prefix   = if (tx.isCredit) "+" else "-"

    ListItem(
        headlineContent = { Text(tx.merchant, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Text(
                tx.category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$prefix${rupeeFormat.format(tx.amount)}",
                    color = amtColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    tx.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

private fun parseCategoryColor(hex: String?): Color {
    if (hex.isNullOrBlank()) return Color(0xFF9E9E9E)
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: IllegalArgumentException) {
        Color(0xFF9E9E9E)
    }
}
