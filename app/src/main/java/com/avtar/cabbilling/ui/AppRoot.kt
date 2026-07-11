package com.avtar.cabbilling.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avtar.cabbilling.ui.navigation.MainScaffold
import com.avtar.cabbilling.ui.screens.pin.PinScreen
import com.avtar.cabbilling.ui.screens.setup.SetupScreen
import com.avtar.cabbilling.ui.theme.CabBillingTheme

private enum class Gate { Loading, Setup, Pin, Main }

/**
 * Single entry composable. Chooses the theme from persisted config and gates the
 * UI through: loading → first-time setup → PIN unlock → the main tabbed app,
 * cross-fading between those states.
 */
@Composable
fun AppRoot() {
    val vm = containerViewModel { MainViewModel(it.configRepository, it.sessionState) }
    val state by vm.uiState.collectAsStateWithLifecycle()

    val gate = when {
        state.loading -> Gate.Loading
        !state.setupComplete -> Gate.Setup
        !state.unlocked -> Gate.Pin
        else -> Gate.Main
    }

    CabBillingTheme(appTheme = state.theme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Crossfade(targetState = gate, animationSpec = tween(320), label = "app-gate") { current ->
                when (current) {
                    Gate.Loading -> LoadingGate()
                    Gate.Setup -> SetupScreen(onSetupComplete = vm::unlock)
                    Gate.Pin -> PinScreen(onUnlocked = vm::unlock)
                    Gate.Main -> MainScaffold()
                }
            }
        }
    }
}

@Composable
private fun LoadingGate() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
