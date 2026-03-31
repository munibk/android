package com.financetracker.app.presentation.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.financetracker.app.data.db.entity.CategoryEntity
import com.financetracker.app.data.db.entity.TransactionEntity
import com.financetracker.app.presentation.viewmodel.TransactionFilter
import com.financetracker.app.presentation.viewmodel.TransactionsViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private val rupeeFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
private val dateFmt     = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(vm: TransactionsViewModel = hiltViewModel()) {
    val filter     by vm.filter.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val transactions: LazyPagingItems<TransactionEntity> = vm.transactions.collectAsLazyPagingItems()

    var showAddSheet    by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedTx      by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, "Filter")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, "Add transaction")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = filter.query,
                onValueChange = { vm.updateFilter(filter.copy(query = it)) },
                placeholder = { Text("Search transactions…") },
                leadingIcon  = { Icon(Icons.Default.Search, null) },
                modifier     = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine   = true,
                shape        = RoundedCornerShape(28.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    count = transactions.itemCount,
                    key   = transactions.itemKey { it.id }
                ) { index ->
                    val tx = transactions[index] ?: return@items
                    TransactionListItem(tx, onClick = { selectedTx = tx })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    // Detail / Edit bottom sheet
    selectedTx?.let { tx ->
        TransactionDetailSheet(
            transaction = tx,
            categories  = categories,
            onDismiss   = { selectedTx = null },
            onUpdateCategory = { vm.updateCategory(tx, it) },
            onUpdateNotes    = { vm.updateNotes(tx, it) },
            onToggleRecurring = { vm.toggleRecurring(tx) }
        )
    }

    // Add manual transaction sheet
    if (showAddSheet) {
        AddTransactionSheet(
            categories = categories,
            onDismiss  = { showAddSheet = false },
            onAdd      = { merchant, amount, category, dateMs, isCredit, notes ->
                vm.addManual(merchant, amount, category, dateMs, isCredit, notes)
                showAddSheet = false
            }
        )
    }

    // Filter sheet
    if (showFilterSheet) {
        FilterSheet(
            currentFilter = filter,
            categories    = categories,
            onApply       = { vm.updateFilter(it); showFilterSheet = false },
            onDismiss     = { showFilterSheet = false }
        )
    }
}

@Composable
private fun TransactionListItem(tx: TransactionEntity, onClick: () -> Unit) {
    val amtColor = if (tx.isCredit) Color(0xFF43A047) else Color(0xFFE53935)
    val prefix   = if (tx.isCredit) "+" else "-"

    ListItem(
        modifier       = Modifier.padding(vertical = 2.dp),
        headlineContent = { Text(tx.merchant, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tx.category, style = MaterialTheme.typography.bodySmall)
                Text("·", style = MaterialTheme.typography.bodySmall)
                Text(dateFmt.format(Date(tx.date)), style = MaterialTheme.typography.bodySmall)
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$prefix${rupeeFormat.format(tx.amount)}",
                    color = amtColor,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        tx.source,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        leadingContent = {
            IconButton(onClick = onClick) {
                Icon(Icons.Default.Info, contentDescription = "Details")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailSheet(
    transaction: TransactionEntity,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onUpdateCategory: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onToggleRecurring: () -> Unit
) {
    var notes    by remember { mutableStateOf(transaction.notes) }
    var showCatPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Transaction Details", style = MaterialTheme.typography.titleLarge)

            InfoRow("Merchant", transaction.merchant)
            InfoRow("Amount", rupeeFormat.format(transaction.amount))
            InfoRow("Date", dateFmt.format(Date(transaction.date)))
            InfoRow("Source", transaction.source)

            // Raw text
            if (transaction.rawText.isNotBlank()) {
                Text("Raw Text", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(transaction.rawText, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                }
            }

            // Category selector
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Category: ${transaction.category}", modifier = Modifier.weight(1f))
                TextButton(onClick = { showCatPicker = !showCatPicker }) { Text("Change") }
            }

            if (showCatPicker) {
                categories.forEach { cat ->
                    TextButton(onClick = { onUpdateCategory(cat.name); showCatPicker = false }) {
                        Text("${cat.icon} ${cat.name}")
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth()
            )

            // Recurring toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recurring", modifier = Modifier.weight(1f))
                Switch(checked = transaction.isRecurring, onCheckedChange = { onToggleRecurring() })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick  = { onUpdateNotes(notes); onDismiss() },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionSheet(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onAdd: (String, Double, String, Long, Boolean, String) -> Unit
) {
    var merchant  by remember { mutableStateOf("") }
    var amount    by remember { mutableStateOf("") }
    var category  by remember { mutableStateOf("Other") }
    var isCredit  by remember { mutableStateOf(false) }
    var notes     by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add Transaction", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = merchant, onValueChange = { merchant = it },
                label = { Text("Merchant") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = amount, onValueChange = { amount = it },
                label = { Text("Amount (₹)") }, modifier = Modifier.fillMaxWidth())

            // Category dropdown (simple text chips)
            Text("Category", style = MaterialTheme.typography.labelSmall)
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories.size) { i ->
                    val cat = categories[i]
                    FilterChip(
                        selected = category == cat.name,
                        onClick  = { category = cat.name },
                        label    = { Text("${cat.icon} ${cat.name}") }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Credit (income)", modifier = Modifier.weight(1f))
                Switch(checked = isCredit, onCheckedChange = { isCredit = it })
            }

            OutlinedTextField(value = notes, onValueChange = { notes = it },
                label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    onAdd(merchant, amt, category, System.currentTimeMillis(), isCredit, notes)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add Transaction") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    currentFilter: TransactionFilter,
    categories: List<CategoryEntity>,
    onApply: (TransactionFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(currentFilter.category) }
    var selectedSource   by remember { mutableStateOf(currentFilter.source) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Filter Transactions", style = MaterialTheme.typography.titleLarge)

            Text("Category", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick  = { selectedCategory = null },
                    label    = { Text("All") }
                )
                categories.take(5).forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.name,
                        onClick  = { selectedCategory = cat.name },
                        label    = { Text(cat.name) }
                    )
                }
            }

            Text("Source", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(null, "SMS", "Gmail", "Manual").forEach { src ->
                    FilterChip(
                        selected = selectedSource == src,
                        onClick  = { selectedSource = src },
                        label    = { Text(src ?: "All") }
                    )
                }
            }

            Button(
                onClick  = { onApply(currentFilter.copy(category = selectedCategory, source = selectedSource)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Apply Filters") }
            Spacer(Modifier.height(8.dp))
        }
    }
}
