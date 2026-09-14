package com.example.expensetracker.frontend.services.homeService

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    object Idle : HomeUiState()
    data class Loaded(val dashboard: HomeDashboard) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(
    private val repo: HomeRepository
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var started = false

    /**
     * Starts observing Settings/Expenses/Todo. Home is read-only, so this
     * only ever reads — call it once (e.g. from LaunchedEffect(Unit)); it's
     * a no-op on later calls since the same Flow keeps emitting live updates.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun dashboardData() {
        if (started) return
        started = true
        viewModelScope.launch {
            repo.observeDashboard()
                .catch { e -> _state.value = HomeUiState.Error(e.message ?: "Something went wrong") }
                .collect { dashboard -> _state.value = HomeUiState.Loaded(dashboard) }
        }
    }
}

class HomeViewModelFactory(
    private val repo: HomeRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java))
            return HomeViewModel(repo) as T
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}