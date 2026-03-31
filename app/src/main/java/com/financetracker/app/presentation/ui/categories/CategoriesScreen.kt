package com.financetracker.app.presentation.ui.categories

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.financetracker.app.data.db.entity.CategoryEntity
import com.financetracker.app.presentation.viewmodel.CategoriesViewModel
import java.text.NumberFormat
import java.util.Locale

private val rupeeFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(vm: CategoriesViewModel = hiltViewModel()) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget    by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Categories") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add category")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories, key = { it.id }) { cat ->
                CategoryRow(
                    category = cat,
                    onEdit   = { editTarget = cat },
                    onDelete = { vm.deleteCategory(cat) }
                )
            }
        }
    }

    if (showAddDialog) {
        CategoryDialog(
            initial   = null,
            onDismiss = { showAddDialog = false },
            onSave    = { name, icon, color, budget ->
                vm.addCategory(name, icon, color, budget)
                showAddDialog = false
            }
        )
    }

    editTarget?.let { cat ->
        CategoryDialog(
            initial   = cat,
            onDismiss = { editTarget = null },
            onSave    = { name, icon, color, budget ->
                vm.updateCategory(cat.copy(name = name, icon = icon, colorHex = color, monthlyBudget = budget))
                editTarget = null
            }
        )
    }
}

@Composable
private fun CategoryRow(
    category: CategoryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val bgColor = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (e: Exception) {
        Color(0xFF9E9E9E)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = bgColor.copy(alpha = 0.2f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(category.icon, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, fontWeight = FontWeight.Medium)
                if (category.monthlyBudget > 0) {
                    Text(
                        "Budget: ${rupeeFormat.format(category.monthlyBudget)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun CategoryDialog(
    initial: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double) -> Unit
) {
    var name   by remember { mutableStateOf(initial?.name ?: "") }
    var icon   by remember { mutableStateOf(initial?.icon ?: "📦") }
    var color  by remember { mutableStateOf(initial?.colorHex ?: "#9E9E9E") }
    var budget by remember { mutableStateOf(initial?.monthlyBudget?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Category" else "Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = icon, onValueChange = { icon = it }, label = { Text("Icon (emoji)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Color (hex, e.g. #FF5722)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = budget, onValueChange = { budget = it }, label = { Text("Monthly Budget (₹, optional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onSave(name, icon, color, budget.toDoubleOrNull() ?: 0.0)
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
