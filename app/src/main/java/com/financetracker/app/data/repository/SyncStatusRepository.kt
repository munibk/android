package com.financetracker.app.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncState { IDLE, RUNNING, ERROR }

data class SyncStatus(
    val state: SyncState = SyncState.IDLE,
    val lastSyncMs: Long = 0L,
    val errorMessage: String = "",
    val emailsFound: Int = 0,
    val emailsProcessed: Int = 0,
    val newTransactions: Int = 0
)

@Singleton
class SyncStatusRepository @Inject constructor() {
    private val _status = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    fun setRunning() {
        _status.value = _status.value.copy(
            state = SyncState.RUNNING,
            errorMessage = "",
            emailsFound = 0,
            emailsProcessed = 0,
            newTransactions = 0
        )
    }

    fun setProgress(emailsFound: Int, emailsProcessed: Int, newTransactions: Int) {
        _status.value = _status.value.copy(
            emailsFound = emailsFound,
            emailsProcessed = emailsProcessed,
            newTransactions = newTransactions
        )
    }

    fun setIdle(syncTimeMs: Long = System.currentTimeMillis()) {
        _status.value = _status.value.copy(state = SyncState.IDLE, lastSyncMs = syncTimeMs)
    }

    fun setError(message: String) {
        _status.value = _status.value.copy(state = SyncState.ERROR, errorMessage = message)
    }
}
