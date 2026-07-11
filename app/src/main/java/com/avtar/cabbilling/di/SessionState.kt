package com.avtar.cabbilling.di

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory unlock flag for the current process. Because it is not persisted,
 * killing the app (process death) always re-locks it — every fresh launch must
 * pass the PIN screen again.
 */
class SessionState {
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    fun unlock() { _unlocked.value = true }
    fun lock() { _unlocked.value = false }
}
