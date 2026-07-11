package com.avtar.cabbilling.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avtar.cabbilling.data.model.AppTheme
import com.avtar.cabbilling.data.repository.ConfigRepository
import com.avtar.cabbilling.di.SessionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Top-level app state that decides which gate (setup / PIN / main) is shown. */
data class AppUiState(
    val loading: Boolean = true,
    val setupComplete: Boolean = false,
    val unlocked: Boolean = false,
    val theme: AppTheme = AppTheme.AUTO
)

class MainViewModel(
    configRepository: ConfigRepository,
    private val sessionState: SessionState
) : ViewModel() {

    val uiState: StateFlow<AppUiState> =
        combine(configRepository.config, sessionState.unlocked) { config, unlocked ->
            AppUiState(
                loading = false,
                setupComplete = config?.isSetupComplete == true,
                unlocked = unlocked,
                theme = AppTheme.fromId(config?.theme)
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppUiState()
        )

    fun unlock() = sessionState.unlock()
}
