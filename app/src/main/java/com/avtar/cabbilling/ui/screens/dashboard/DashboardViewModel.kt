package com.avtar.cabbilling.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avtar.cabbilling.data.local.entity.BillEntity
import com.avtar.cabbilling.data.model.Car
import com.avtar.cabbilling.data.repository.BillRepository
import com.avtar.cabbilling.data.repository.ConfigRepository
import com.avtar.cabbilling.data.repository.NewBillInput
import com.avtar.cabbilling.util.InvoiceSequence
import com.avtar.cabbilling.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardUiState(
    val companyName: String = "",
    val currencySymbol: String = "₹",
    val cars: List<Car> = emptyList(),
    val selectedCar: Car? = null,
    val fromLocation: String = "",
    val toLocation: String = "",
    val passengerName: String = "",
    val amountText: String = "",
    val notes: String = "",
    val date: LocalDate = LocalDate.now(),
    val nextInvoiceCode: String = "0001",
    val saving: Boolean = false,
    val error: String? = null,
    val justSaved: BillEntity? = null
) {
    val amount: Double? get() = amountText.trim().toDoubleOrNull()
    val canSave: Boolean
        get() = selectedCar != null && fromLocation.isNotBlank() &&
            toLocation.isNotBlank() && (amount?.let { it > 0.0 } == true) && !saving
}

class DashboardViewModel(
    private val billRepository: BillRepository,
    configRepository: ConfigRepository
) : ViewModel() {

    private data class Form(
        val selectedCar: Car? = null,
        val fromLocation: String = "",
        val toLocation: String = "",
        val passengerName: String = "",
        val amountText: String = "",
        val notes: String = "",
        val date: LocalDate = LocalDate.now(),
        val saving: Boolean = false,
        val error: String? = null,
        val justSaved: BillEntity? = null
    )

    private val form = MutableStateFlow(Form())

    val uiState: StateFlow<DashboardUiState> =
        combine(configRepository.config, form) { config, f ->
            val year = f.date.year
            val nextNumber = InvoiceSequence.nextNumber(
                lastNumber = config?.lastInvoiceNumber ?: 0,
                lastYear = config?.lastInvoiceYear ?: 0,
                targetYear = year
            )
            val cars = config?.carNames.orEmpty().map { Car.fromStorage(it) }
            DashboardUiState(
                companyName = config?.companyName.orEmpty(),
                currencySymbol = config?.currencySymbol ?: "₹",
                cars = cars,
                selectedCar = f.selectedCar ?: cars.firstOrNull(),
                fromLocation = f.fromLocation,
                toLocation = f.toLocation,
                passengerName = f.passengerName,
                amountText = f.amountText,
                notes = f.notes,
                date = f.date,
                nextInvoiceCode = InvoiceSequence.code(nextNumber),
                saving = f.saving,
                error = f.error,
                justSaved = f.justSaved
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun selectCar(car: Car) = form.update { it.copy(selectedCar = car, justSaved = null) }
    fun setFrom(value: String) = form.update { it.copy(fromLocation = value, justSaved = null) }
    fun setTo(value: String) = form.update { it.copy(toLocation = value, justSaved = null) }
    fun swapRoute() = form.update { it.copy(fromLocation = it.toLocation, toLocation = it.fromLocation) }
    fun setPassenger(value: String) = form.update { it.copy(passengerName = value) }
    fun setNotes(value: String) = form.update { it.copy(notes = value) }
    fun setDate(value: LocalDate) = form.update { it.copy(date = value, justSaved = null) }

    fun setAmount(value: String) {
        val filtered = buildString {
            var dotSeen = false
            for (c in value) {
                if (c.isDigit()) append(c)
                else if (c == '.' && !dotSeen) { append(c); dotSeen = true }
            }
        }
        form.update { it.copy(amountText = filtered, justSaved = null) }
    }

    fun dismissSaved() = form.update { it.copy(justSaved = null) }
    fun clearError() = form.update { it.copy(error = null) }

    fun save() {
        val s = uiState.value
        val car = s.selectedCar
        val amount = s.amount
        if (car == null || s.fromLocation.isBlank() || s.toLocation.isBlank() || amount == null || amount <= 0.0) {
            form.update { it.copy(error = "Choose a car, route, and a valid amount.") }
            return
        }
        form.update { it.copy(saving = true, error = null, justSaved = null) }
        viewModelScope.launch {
            billRepository.createBill(
                NewBillInput(
                    carName = car.name,
                    carPlate = car.plate,
                    fromLocation = s.fromLocation,
                    toLocation = s.toLocation,
                    passengerName = s.passengerName,
                    amount = amount,
                    notes = s.notes,
                    date = s.date
                )
            ).onSuccess { bill ->
                form.value = Form(date = LocalDate.now(), justSaved = bill)
            }.onFailure { t ->
                form.update { it.copy(saving = false, error = t.toUserMessage("Could not save the bill.")) }
            }
        }
    }
}
