package com.avtar.cabbilling.ui.screens.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avtar.cabbilling.data.local.entity.BillEntity
import com.avtar.cabbilling.ui.components.BillCard
import com.avtar.cabbilling.ui.components.ConfirmDialog
import com.avtar.cabbilling.ui.components.EmptyState
import com.avtar.cabbilling.ui.components.ScreenHeader
import com.avtar.cabbilling.ui.components.StatTile
import com.avtar.cabbilling.ui.containerViewModel
import com.avtar.cabbilling.util.Formatters

@Composable
fun LedgerScreen(
    onOpenBill: (Long) -> Unit,
    onMessage: (String) -> Unit
) {
    val vm = containerViewModel { LedgerViewModel(it.billRepository, it.configRepository) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<BillEntity?>(null) }
    val haptics = LocalHapticFeedback.current

    LaunchedMessages(vm, onMessage)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeader(title = "Ledger", subtitle = "${state.count} saved invoice(s)")

        OutlinedTextField(
            value = state.query,
            onValueChange = vm::setQuery,
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { vm.setQuery("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            placeholder = { Text("Search invoice, route, car, passenger") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateFilter.entries.forEach { f ->
                FilterChip(
                    selected = state.filter == f,
                    onClick = { vm.setFilter(f) },
                    label = { Text(f.label) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = "Invoices",
                value = state.count.toString(),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Total billed",
                value = Formatters.amountCompact(state.total, state.currencySymbol),
                modifier = Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(Modifier.height(12.dp))
        if (state.bills.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.ListAlt,
                title = "No invoices found",
                message = if (state.query.isNotEmpty() || state.filter != DateFilter.ALL)
                    "Try clearing the search or filter."
                else "Saved bills will appear here.",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
            ) {
                items(state.bills, key = { it.id }) { bill ->
                    BillCard(
                        bill = bill,
                        currencySymbol = state.currencySymbol,
                        onClick = { onOpenBill(bill.id) },
                        onDelete = { pendingDelete = bill },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    pendingDelete?.let { bill ->
        ConfirmDialog(
            title = "Delete invoice?",
            message = "Invoice #${bill.invoiceCode} · ${Formatters.amount(bill.amount, state.currencySymbol)} " +
                "(${bill.fromLocation} → ${bill.toLocation}) will be permanently removed — gone for good, no backup.",
            confirmLabel = "Delete",
            onConfirm = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.delete(bill)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun LaunchedMessages(vm: LedgerViewModel, onMessage: (String) -> Unit) {
    androidx.compose.runtime.LaunchedEffect(vm) {
        vm.messages.collect { onMessage(it) }
    }
}
