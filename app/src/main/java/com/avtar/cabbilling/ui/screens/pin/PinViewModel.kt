package com.avtar.cabbilling.ui.screens.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avtar.cabbilling.data.repository.ConfigRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PinUiState(
    val entered: String = "",
    val checking: Boolean = false,
    val error: Boolean = false,
    val failedAttempts: Int = 0,
    val cooldownRemaining: Int = 0
) {
    val locked: Boolean get() = cooldownRemaining > 0
}

class PinViewModel(private val configRepository: ConfigRepository) : ViewModel() {

    private val _state = MutableStateFlow(PinUiState())
    val state: StateFlow<PinUiState> = _state.asStateFlow()

    private var cooldownJob: Job? = null

    fun append(digit: Char) {
        val s = _state.value
        if (s.checking || s.locked || s.entered.length >= MAX_LEN) return
        _state.update { it.copy(entered = it.entered + digit, error = false) }
    }

    fun backspace() = _state.update {
        if (it.entered.isEmpty()) it else it.copy(entered = it.entered.dropLast(1), error = false)
    }

    fun verify(onUnlocked: () -> Unit) {
        val s = _state.value
        if (s.checking || s.locked) return
        if (s.entered.length < MIN_LEN) {
            _state.update { it.copy(error = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(checking = true) }
            val ok = configRepository.verifyPin(s.entered)
            if (ok) {
                onUnlocked()
            } else {
                val attempts = s.failedAttempts + 1
                _state.update {
                    it.copy(checking = false, entered = "", error = true, failedAttempts = attempts)
                }
                if (attempts >= MAX_ATTEMPTS) startCooldown()
            }
        }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            var remaining = COOLDOWN_SECONDS
            while (remaining > 0) {
                _state.update { it.copy(cooldownRemaining = remaining) }
                delay(1_000)
                remaining--
            }
            _state.update { it.copy(cooldownRemaining = 0, failedAttempts = 0, error = false) }
        }
    }

    companion object {
        const val MIN_LEN = 4
        const val MAX_LEN = 6
        const val MAX_ATTEMPTS = 5
        const val COOLDOWN_SECONDS = 30
    }
}
