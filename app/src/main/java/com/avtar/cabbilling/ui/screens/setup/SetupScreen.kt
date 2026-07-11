package com.avtar.cabbilling.ui.screens.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avtar.cabbilling.data.model.DEFAULT_CARS
import com.avtar.cabbilling.ui.components.FieldLabel
import com.avtar.cabbilling.ui.components.SectionCard
import com.avtar.cabbilling.ui.containerViewModel

private const val LAST_STEP = 2

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(onSetupComplete: () -> Unit) {
    val vm = containerViewModel { SetupViewModel(it.configRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableIntStateOf(0) }

    val canAdvance = when (step) {
        0 -> state.companyName.isNotBlank()
        1 -> state.carName.isNotBlank()
        else -> state.canFinish
    }

    val carSuggestions by remember(state.carName) {
        derivedStateOf {
            val q = state.carName.trim()
            if (q.length < 2) emptyList()
            else DEFAULT_CARS.filter { it.contains(q, ignoreCase = true) }.take(5)
        }
    }

    Scaffold(
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (step > 0) {
                        OutlinedButton(
                            onClick = { step-- },
                            modifier = Modifier.height(52.dp)
                        ) { Text("Back") }
                    }
                    Button(
                        onClick = {
                            if (step < LAST_STEP) step++ else vm.submit(onSetupComplete)
                        },
                        enabled = canAdvance,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        if (state.saving) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(if (step < LAST_STEP) "Continue" else "Finish — issue invoice #0001")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            SetupHeader(step)
            Spacer(Modifier.height(20.dp))

            when (step) {
                0 -> {
                    SectionCard {
                        FieldLabel("Company / Operator name")
                        OutlinedTextField(
                            value = state.companyName,
                            onValueChange = vm::onCompanyChange,
                            singleLine = true,
                            placeholder = { Text("e.g. Sharma Travels") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        FieldLabel("Mobile number (optional)")
                        OutlinedTextField(
                            value = state.companyPhone,
                            onValueChange = vm::onPhoneChange,
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                            placeholder = { Text("e.g. +91 98765 43210") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                1 -> {
                    SectionCard {
                        FieldLabel("Car name")
                        OutlinedTextField(
                            value = state.carName,
                            onValueChange = vm::onCarNameChange,
                            singleLine = true,
                            placeholder = { Text("e.g. Maruti Suzuki Ertiga") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (carSuggestions.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                carSuggestions.forEach { suggestion ->
                                    TextButton(onClick = { vm.onCarNameChange(suggestion) }) {
                                        Text(suggestion, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        FieldLabel("Number plate")
                        OutlinedTextField(
                            value = state.carPlate,
                            onValueChange = vm::onCarPlateChange,
                            singleLine = true,
                            placeholder = { Text("e.g. DL 01 AB 1234") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "You can add more cars later in Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    SectionCard {
                        FieldLabel("Currency symbol")
                        OutlinedTextField(
                            value = state.currencySymbol,
                            onValueChange = vm::onCurrencyChange,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    SectionCard {
                        FieldLabel("Security PIN (4-6 digits)")
                        OutlinedTextField(
                            value = state.pin,
                            onValueChange = vm::onPinChange,
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            placeholder = { Text("Enter PIN") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        FieldLabel("Confirm PIN")
                        OutlinedTextField(
                            value = state.confirmPin,
                            onValueChange = vm::onConfirmPinChange,
                            singleLine = true,
                            isError = state.confirmPin.isNotEmpty() && !state.pinsMatch,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            placeholder = { Text("Re-enter PIN") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (state.confirmPin.isNotEmpty() && !state.pinsMatch) {
                            Spacer(Modifier.height(6.dp))
                            Text("PINs do not match.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            state.error?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SetupHeader(step: Int) {
    val (icon, title, subtitle) = when (step) {
        0 -> Triple(Icons.Filled.Check, "Your company", "This name prints on every invoice you generate.")
        1 -> Triple(Icons.Filled.DirectionsCar, "Your car", "Add your primary car — you can add more later in Settings.")
        else -> Triple(Icons.Filled.Lock, "Last step — lock it down", "A PIN protects your app. You'll enter it on every launch.")
    }
    Column {
        Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Step ${step + 1} of 3", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { 0.25f + 0.75f * (step + 1) / 3f },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
