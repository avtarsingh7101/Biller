package com.avtar.cabbilling.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avtar.cabbilling.data.local.entity.BillEntity
import com.avtar.cabbilling.data.repository.BillRepository
import com.avtar.cabbilling.data.repository.ConfigRepository
import com.avtar.cabbilling.util.toUserMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val loading: Boolean = true,
    val bill: BillEntity? = null,
    val currencySymbol: String = "₹",
    val companyPhone: String = ""
)

class DetailViewModel(
    private val billRepository: BillRepository,
    configRepository: ConfigRepository,
    billId: Long
) : ViewModel() {

    private val events = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = events.receiveAsFlow()

    val uiState: StateFlow<DetailUiState> =
        combine(billRepository.observeById(billId), configRepository.config) { bill, config ->
            DetailUiState(
                loading = false,
                bill = bill,
                currencySymbol = config?.currencySymbol ?: "₹",
                companyPhone = config?.companyPhone ?: ""
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun delete(onDeleted: () -> Unit) {
        val bill = uiState.value.bill ?: return
        viewModelScope.launch {
            billRepository.deleteBill(bill)
                .onSuccess { onDeleted() }
                .onFailure { events.send(it.toUserMessage("Could not delete the bill.")) }
        }
    }
}
