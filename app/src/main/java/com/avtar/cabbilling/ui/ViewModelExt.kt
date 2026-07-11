package com.avtar.cabbilling.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.avtar.cabbilling.CabBillingApplication
import com.avtar.cabbilling.di.AppContainer

/**
 * Builds (or reuses) a [ViewModel], injecting the app's [AppContainer]. Keeps
 * every screen's VM wiring to a single readable line without a DI framework.
 */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(
    crossinline create: (AppContainer) -> VM
): VM {
    val container = (LocalContext.current.applicationContext as CabBillingApplication).container
    return viewModel<VM>(factory = viewModelFactory { initializer { create(container) } })
}
