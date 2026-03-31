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
    val errorMessage: String = ""
)

@Singleton
class SyncStatusRepository @Inject constructor() {
    private val _status = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    fun setRunning() {
        _status.value = _status.value.copy(state = SyncState.RUNNING, errorMessage = "")
    }

    fun setIdle(syncTimeMs: Long = System.currentTimeMillis()) {
        _status.value = _status.value.copy(state = SyncState.IDLE, lastSyncMs = syncTimeMs)
    }

    fun setError(message: String) {
        _status.value = _status.value.copy(state = SyncState.ERROR, errorMessage = message)
    }
}
