package com.avtar.cabbilling.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.avtar.cabbilling.ui.components.BillCard
import com.avtar.cabbilling.ui.components.ScreenHeader
import com.avtar.cabbilling.ui.components.SectionCard
import com.avtar.cabbilling.ui.components.StatTile
import com.avtar.cabbilling.ui.containerViewModel
import com.avtar.cabbilling.util.Formatters
import java.time.LocalDate

private val WEEKDAYS = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

@Composable
fun CalendarScreen(onOpenBill: (Long) -> Unit) {
    val vm = containerViewModel { CalendarViewModel(it.billRepository, it.configRepository) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ScreenHeader(title = "Calendar")

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = vm::previousMonth) {
                    Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                }
                Text(
                    Formatters.monthYear.format(state.month.atDay(1)),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = vm::nextMonth) {
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next month")
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                WEEKDAYS.forEach { d ->
                    Text(
                        d,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            MonthGrid(
                cells = buildCells(state.month.lengthOfMonth(), leadingBlanks(state.month.atDay(1))),
                onDayClick = { day -> vm.selectDate(state.month.atDay(day)) },
                isSelected = { day -> state.selectedDate == state.month.atDay(day) },
                isToday = { day -> today == state.month.atDay(day) },
                amountFor = { day -> state.dailyTotals[state.month.atDay(day)]?.total ?: 0.0 },
                currencySymbol = state.currencySymbol
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = "Month trips",
                value = state.monthCount.toString(),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Month earnings",
                value = Formatters.amountCompact(state.monthTotal, state.currencySymbol),
                modifier = Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(Modifier.height(16.dp))
        DaySection(state = state, onOpenBill = onOpenBill)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MonthGrid(
    cells: List<Int?>,
    onDayClick: (Int) -> Unit,
    isSelected: (Int) -> Boolean,
    isToday: (Int) -> Boolean,
    amountFor: (Int) -> Double,
    currencySymbol: String
) {
    Column {
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    // Each cell claims an equal 1/7 width and is kept square, so
                    // blank leading cells hold their place and days stay aligned.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(3.dp)
                    ) {
                        if (day != null) {
                            DayCell(
                                day = day,
                                selected = isSelected(day),
                                today = isToday(day),
                                amount = amountFor(day),
                                currencySymbol = currencySymbol,
                                onClick = { onDayClick(day) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    selected: Boolean,
    today: Boolean,
    amount: Double,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .then(
                if (today && !selected)
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = fg,
                fontWeight = if (today || selected) FontWeight.Bold else FontWeight.Normal
            )
            if (amount > 0.0) {
                Text(
                    Formatters.amountShort(amount, currencySymbol),
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    maxLines = 1,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun DaySection(
    state: CalendarUiState,
    onOpenBill: (Long) -> Unit
) {
    val selected = state.selectedDate
    if (selected == null) {
        Text(
            "Select a date to see its trips.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val summary = state.selectedSummary
    Text(
        Formatters.dateFull.format(selected),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(2.dp))
    Text(
        "${summary.count} trip(s) · ${Formatters.amount(summary.total, state.currencySymbol)}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(Modifier.height(12.dp))
    if (state.selectedBills.isEmpty()) {
        Text(
            "No trips recorded on this day.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.selectedBills.forEach { bill ->
                BillCard(
                    bill = bill,
                    currencySymbol = state.currencySymbol,
                    onClick = { onOpenBill(bill.id) }
                )
            }
        }
    }
}

/** Number of empty leading cells so day 1 lands under the correct weekday (Sunday-start). */
private fun leadingBlanks(firstOfMonth: LocalDate): Int = firstOfMonth.dayOfWeek.value % 7

private fun buildCells(daysInMonth: Int, leading: Int): List<Int?> {
    val cells = ArrayList<Int?>(leading + daysInMonth)
    repeat(leading) { cells.add(null) }
    for (d in 1..daysInMonth) cells.add(d)
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}
