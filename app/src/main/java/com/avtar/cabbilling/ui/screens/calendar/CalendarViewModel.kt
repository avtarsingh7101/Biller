package com.avtar.cabbilling.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avtar.cabbilling.data.local.entity.BillEntity
import com.avtar.cabbilling.data.repository.BillRepository
import com.avtar.cabbilling.data.repository.ConfigRepository
import com.avtar.cabbilling.util.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class DaySummary(val count: Int, val total: Double)

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate? = LocalDate.now(),
    val currencySymbol: String = "₹",
    val dailyTotals: Map<LocalDate, DaySummary> = emptyMap(),
    val monthTotal: Double = 0.0,
    val monthCount: Int = 0,
    val selectedBills: List<BillEntity> = emptyList()
) {
    val selectedSummary: DaySummary
        get() = DaySummary(selectedBills.size, selectedBills.sumOf { it.amount })
}

class CalendarViewModel(
    billRepository: BillRepository,
    configRepository: ConfigRepository
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow<LocalDate?>(LocalDate.now())

    val uiState: StateFlow<CalendarUiState> =
        combine(
            billRepository.observeAll(),
            configRepository.config,
            month,
            selectedDate
        ) { bills, config, m, selected ->
            val monthBills = bills.filter { YearMonth.from(Formatters.localDate(it.dateEpochMillis)) == m }
            val daily = monthBills
                .groupBy { Formatters.localDate(it.dateEpochMillis) }
                .mapValues { (_, list) -> DaySummary(list.size, list.sumOf { it.amount }) }
            val selectedBills = selected
                ?.let { d -> bills.filter { Formatters.localDate(it.dateEpochMillis) == d } }
                .orEmpty()

            CalendarUiState(
                month = m,
                selectedDate = selected,
                currencySymbol = config?.currencySymbol ?: "₹",
                dailyTotals = daily,
                monthTotal = monthBills.sumOf { it.amount },
                monthCount = monthBills.size,
                selectedBills = selectedBills
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
        selectedDate.value = null
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
        selectedDate.value = null
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }
}
