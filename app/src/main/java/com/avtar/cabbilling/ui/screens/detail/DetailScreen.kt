package com.avtar.cabbilling.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avtar.cabbilling.data.local.entity.BillEntity
import com.avtar.cabbilling.ui.components.ConfirmDialog
import com.avtar.cabbilling.ui.containerViewModel
import com.avtar.cabbilling.ui.theme.AppGradients
import com.avtar.cabbilling.util.Formatters
import com.avtar.cabbilling.util.InvoiceExporter
import com.avtar.cabbilling.util.toUserMessage

@Composable
fun BillDetailScreen(
    billId: Long,
    onBack: () -> Unit,
    onMessage: (String) -> Unit
) {
    val vm = containerViewModel { DetailViewModel(it.billRepository, it.configRepository, billId) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(vm) { vm.messages.collect { onMessage(it) } }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = state.bill?.let { "Invoice #${it.invoiceCode}" } ?: "Invoice",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            if (state.bill != null) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        val bill = state.bill
        when {
            state.loading -> Unit
            bill == null -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(48.dp))
                Text("This invoice is no longer available.", style = MaterialTheme.typography.bodyLarge)
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Receipt(bill = bill, currencySymbol = state.currencySymbol, companyPhone = state.companyPhone)

                Spacer(Modifier.height(20.dp))
                WhatsAppButton(
                    onClick = {
                        InvoiceExporter.shareToWhatsApp(context, bill, state.currencySymbol, state.companyPhone)
                            .onFailure { onMessage(it.toUserMessage("Couldn't open WhatsApp.")) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            InvoiceExporter.sharePdf(context, bill, state.currencySymbol, state.companyPhone)
                                .onFailure { onMessage(it.toUserMessage("Couldn't create the PDF.")) }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("PDF")
                    }
                    OutlinedButton(
                        onClick = {
                            InvoiceExporter.shareAsImage(context, bill, state.currencySymbol, state.companyPhone)
                                .onFailure { onMessage(it.toUserMessage("Couldn't create the image.")) }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Image")
                    }
                    OutlinedButton(
                        onClick = {
                            InvoiceExporter.sharePlainText(context, bill, state.currencySymbol, state.companyPhone)
                                .onFailure { onMessage(it.toUserMessage("Couldn't share the text.")) }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Text")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (confirmDelete && state.bill != null) {
        val bill = state.bill!!
        ConfirmDialog(
            title = "Delete invoice?",
            message = "Invoice #${bill.invoiceCode} · ${Formatters.amount(bill.amount, state.currencySymbol)} will be permanently removed — gone for good, no backup.",
            confirmLabel = "Delete",
            onConfirm = {
                confirmDelete = false
                vm.delete(onDeleted = onBack)
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun WhatsAppButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val green = Color(0xFF25D366)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(green)
            .clickable(onClick = onClick)
            .heightIn(min = 54.dp)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Share, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(
                "Share on WhatsApp",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun Receipt(bill: BillEntity, currencySymbol: String, companyPhone: String) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Gradient header band
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppGradients.brand())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    bill.companyName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                if (companyPhone.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        companyPhone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "INVOICE · RECEIPT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    LabelValue("Invoice", "#${bill.invoiceCode}", Modifier.weight(1f))
                    LabelValue(
                        "Date",
                        Formatters.dateFull.format(Formatters.localDate(bill.dateEpochMillis)),
                        Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(16.dp))
                DetailRow(Icons.Filled.TripOrigin, "From", bill.fromLocation)
                DetailRow(Icons.Filled.Place, "To", bill.toLocation)
                DetailRow(Icons.Filled.DirectionsCar, "Car",
                    if (!bill.carPlate.isNullOrBlank()) "${bill.carName} · ${bill.carPlate}" else bill.carName
                )
                if (bill.passengerName.isNotBlank()) {
                    DetailRow(Icons.Filled.Person, "Passenger", bill.passengerName)
                }
                DetailRow(
                    Icons.Filled.CalendarMonth,
                    "Created",
                    Formatters.dateTime.format(Formatters.localDateTime(bill.createdAtMillis))
                )
                if (bill.notes.isNotBlank()) {
                    DetailRow(Icons.Filled.Notes, "Notes", bill.notes)
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "TOTAL",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        Formatters.amount(bill.amount, currencySymbol),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
