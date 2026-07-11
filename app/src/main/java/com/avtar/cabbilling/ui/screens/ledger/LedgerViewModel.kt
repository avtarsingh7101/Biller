package com.avtar.cabbilling.ui.screens.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avtar.cabbilling.data.local.entity.BillEntity
import com.avtar.cabbilling.data.repository.BillRepository
import com.avtar.cabbilling.data.repository.ConfigRepository
import com.avtar.cabbilling.util.Formatters
import com.avtar.cabbilling.util.toUserMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class DateFilter(val label: String) {
    ALL("All"), TODAY("Today"), THIS_MONTH("This month")
}

data class LedgerUiState(
    val query: String = "",
    val filter: DateFilter = DateFilter.ALL,
    val bills: List<BillEntity> = emptyList(),
    val currencySymbol: String = "₹",
    val total: Double = 0.0,
    val count: Int = 0
)

class LedgerViewModel(
    private val billRepository: BillRepository,
    configRepository: ConfigRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(DateFilter.ALL)

    private val events = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = events.receiveAsFlow()

    val uiState: StateFlow<LedgerUiState> =
        combine(billRepository.observeAll(), configRepository.config, query, filter) { bills, config, q, f ->
            val today = LocalDate.now()
            val filtered = bills
                .filter { matchesDate(it, f, today) }
                .filter { matchesQuery(it, q) }
            LedgerUiState(
                query = q,
                filter = f,
                bills = filtered,
                currencySymbol = config?.currencySymbol ?: "₹",
                total = filtered.sumOf { it.amount },
                count = filtered.size
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LedgerUiState())

    fun setQuery(value: String) { query.value = value }
    fun setFilter(value: DateFilter) { filter.value = value }

    fun delete(bill: BillEntity) {
        viewModelScope.launch {
            billRepository.deleteBill(bill)
                .onSuccess { events.send("Invoice #${bill.invoiceCode} deleted") }
                .onFailure { events.send(it.toUserMessage("Could not delete the bill.")) }
        }
    }

    private fun matchesQuery(bill: BillEntity, q: String): Boolean {
        val term = q.trim()
        if (term.isEmpty()) return true
        return bill.invoiceCode.contains(term, ignoreCase = true) ||
            bill.fromLocation.contains(term, ignoreCase = true) ||
            bill.toLocation.contains(term, ignoreCase = true) ||
            bill.carName.contains(term, ignoreCase = true) ||
            bill.passengerName.contains(term, ignoreCase = true)
    }

    private fun matchesDate(bill: BillEntity, f: DateFilter, today: LocalDate): Boolean {
        if (f == DateFilter.ALL) return true
        val date = Formatters.localDate(bill.dateEpochMillis)
        return when (f) {
            DateFilter.TODAY -> date == today
            DateFilter.THIS_MONTH -> date.year == today.year && date.month == today.month
            DateFilter.ALL -> true
        }
    }
}
