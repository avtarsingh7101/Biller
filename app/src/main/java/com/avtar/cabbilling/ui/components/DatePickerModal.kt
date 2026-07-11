package com.avtar.cabbilling.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Wraps the Material 3 date picker. All conversions use UTC so the calendar day
 * the user taps is exactly the day returned (no timezone off-by-one).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { millis ->
                    onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = pickerState)
    }
}
