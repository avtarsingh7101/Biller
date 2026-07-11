package com.avtar.cabbilling.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avtar.cabbilling.data.model.Car
import com.avtar.cabbilling.ui.components.FieldLabel
import com.avtar.cabbilling.ui.components.GradientButton
import com.avtar.cabbilling.ui.components.LocationPickerDialog
import com.avtar.cabbilling.ui.components.DatePickerModal
import com.avtar.cabbilling.ui.components.ScreenHeader
import com.avtar.cabbilling.ui.components.SectionCard
import com.avtar.cabbilling.ui.components.SelectorField
import com.avtar.cabbilling.ui.containerViewModel
import com.avtar.cabbilling.ui.theme.AppGradients
import com.avtar.cabbilling.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenBill: (Long) -> Unit,
    onMessage: (String) -> Unit
) {
    val vm = containerViewModel { DashboardViewModel(it.billRepository, it.configRepository) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.error) {
        state.error?.let { onMessage(it); vm.clearError() }
    }

    var showFrom by remember { mutableStateOf(false) }
    var showTo by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var carMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp)
    ) {
        DashboardHero(
            company = state.companyName.ifBlank { "Cab Billing" },
            nextInvoice = state.nextInvoiceCode
        )

        if (state.cars.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            InfoBanner("Add at least one car in Settings before you can bill a trip.")
        }

        val savedBill = state.justSaved
        var lastSaved by remember { mutableStateOf(savedBill) }
        if (savedBill != null) lastSaved = savedBill
        AnimatedVisibility(
            visible = savedBill != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
                lastSaved?.let { bill ->
                    SavedBanner(
                        code = bill.invoiceCode,
                        amount = Formatters.amount(bill.amount, state.currencySymbol),
                        onView = { onOpenBill(bill.id) },
                        onDismiss = vm::dismissSaved
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionCard(title = "Trip") {
            FieldLabel("From")
            SelectorField(
                value = state.fromLocation,
                placeholder = "Pickup location",
                onClick = { showFrom = true },
                leadingIcon = Icons.Filled.MyLocation
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = vm::swapRoute) {
                    Icon(Icons.Filled.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Swap")
                }
            }
            FieldLabel("To")
            SelectorField(
                value = state.toLocation,
                placeholder = "Drop location",
                onClick = { showTo = true },
                leadingIcon = Icons.Filled.Place
            )
            Spacer(Modifier.height(12.dp))
            FieldLabel("Trip date")
            SelectorField(
                value = Formatters.dateFull.format(state.date),
                placeholder = "Select date",
                onClick = { showDate = true },
                leadingIcon = Icons.Filled.CalendarMonth
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionCard(title = "Vehicle & Passenger") {
            FieldLabel("Car")
            ExposedDropdownMenuBox(
                expanded = carMenu,
                onExpandedChange = { if (state.cars.isNotEmpty()) carMenu = !carMenu }
            ) {
                OutlinedTextField(
                    value = state.selectedCar?.display ?: "",
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    placeholder = { Text("Select car") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = carMenu) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = carMenu,
                    onDismissRequest = { carMenu = false }
                ) {
                    state.cars.forEach { car ->
                        DropdownMenuItem(
                            text = { Text(car.display) },
                            onClick = { vm.selectCar(car); carMenu = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            FieldLabel("Passenger (optional)")
            OutlinedTextField(
                value = state.passengerName,
                onValueChange = vm::setPassenger,
                singleLine = true,
                placeholder = { Text("Passenger name") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionCard(title = "Billing") {
            FieldLabel("Amount")
            OutlinedTextField(
                value = state.amountText,
                onValueChange = vm::setAmount,
                singleLine = true,
                prefix = { Text(state.currencySymbol) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                placeholder = { Text("0.00") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            FieldLabel("Notes (optional)")
            OutlinedTextField(
                value = state.notes,
                onValueChange = vm::setNotes,
                placeholder = { Text("Toll, waiting charges, etc.") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(20.dp))
        GradientButton(
            text = if ((state.amount ?: 0.0) > 0.0)
                "Save Invoice #${state.nextInvoiceCode} · ${Formatters.amount(state.amount ?: 0.0, state.currencySymbol)}"
            else "Save Invoice #${state.nextInvoiceCode}",
            onClick = {
                focusManager.clearFocus()
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.save()
            },
            enabled = state.canSave,
            loading = state.saving,
            leadingIcon = Icons.Filled.Check,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showFrom) {
        LocationPickerDialog(
            title = "Pickup location",
            onSelected = { vm.setFrom(it); showFrom = false },
            onDismiss = { showFrom = false }
        )
    }
    if (showTo) {
        LocationPickerDialog(
            title = "Drop location",
            onSelected = { vm.setTo(it); showTo = false },
            onDismiss = { showTo = false }
        )
    }
    if (showDate) {
        DatePickerModal(
            initialDate = state.date,
            onDateSelected = vm::setDate,
            onDismiss = { showDate = false }
        )
    }
}

@Composable
private fun DashboardHero(company: String, nextInvoice: String) {
    val onBrand = MaterialTheme.colorScheme.onPrimary
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.background(AppGradients.brand())) {
            Icon(
                Icons.Filled.LocalTaxi,
                contentDescription = null,
                tint = onBrand.copy(alpha = 0.12f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .size(104.dp)
            )
            Column(Modifier.padding(20.dp)) {
                Text(
                    company.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = onBrand.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "New Bill",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = onBrand
                )
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(10.dp), color = onBrand.copy(alpha = 0.18f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = onBrand,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Next invoice #$nextInvoice",
                            style = MaterialTheme.typography.labelLarge,
                            color = onBrand,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoBanner(text: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
private fun SavedBanner(code: String, amount: String, onView: () -> Unit, onDismiss: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.secondary) {
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Invoice #$code saved",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    amount,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            TextButton(onClick = onView) { Text("View") }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}
