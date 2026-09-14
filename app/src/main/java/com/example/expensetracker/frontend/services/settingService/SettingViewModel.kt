package com.example.expensetracker.frontend.services.settingService

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface SettingsUiState {
    object Loading : SettingsUiState
    data class Success(val settings: SettingsEntity?) : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}

class SettingViewModel(
    private val settingRepository: SettingRepository
) : ViewModel() {

    // Dedicated state flow for transient error messages

    @RequiresApi(Build.VERSION_CODES.O)
    val today = LocalDate.now()
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    val uiState: StateFlow<SettingsUiState> = settingRepository
        .getSettings(today.year, today.monthValue)
        .map<SettingsEntity?, SettingsUiState> { settings ->
            SettingsUiState.Success(settings)
        }
        .catch { e ->
            val message = e.message ?: "Failed to load settings"
            _errorMessage.value = message
            emit(SettingsUiState.Error(message))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsUiState.Loading
        )

    init {
        // API-gate the call site since `today` (and the query it drives) require O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            seedDefaultSettingsIfNeeded()
        }
    }

    /**
     * If no settings row exists yet for the current year/month (first time this
     * user/device has hit Settings), create one with sample defaults so the screen
     * never has to render blank/disabled fields.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun seedDefaultSettingsIfNeeded() {
        viewModelScope.launch {
            try {
                val existing = settingRepository.getSettings(today.year, today.monthValue).first()
                if (existing == null) {
                    val sample = SettingsEntity(
                        year                 = today.year,
                        month                = today.monthValue,
                        monthlyBudget        = 1200.0,
                        weeklyBudget         = 300.0,
                        dailyLimit           = 50.0,
                        notificationEnabled  = true,
                        isDarkMode           = false
                    )
                    settingRepository.insertSettings(sample)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to initialize default settings"
            }
        }
    }

    fun initSettings(settingsEntity: SettingsEntity) {
        viewModelScope.launch {
            try {
                settingRepository.insertSettings(settingsEntity)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to initialize settings"
            }
        }
    }

    fun updateSettings(settingsEntity: SettingsEntity) {
        viewModelScope.launch {
            try {
                settingRepository.updateSettings(settingsEntity)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to update settings"
            }
        }
    }

    /**
     * Clears the current error message after it has been displayed to the user.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

class SettingViewModelFactory(
    private val repo: SettingRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingViewModel::class.java)) return SettingViewModel(repo) as T
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
