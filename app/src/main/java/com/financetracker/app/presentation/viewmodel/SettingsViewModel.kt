package com.financetracker.app.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financetracker.app.data.repository.SyncStatusRepository
import com.financetracker.app.service.gmail.AuthMethod
import com.financetracker.app.service.gmail.GmailOAuthManager
import com.financetracker.app.service.gmail.GmailRestFetcher
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
    val authMethod: AuthMethod = AuthMethod.IMAP,
    val connectionTest: ConnectionTestResult = ConnectionTestResult.Idle,
    val smsEnabled: Boolean = true,
    val syncIntervalHours: Int = 6,
    val syncFromYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - 1
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val credentialsManager: ImapCredentialsManager,
    private val oauthManager: GmailOAuthManager,
    private val gmailFetcher: ImapGmailFetcher,
    private val gmailRestFetcher: GmailRestFetcher,
    private val syncStatusRepo: SyncStatusRepository
) : ViewModel() {

    private val _connectionTest = MutableStateFlow<ConnectionTestResult>(ConnectionTestResult.Idle)
    private val _smsEnabled     = MutableStateFlow(true)
    private val _syncInterval   = MutableStateFlow(6)
    private val _syncFromYear   = MutableStateFlow(credentialsManager.getSyncFromYear())
    private val _authMethod     = MutableStateFlow(credentialsManager.getAuthMethod())

    val uiState: StateFlow<SettingsUiState> = combine(
        _connectionTest,
        _smsEnabled,
        _syncInterval,
        _syncFromYear,
        _authMethod
    ) { test, sms, interval, year, method ->
        SettingsUiState(
            gmailEmail        = if (method == AuthMethod.OAUTH) oauthManager.getEmail() ?: ""
                                else credentialsManager.getEmail() ?: "",
            hasCredentials    = if (method == AuthMethod.OAUTH) oauthManager.hasCredentials()
                                else credentialsManager.hasCredentials(),
            authMethod        = method,
            connectionTest    = test,
            smsEnabled        = sms,
            syncIntervalHours = interval,
            syncFromYear      = year
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        run {
            val method = credentialsManager.getAuthMethod()
            SettingsUiState(
                gmailEmail     = if (method == AuthMethod.OAUTH) oauthManager.getEmail() ?: ""
                                 else credentialsManager.getEmail() ?: "",
                hasCredentials = if (method == AuthMethod.OAUTH) oauthManager.hasCredentials()
                                 else credentialsManager.hasCredentials(),
                authMethod     = method,
                syncFromYear   = credentialsManager.getSyncFromYear()
            )
        }
    )

    val syncStatus = syncStatusRepo.status

    // ── IMAP ──────────────────────────────────────────────────────────────

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
            credentialsManager.saveCredentials(email, appPassword)
            val result = gmailFetcher.fetchEmails(lastSyncMs = 0L, maxRetries = 1)
            _connectionTest.value = when (result) {
                is ImapResult.Success      -> ConnectionTestResult.Success
                is ImapResult.AuthError    -> ConnectionTestResult.Failure(result.message)
                is ImapResult.NetworkError -> ConnectionTestResult.Failure(result.message)
            }
        }
    }

    // ── OAuth ─────────────────────────────────────────────────────────────

    fun onOAuthSignIn(email: String) {
        oauthManager.saveEmail(email)
        credentialsManager.saveAuthMethod(AuthMethod.OAUTH)
        _authMethod.value = AuthMethod.OAUTH
        _connectionTest.value = ConnectionTestResult.Idle
    }

    fun clearOAuthCredentials() {
        oauthManager.clearCredentials()
        credentialsManager.saveAuthMethod(AuthMethod.IMAP)
        _authMethod.value = AuthMethod.IMAP
        _connectionTest.value = ConnectionTestResult.Idle
    }

    fun testOAuthConnection() {
        viewModelScope.launch {
            _connectionTest.value = ConnectionTestResult.Testing
            val result = gmailRestFetcher.testConnection()
            _connectionTest.value = when (result) {
                is ImapResult.Success      -> ConnectionTestResult.Success
                is ImapResult.AuthError    -> ConnectionTestResult.Failure(result.message)
                is ImapResult.NetworkError -> ConnectionTestResult.Failure(result.message)
            }
        }
    }

    fun switchAuthMethod(method: AuthMethod) {
        credentialsManager.saveAuthMethod(method)
        _authMethod.value = method
        _connectionTest.value = ConnectionTestResult.Idle
    }

    // ── Shared ────────────────────────────────────────────────────────────

    fun setSmsEnabled(enabled: Boolean) { _smsEnabled.value = enabled }

    fun setSyncInterval(hours: Int) { _syncInterval.value = hours }

    fun setSyncFromYear(year: Int) {
        credentialsManager.saveSyncFromYear(year)
        _syncFromYear.value = year
    }

    fun fetchGmailNow() {
        GmailFetchWorker.enqueueOneTime(context)
    }
}
