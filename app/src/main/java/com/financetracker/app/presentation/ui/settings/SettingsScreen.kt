package com.financetracker.app.presentation.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financetracker.app.data.repository.SyncState
import com.financetracker.app.presentation.viewmodel.ConnectionTestResult
import com.financetracker.app.presentation.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.*

private val dateFmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val state      by vm.uiState.collectAsStateWithLifecycle()
    val syncStatus by vm.syncStatus.collectAsStateWithLifecycle()
    val context    = LocalContext.current
    val activity   = context as? Activity

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasSmsPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECEIVE_SMS
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasSmsPermission = result[Manifest.permission.READ_SMS] == true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ── Gmail IMAP section ──────────────────────────────────────────
            SectionHeader("Gmail IMAP")
            GmailSection(
                hasCredentials = state.hasCredentials,
                currentEmail   = state.gmailEmail,
                testResult     = state.connectionTest,
                onSave         = { email, pass -> vm.saveGmailCredentials(email, pass) },
                onTest         = { email, pass -> vm.testConnection(email, pass) },
                onDisconnect   = { vm.clearGmailCredentials() }
            )
            if (state.hasCredentials) {
                var syncQueued by remember { mutableStateOf(false) }
                // Reset syncQueued when sync finishes
                LaunchedEffect(syncStatus.state) {
                    if (syncStatus.state != com.financetracker.app.data.repository.SyncState.RUNNING) {
                        syncQueued = false
                    }
                }
                ListItem(
                    headlineContent = { Text(if (syncQueued) "Sync queued…" else "Fetch Gmail Now") },
                    supportingContent = { Text("Runs an immediate Gmail import in the background") },
                    leadingContent = {
                        Icon(Icons.Default.Sync, contentDescription = null,
                            tint = if (syncQueued) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingContent = {
                        Button(
                            onClick = { vm.fetchGmailNow(); syncQueued = true },
                            enabled = !syncQueued
                        ) { Text("Sync Now") }
                    }
                )
            }

            // ── SMS section ────────────────────────────────────────────────
            SectionHeader("SMS Sync")
            ListItem(
                headlineContent = { Text("Enable SMS Sync") },
                supportingContent = {
                    if (syncStatus.lastSyncMs > 0) {
                        Text("Last synced: ${dateFmt.format(Date(syncStatus.lastSyncMs))}")
                    } else {
                        Text("Reads bank SMS from your inbox automatically")
                    }
                },
                trailingContent = {
                    Switch(checked = state.smsEnabled, onCheckedChange = vm::setSmsEnabled)
                }
            )
            if (!hasSmsPermission) {
                val permanentlyDenied = activity != null &&
                    !activity.shouldShowRequestPermissionRationale(Manifest.permission.RECEIVE_SMS) &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) !=
                        PackageManager.PERMISSION_GRANTED

                ListItem(
                    headlineContent = { Text("SMS Permission Required") },
                    supportingContent = {
                        Text(
                            if (permanentlyDenied)
                                "Permission blocked — tap Open Settings to enable it manually"
                            else
                                "Allow to automatically capture new bank SMS messages"
                        )
                    },
                    leadingContent = {
                        Icon(Icons.Default.Sms, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error)
                    },
                    trailingContent = {
                        Button(onClick = {
                            if (permanentlyDenied) {
                                // Send user to App Settings since dialog won't show
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            } else {
                                smsPermissionLauncher.launch(
                                    arrayOf(Manifest.permission.RECEIVE_SMS)
                                )
                            }
                        }) { Text(if (permanentlyDenied) "Open Settings" else "Grant") }
                    }
                )
            }

            // ── Sync interval ──────────────────────────────────────────────
            SectionHeader("Sync Frequency")
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1, 6, 12, 24).forEach { hours ->
                    FilterChip(
                        selected = state.syncIntervalHours == hours,
                        onClick  = { vm.setSyncInterval(hours) },
                        label    = { Text("${hours}h") }
                    )
                }
            }

            // ── Sync status indicator ──────────────────────────────────────
            if (syncStatus.state == SyncState.RUNNING) {
                val found     = syncStatus.emailsFound
                val processed = syncStatus.emailsProcessed
                val newTx     = syncStatus.newTransactions
                val progress  = if (found > 0) processed.toFloat() / found else 0f

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (found == 0) "Connecting to Gmail…"
                                else "Reading emails… $processed / $found",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (newTx > 0) {
                            Text(
                                "+$newTx new",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (found > 0) {
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${(progress * 100).toInt()}% complete",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (syncStatus.state == SyncState.ERROR) {
                ListItem(
                    headlineContent = { Text("Sync Error", color = MaterialTheme.colorScheme.error) },
                    supportingContent = { Text(syncStatus.errorMessage) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── About ──────────────────────────────────────────────────────
            SectionHeader("About")
            ListItem(
                headlineContent = { Text("Finance Tracker") },
                supportingContent = { Text("Version 1.0 — Built with Kotlin + Jetpack Compose") }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.labelLarge,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun GmailSection(
    hasCredentials: Boolean,
    currentEmail: String,
    testResult: ConnectionTestResult,
    onSave: (String, String) -> Unit,
    onTest: (String, String) -> Unit,
    onDisconnect: () -> Unit
) {
    var email       by remember { mutableStateOf(currentEmail) }
    var password    by remember { mutableStateOf("") }
    var showPass    by remember { mutableStateOf(false) }
    var expanded    by remember { mutableStateOf(!hasCredentials) }

    ListItem(
        headlineContent = {
            Text(if (hasCredentials) "Connected: $currentEmail" else "Not connected")
        },
        supportingContent = {
            Text(if (hasCredentials) "Tap to manage" else "Add Gmail App Password to enable email sync")
        },
        leadingContent  = {
            Icon(
                Icons.Default.Email,
                contentDescription = null,
                tint = if (hasCredentials) Color(0xFF43A047) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
        }
    )

    if (expanded) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Gmail Address") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            OutlinedTextField(
                value = password,
                onValueChange = { if (it.length <= 16) password = it },
                label = { Text("App Password (16 chars)") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPass = !showPass }) {
                        Icon(
                            if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                }
            )

            // Connection test result
            when (testResult) {
                is ConnectionTestResult.Testing ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Testing connection…")
                    }
                is ConnectionTestResult.Success ->
                    Text("✓ Connection successful", color = Color(0xFF43A047))
                is ConnectionTestResult.Failure ->
                    Text("✗ ${testResult.message}", color = MaterialTheme.colorScheme.error)
                else -> {}
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick  = { onTest(email, password) },
                    modifier = Modifier.weight(1f)
                ) { Text("Test") }
                Button(
                    onClick  = { onSave(email, password); expanded = false },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
            }

            if (hasCredentials) {
                TextButton(
                    onClick    = { onDisconnect(); expanded = false },
                    colors     = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Disconnect") }
            }

            Text(
                "Generate an App Password at: Google Account → Security → 2-Step Verification → App Passwords",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
