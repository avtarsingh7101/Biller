package com.avtar.cabbilling.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avtar.cabbilling.data.model.AppTheme
import com.avtar.cabbilling.data.model.DEFAULT_CARS
import com.avtar.cabbilling.ui.components.ConfirmDialog
import com.avtar.cabbilling.ui.components.FieldLabel
import com.avtar.cabbilling.ui.components.ScreenHeader
import com.avtar.cabbilling.ui.components.SectionCard
import com.avtar.cabbilling.ui.containerViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onMessage: (String) -> Unit, onOpenFaq: () -> Unit = {}) {
    val vm = containerViewModel { SettingsViewModel(it.billRepository, it.configRepository) }
    val state by vm.state.collectAsStateWithLifecycle()

    var showChangePin by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }

    val carSuggestions by remember(state.carDraft) {
        derivedStateOf {
            val q = state.carDraft.trim()
            if (q.length < 2) emptyList()
            else DEFAULT_CARS.filter { it.contains(q, ignoreCase = true) }
                .filterNot { name -> state.cars.any { it.name.equals(name, ignoreCase = true) } }
                .take(5)
        }
    }

    androidx.compose.runtime.LaunchedEffect(vm) { vm.messages.collect { onMessage(it) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ScreenHeader(title = "Settings")

        SectionHeader(Icons.Filled.Person, "Company Profile")
        SectionCard {
            FieldLabel("Company / Operator name")
            OutlinedTextField(
                value = state.companyName,
                onValueChange = vm::onCompanyChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            FieldLabel("Mobile number")
            OutlinedTextField(
                value = state.companyPhone,
                onValueChange = vm::onPhoneChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                placeholder = { Text("e.g. +91 98765 43210") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            FieldLabel("Currency symbol")
            OutlinedTextField(
                value = state.currencySymbol,
                onValueChange = vm::onCurrencyChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = vm::saveProfile,
                enabled = state.canSaveProfile,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("Save Profile") }
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader(Icons.Filled.DirectionsCar, "Manage Fleet")
        SectionCard {
            FieldLabel("Car name")
            OutlinedTextField(
                value = state.carDraft,
                onValueChange = vm::onCarDraftChange,
                singleLine = true,
                placeholder = { Text("e.g. Maruti Suzuki Ertiga") },
                modifier = Modifier.fillMaxWidth()
            )
            if (carSuggestions.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    carSuggestions.forEach { suggestion ->
                        TextButton(onClick = { vm.onCarDraftChange(suggestion) }) {
                            Text(suggestion, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            FieldLabel("Number plate")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.plateDraft,
                    onValueChange = vm::onPlateDraftChange,
                    singleLine = true,
                    placeholder = { Text("e.g. DL 01 AB 1234") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = vm::addCar) {
                    Icon(Icons.Filled.Add, contentDescription = "Add car")
                }
            }
            if (state.cars.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.cars.forEach { car ->
                        InputChip(
                            selected = false,
                            onClick = { vm.removeCar(car) },
                            label = { Text(car.display) },
                            trailingIcon = {
                                Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader(Icons.Filled.Palette, "Appearance")
        SectionCard {
            AppTheme.entries.forEach { theme ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = state.theme == theme,
                            onClick = { vm.selectTheme(theme) }
                        )
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = state.theme == theme, onClick = { vm.selectTheme(theme) })
                    Spacer(Modifier.size(8.dp))
                    Text(theme.label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader(Icons.Outlined.Shield, "Security")
        SectionCard {
            OutlinedButton(
                onClick = { showChangePin = true },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Change PIN")
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader(Icons.Filled.HelpOutline, "Help")
        SectionCard {
            OutlinedButton(
                onClick = onOpenFaq,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Filled.HelpOutline, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Frequently Asked Questions")
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader(Icons.Outlined.DeleteForever, "Danger Zone")
        SectionCard {
            Text(
                "Erase all invoices, trips, and setup — returning the app to first-launch state.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { showReset = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Outlined.DeleteForever, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Reset app & erase data")
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showChangePin) {
        ChangePinDialog(
            onConfirm = { newPin -> vm.changePin(newPin) { showChangePin = false } },
            onDismiss = { showChangePin = false }
        )
    }

    if (showReset) {
        ConfirmDialog(
            title = "Reset everything?",
            message = "You'll permanently lose every invoice you've billed, plus your company profile, cars and PIN. " +
                "There's no backup — this can't be recovered.",
            confirmLabel = "Erase all data",
            onConfirm = { showReset = false; vm.resetApp() },
            onDismiss = { showReset = false }
        )
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ChangePinDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val valid = pin.length in 4..6 && pin == confirm

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change PIN") },
        text = {
            Column {
                FieldLabel("New PIN (4-6 digits)")
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                FieldLabel("Confirm PIN")
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it.filter(Char::isDigit).take(6) },
                    singleLine = true,
                    isError = confirm.isNotEmpty() && confirm != pin,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm(pin) }) { Text("Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
