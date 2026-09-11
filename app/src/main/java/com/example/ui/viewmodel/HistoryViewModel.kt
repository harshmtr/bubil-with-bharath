package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ScanEntity
import com.example.data.remote.SupabaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class HistoryFilter {
    ALL,
    COMPLIANT,
    VIOLATIONS,
    FOOD,
    MEDICINE
}

data class HistoryUiState(
    val searchQuery: String = "",
    val activeFilter: HistoryFilter = HistoryFilter.ALL,
    val isSyncing: Boolean = false,
    val syncStatusMessage: String? = null
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val scanDao = db.scanDao()
    private val supabaseRepo = SupabaseRepository(application, scanDao)

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    val scans: StateFlow<List<ScanEntity>> = combine(
        scanDao.getAllScans(),
        _uiState
    ) { allScans, state ->
        allScans.filter { scan ->
            // Search query filter
            val matchesQuery = if (state.searchQuery.isBlank()) {
                true
            } else {
                scan.product_name.contains(state.searchQuery, ignoreCase = true) ||
                scan.brand.contains(state.searchQuery, ignoreCase = true) ||
                scan.raw_ocr_text.contains(state.searchQuery, ignoreCase = true)
            }

            // Category/Compliance filter
            val matchesFilter = when (state.activeFilter) {
                HistoryFilter.ALL -> true
                HistoryFilter.COMPLIANT -> scan.trust_score >= 80
                HistoryFilter.VIOLATIONS -> scan.trust_score < 80
                HistoryFilter.FOOD -> scan.category.equals("FOOD", ignoreCase = true)
                HistoryFilter.MEDICINE -> scan.category.equals("MEDICINE", ignoreCase = true)
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setFilter(filter: HistoryFilter) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun deleteScan(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            scanDao.deleteScanById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            scanDao.clearAll()
        }
    }

    fun syncAllToSupabase() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncStatusMessage = null) }
            val currentList = scans.value
            var successCount = 0
            var failCount = 0

            withContext(Dispatchers.IO) {
                currentList.forEach { scan ->
                    val res = supabaseRepo.syncScanToRemote(scan)
                    if (res.isSuccess) successCount++ else failCount++
                }
            }

            _uiState.update {
                it.copy(
                    isSyncing = false,
                    syncStatusMessage = if (failCount == 0) {
                        "All $successCount scans synced with Supabase PostgreSQL!"
                    } else {
                        "$successCount synced, $failCount skipped (Configure Supabase keys in Profile)"
                    }
                )
            }
        }
    }

    fun testSupabaseConnection(onResult: (String, Boolean) -> Unit) {
        viewModelScope.launch {
            val result = supabaseRepo.testConnection()
            result.onSuccess { msg ->
                onResult(msg, true)
            }.onFailure { err ->
                onResult(err.localizedMessage ?: "Connection failed", false)
            }
        }
    }
}
