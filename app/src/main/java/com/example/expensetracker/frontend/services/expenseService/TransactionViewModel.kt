package com.example.expensetracker.frontend.services.expenseService

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.expensetracker.services.entity.ExpenseEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine

data class ExpenseUiState(
    val isLoading: Boolean = false,
    val expenses: List<ExpenseEntity> = emptyList(),
    val todayExpenses: List<ExpenseEntity> = emptyList(),
    val selectedExpense: ExpenseEntity? = null,
    val errorMessage: String? = null
)

sealed interface ExpenseDetailUiState {
    object Idle : ExpenseDetailUiState
    object Loading : ExpenseDetailUiState
    data class Success(val expense: ExpenseEntity) : ExpenseDetailUiState
    data class Error(val message: String) : ExpenseDetailUiState
}

class ExpenseViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _selectedExpense = MutableStateFlow<ExpenseEntity?>(null)
    private val _todayExpenses = MutableStateFlow<List<ExpenseEntity>>(emptyList())

    // Combine individual state feeds into a unified UI State
    val uiState: StateFlow<ExpenseUiState> = combine(
        repository.getAll().catch { emit(emptyList()) },
        _todayExpenses,
        _selectedExpense,
        _isLoading,
        _errorMessage
    ) { expenses, todayExpenses, selectedExpense, isLoading, errorMessage ->
        ExpenseUiState(
            isLoading = isLoading,
            expenses = expenses,
            todayExpenses = todayExpenses,
            selectedExpense = selectedExpense,
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExpenseUiState(isLoading = true)
    )

    // Paging Data flow kept separate for Jetpack Paging compatibility
    private val _searchResults = MutableStateFlow<PagingData<ExpenseEntity>>(PagingData.empty())
    val searchResults: StateFlow<PagingData<ExpenseEntity>> = _searchResults

    // Plain (non-paged) search results for bounded UI like the Search dialog.
    private val _searchResultsSimple = MutableStateFlow<List<ExpenseEntity>>(emptyList())
    val searchResultsSimple: StateFlow<List<ExpenseEntity>> = _searchResultsSimple

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    fun unifiedSearchSimple(q: String, category: String) {
        viewModelScope.launch {
            _isSearching.value = true
            repository.unifiedSearchSimple(q, category)
                .catch { e ->
                    _errorMessage.value = e.message
                    _isSearching.value = false
                }
                .collectLatest { list ->
                    _searchResultsSimple.value = list
                    _isSearching.value = false
                }
        }
    }

    fun loadExpenseById(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getExpenseById(id)
                .catch { e ->
                    _errorMessage.value = e.message
                    _isLoading.value = false
                }
                .collect { expense ->
                    _selectedExpense.value = expense
                    _isLoading.value = false
                }
        }
    }

    fun addExpense(entity: ExpenseEntity, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newId = repository.add(entity)
                onComplete(newId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateExpense(entity: ExpenseEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.update(entity)
                onComplete()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteExpense(id: Long?, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.delete(id)
                if (_selectedExpense.value?.id == id) {
                    _selectedExpense.value = null
                }
                onComplete()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSelectedExpense() {
        _selectedExpense.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

class ExpenseViewModelFactory(
    private val repo: ExpenseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) return ExpenseViewModel(repo) as T
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}