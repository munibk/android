package com.financetracker.app.presentation.ui.creditcards

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financetracker.app.data.db.entity.CreditCardEntity
import com.financetracker.app.data.db.entity.TransactionEntity
import com.financetracker.app.presentation.viewmodel.CreditCardsViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private val rupeeFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
private val dateFmt     = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardsScreen(vm: CreditCardsViewModel = hiltViewModel()) {
    val state   by vm.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Credit Cards") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, "Add card")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.cards.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No credit cards added yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // Horizontal card scroll
                LazyRow(
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.cards, key = { it.id }) { card ->
                        CreditCardWidget(
                            card     = card,
                            selected = state.selectedCardId == card.id,
                            onClick  = { vm.selectCard(if (state.selectedCardId == card.id) null else card.id) }
                        )
                    }
                }

                // Card detail
                state.cards.find { it.id == state.selectedCardId }?.let { card ->
                    CardDetailSection(
                        card         = card,
                        transactions = state.cardTransactions,
                        totalSpend   = state.cardSpend
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddCardSheet(
            onDismiss = { showAdd = false },
            onAdd     = { bank, last4, limit, day, due ->
                vm.addCard(bank, last4, limit, day, due)
                showAdd = false
            }
        )
    }
}

@Composable
private fun CreditCardWidget(
    card: CreditCardEntity,
    selected: Boolean,
    onClick: () -> Unit
) {
    val utilisation = if (card.creditLimit > 0)
        (card.currentBalance / card.creditLimit * 100).toInt().coerceIn(0, 100)
    else 0
    val utilisationColor = when {
        utilisation >= 75 -> Color(0xFFE53935)
        utilisation >= 50 -> Color(0xFFFF9800)
        else              -> Color(0xFF43A047)
    }

    Card(
        onClick = onClick,
        modifier = Modifier.width(220.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(card.bankName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Text("•••• ${card.last4Digits}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text("Limit: ${rupeeFormat.format(card.creditLimit)}", style = MaterialTheme.typography.bodySmall)
            Text("Balance: ${rupeeFormat.format(card.currentBalance)}", style = MaterialTheme.typography.bodySmall)

            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    progress = { utilisation / 100f },
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp,
                    color = utilisationColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text("$utilisation% used", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun CardDetailSection(
    card: CreditCardEntity,
    transactions: List<TransactionEntity>,
    totalSpend: Double
) {
    val context = LocalContext.current
    val daysUntilDue = TimeUnit.MILLISECONDS.toDays(card.dueDate - System.currentTimeMillis())
        .coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Divider()

        // Due date & Pay button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Due: ${dateFmt.format(Date(card.dueDate))}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "$daysUntilDue days remaining",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (daysUntilDue <= 3) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay"))
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                }
            }) {
                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Pay Bill")
            }
        }

        Text(
            "This Cycle Spend: ${rupeeFormat.format(totalSpend)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFE53935)
        )

        Text("Transactions", style = MaterialTheme.typography.titleSmall)

        LazyColumn(
            modifier = Modifier.heightIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(transactions) { tx ->
                ListItem(
                    headlineContent = { Text(tx.merchant) },
                    supportingContent = { Text(dateFmt.format(Date(tx.date))) },
                    trailingContent = {
                        Text(
                            rupeeFormat.format(tx.amount),
                            color = if (tx.isCredit) Color(0xFF43A047) else Color(0xFFE53935),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCardSheet(
    onDismiss: () -> Unit,
    onAdd: (String, String, Double, Int, Long) -> Unit
) {
    var bankName       by remember { mutableStateOf("") }
    var last4          by remember { mutableStateOf("") }
    var creditLimit    by remember { mutableStateOf("") }
    var billingCycleDay by remember { mutableStateOf("1") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add Credit Card", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(value = bankName, onValueChange = { bankName = it },
                label = { Text("Bank Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = last4, onValueChange = { if (it.length <= 4) last4 = it },
                label = { Text("Last 4 Digits") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = creditLimit, onValueChange = { creditLimit = it },
                label = { Text("Credit Limit (₹)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = billingCycleDay, onValueChange = { billingCycleDay = it },
                label = { Text("Billing Cycle Day (1-28)") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    val limit = creditLimit.toDoubleOrNull() ?: return@Button
                    val cycleDay = billingCycleDay.toIntOrNull()?.coerceIn(1, 28) ?: 1
                    // Calculate approximate next due date
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, cycleDay)
                        add(Calendar.MONTH, 1)
                    }
                    onAdd(bankName, last4, limit, cycleDay, cal.timeInMillis)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add Card") }
            Spacer(Modifier.height(8.dp))
        }
    }
}
