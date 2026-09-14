package com.example.expensetracker.frontend.services.analyticsService

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.local.entity.analytics.AnalyticsSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// ── UI State ─────────────────────────────────────────────────────────────────

sealed class AnalyticsUiState {
    object Idle : AnalyticsUiState()
    object Loading : AnalyticsUiState()
    data class Loaded(val data: AnalyticsSummary) : AnalyticsUiState()
    data class Error(val message: String) : AnalyticsUiState()
}

// ── ViewModel ────────────────────────────────────────────────────────────────

class AnalyticsViewModel(private val repo: AnalyticsRepository) : ViewModel() {

    private val _state = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Idle)
    val state: StateFlow<AnalyticsUiState> = _state.asStateFlow()

    private val _selectedMonth = MutableStateFlow<Int?>(null)
    val selectedMonth: StateFlow<Int?> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow<Int?>(null)
    val selectedYear: StateFlow<Int?> = _selectedYear.asStateFlow()

    private var observeJob: Job? = null

    // Starts (or restarts) a live subscription to the Room-backed summary.
    // Because this is a Flow all the way down to the `expenses` table, the
    // screen updates automatically whenever an expense is added/edited/deleted
    // — no manual refresh needed.
    fun loadSummary(
        month: Int? = null,
        year: Int? = null,
        trendMonths: Int = 6,
        monthlyBudget: Double = 0.0,
    ) {
        _selectedMonth.value = month
        _selectedYear.value = year

        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _state.value = AnalyticsUiState.Loading
            repo.getSummary(month, year, trendMonths, monthlyBudget)
                .catch { e -> _state.value = AnalyticsUiState.Error(e.message ?: "Something went wrong") }
                .collectLatest { summary -> _state.value = AnalyticsUiState.Loaded(summary) }
        }
    }

    fun changeMonth(month: Int, year: Int) {
        loadSummary(month = month, year = year)
    }

    fun resetState() {
        observeJob?.cancel()
        _state.value = AnalyticsUiState.Idle
    }
}

// ── Factory ──────────────────────────────────────────────────────────────────

class AnalyticsViewModelFactory(private val repo: AnalyticsRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            return AnalyticsViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}