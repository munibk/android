package com.financetracker.app.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financetracker.app.data.repository.SyncStatusRepository
import com.financetracker.app.service.gmail.ImapCredentialsManager
import com.financetracker.app.service.gmail.ImapGmailFetcher
import com.financetracker.app.service.gmail.ImapResult
import com.financetracker.app.workers.GmailFetchWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ConnectionTestResult {
    object Idle    : ConnectionTestResult()
    object Testing : ConnectionTestResult()
    object Success : ConnectionTestResult()
    data class Failure(val message: String) : ConnectionTestResult()
}

data class SettingsUiState(
    val gmailEmail: String = "",
    val hasCredentials: Boolean = false,
    val connectionTest: ConnectionTestResult = ConnectionTestResult.Idle,
    val smsEnabled: Boolean = true,
    val syncIntervalHours: Int = 6
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val credentialsManager: ImapCredentialsManager,
    private val gmailFetcher: ImapGmailFetcher,
    private val syncStatusRepo: SyncStatusRepository
) : ViewModel() {

    private val _connectionTest = MutableStateFlow<ConnectionTestResult>(ConnectionTestResult.Idle)
    private val _smsEnabled     = MutableStateFlow(true)
    private val _syncInterval   = MutableStateFlow(6)

    val uiState: StateFlow<SettingsUiState> = combine(
        _connectionTest,
        _smsEnabled,
        _syncInterval
    ) { test, sms, interval ->
        SettingsUiState(
            gmailEmail        = credentialsManager.getEmail() ?: "",
            hasCredentials    = credentialsManager.hasCredentials(),
            connectionTest    = test,
            smsEnabled        = sms,
            syncIntervalHours = interval
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(
        gmailEmail     = credentialsManager.getEmail() ?: "",
        hasCredentials = credentialsManager.hasCredentials()
    ))

    val syncStatus = syncStatusRepo.status

    fun saveGmailCredentials(email: String, appPassword: String) {
        credentialsManager.saveCredentials(email, appPassword)
    }

    fun clearGmailCredentials() {
        credentialsManager.clearCredentials()
        _connectionTest.value = ConnectionTestResult.Idle
    }

    fun testConnection(email: String, appPassword: String) {
        viewModelScope.launch {
            _connectionTest.value = ConnectionTestResult.Testing
            // Temporarily save to test
            credentialsManager.saveCredentials(email, appPassword)
            val result = gmailFetcher.fetchEmails(lastSyncMs = 0L, maxRetries = 1)
            _connectionTest.value = when (result) {
                is ImapResult.Success    -> ConnectionTestResult.Success
                is ImapResult.AuthError  -> ConnectionTestResult.Failure(result.message)
                is ImapResult.NetworkError -> ConnectionTestResult.Failure(result.message)
            }
        }
    }

    fun setSmsEnabled(enabled: Boolean) { _smsEnabled.value = enabled }

    fun setSyncInterval(hours: Int) { _syncInterval.value = hours }

    fun fetchGmailNow() {
        GmailFetchWorker.enqueueOneTime(context)
    }
}
