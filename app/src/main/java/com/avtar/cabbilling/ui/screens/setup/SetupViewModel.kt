package com.avtar.cabbilling.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avtar.cabbilling.data.model.AppTheme
import com.avtar.cabbilling.data.model.Car
import com.avtar.cabbilling.data.repository.ConfigRepository
import com.avtar.cabbilling.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetupUiState(
    val companyName: String = "",
    val companyPhone: String = "",
    val carName: String = "",
    val carPlate: String = "",
    val currencySymbol: String = "₹",
    val pin: String = "",
    val confirmPin: String = "",
    val saving: Boolean = false,
    val error: String? = null
) {
    val pinValid: Boolean get() = pin.length in 4..6
    val pinsMatch: Boolean get() = pin == confirmPin
    val canFinish: Boolean
        get() = companyName.isNotBlank() && carName.isNotBlank() &&
            currencySymbol.isNotBlank() && pinValid && pinsMatch && !saving
}

class SetupViewModel(private val configRepository: ConfigRepository) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    fun onCompanyChange(value: String) = _state.update { it.copy(companyName = value, error = null) }

    fun onPhoneChange(value: String) = _state.update {
        it.copy(companyPhone = value.filter { c -> c.isDigit() || c == '+' || c == ' ' }.take(15), error = null)
    }

    fun onCarNameChange(value: String) = _state.update { it.copy(carName = value, error = null) }
    fun onCarPlateChange(value: String) = _state.update { it.copy(carPlate = value.uppercase(), error = null) }

    fun onCurrencyChange(value: String) =
        _state.update { it.copy(currencySymbol = value.take(3), error = null) }

    fun onPinChange(value: String) =
        _state.update { it.copy(pin = value.filter(Char::isDigit).take(6), error = null) }

    fun onConfirmPinChange(value: String) =
        _state.update { it.copy(confirmPin = value.filter(Char::isDigit).take(6), error = null) }

    fun submit(onComplete: () -> Unit) {
        val s = _state.value
        if (!s.canFinish) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            val car = Car(s.carName.trim(), s.carPlate.trim())
            configRepository.completeSetup(
                companyName = s.companyName,
                companyPhone = s.companyPhone,
                carNames = listOf(car.toStorage()),
                currencySymbol = s.currencySymbol,
                pin = s.pin,
                theme = AppTheme.AUTO
            ).onSuccess {
                onComplete()
            }.onFailure { t ->
                _state.update { it.copy(saving = false, error = t.toUserMessage("Could not save setup.")) }
            }
        }
    }
}
