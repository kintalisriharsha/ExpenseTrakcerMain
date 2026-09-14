package com.example.expensetracker.frontend.services.TodoService

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.debounce
import kotlin.collections.emptyList

sealed class TodoHistoryUiState {
    object Idle : TodoHistoryUiState()
    object Loading : TodoHistoryUiState()

    data class Loaded(val items: List<TodoEntity>, val requestedOffset: Int) : TodoHistoryUiState()
    data class Error(val message: String) : TodoHistoryUiState()
}

class TodoViewModel(
    private val repo: TodoRepository
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    val todos: StateFlow<List<TodoEntity>> = repo.getAllTodos()
        .catch { e ->
            _errorMessage.value = e.message
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun insertTodo(todoEntity: TodoEntity, onComplete: (TodoEntity) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val inserted = repo.addTodo(todoEntity)
                onComplete(inserted)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun updateTodo(todoEntity: TodoEntity) {
        viewModelScope.launch {
            try {
                repo.updateTodo(todoEntity)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun deleteTodo(todoEntity: TodoEntity) {
        viewModelScope.launch {
            try {
                repo.deleteTodo(todoEntity)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<TodoEntity>> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) repo.getAllTodos() else repo.searchTodos(query)
        }
        .catch { e ->
            _errorMessage.value = e.message
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private val _historyState = MutableStateFlow<TodoHistoryUiState>(TodoHistoryUiState.Idle)
    val historyState: StateFlow<TodoHistoryUiState> = _historyState

    fun loadHistoryPage(limit: Int, offset: Int) {
        viewModelScope.launch {
            _historyState.value = TodoHistoryUiState.Loading
            try {
                val page = repo.getHistory(limit = limit, offset = offset)
                _historyState.value = TodoHistoryUiState.Loaded(items = page, requestedOffset = offset)
            } catch (e: Exception) {
                _historyState.value = TodoHistoryUiState.Error(e.message ?: "Failed to load history")
            }
        }
    }

    // Convenience toggle for the checkbox — flips checkBox and persists it.
    fun toggleDone(todoEntity: TodoEntity) {
        updateTodo(todoEntity.copy(checkBox = !todoEntity.checkBox))
    }


    fun clearError() {
        _errorMessage.value = null
    }
}

class TodoViewModelFactory(
    private val repo: TodoRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoViewModel::class.java)) return TodoViewModel(repo) as T
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}