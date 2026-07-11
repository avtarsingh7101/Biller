package com.avtar.cabbilling.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avtar.cabbilling.data.model.AppTheme
import com.avtar.cabbilling.data.model.Car
import com.avtar.cabbilling.data.repository.BillRepository
import com.avtar.cabbilling.data.repository.ConfigRepository
import com.avtar.cabbilling.util.toUserMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val loaded: Boolean = false,
    val companyName: String = "",
    val companyPhone: String = "",
    val currencySymbol: String = "₹",
    val carDraft: String = "",
    val plateDraft: String = "",
    val cars: List<Car> = emptyList(),
    val theme: AppTheme = AppTheme.AUTO,
    val saving: Boolean = false
) {
    val canSaveProfile: Boolean
        get() = companyName.isNotBlank() && currencySymbol.isNotBlank() && cars.isNotEmpty() && !saving
}

class SettingsViewModel(
    private val billRepository: BillRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val events = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = events.receiveAsFlow()

    init {
        viewModelScope.launch {
            configRepository.config.collect { config ->
                if (config == null) return@collect
                _state.update { s ->
                    if (s.loaded) {
                        s.copy(theme = AppTheme.fromId(config.theme))
                    } else {
                        s.copy(
                            loaded = true,
                            companyName = config.companyName,
                            companyPhone = config.companyPhone ?: "",
                            currencySymbol = config.currencySymbol,
                            cars = config.carNames.map { Car.fromStorage(it) },
                            theme = AppTheme.fromId(config.theme)
                        )
                    }
                }
            }
        }
    }

    fun onCompanyChange(value: String) = _state.update { it.copy(companyName = value) }
    fun onPhoneChange(value: String) = _state.update {
        it.copy(companyPhone = value.filter { c -> c.isDigit() || c == '+' || c == ' ' }.take(15))
    }
    fun onCurrencyChange(value: String) = _state.update { it.copy(currencySymbol = value.take(3)) }
    fun onCarDraftChange(value: String) = _state.update { it.copy(carDraft = value) }
    fun onPlateDraftChange(value: String) = _state.update { it.copy(plateDraft = value.uppercase()) }

    fun addCar() = _state.update { s ->
        val name = s.carDraft.trim()
        val plate = s.plateDraft.trim()
        if (name.isEmpty() || s.cars.any { it.name.equals(name, ignoreCase = true) }) {
            s.copy(carDraft = "", plateDraft = "")
        } else {
            s.copy(cars = s.cars + Car(name, plate), carDraft = "", plateDraft = "")
        }
    }

    fun removeCar(car: Car) = _state.update { it.copy(cars = it.cars - car) }

    fun saveProfile() {
        val s = _state.value
        if (!s.canSaveProfile) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            configRepository.updateProfile(
                s.companyName,
                s.companyPhone,
                s.cars.map { it.toStorage() },
                s.currencySymbol
            )
                .onSuccess { events.send("Profile saved") }
                .onFailure { events.send(it.toUserMessage("Could not save profile.")) }
            _state.update { it.copy(saving = false) }
        }
    }

    fun selectTheme(theme: AppTheme) {
        _state.update { it.copy(theme = theme) }
        viewModelScope.launch { configRepository.updateTheme(theme) }
    }

    fun changePin(newPin: String, onDone: () -> Unit) {
        viewModelScope.launch {
            configRepository.changePin(newPin)
                .onSuccess { events.send("PIN updated"); onDone() }
                .onFailure { events.send(it.toUserMessage("Could not update PIN.")) }
        }
    }

    fun resetApp() {
        viewModelScope.launch {
            billRepository.resetApp()
                .onFailure { events.send(it.toUserMessage("Could not reset the app.")) }
        }
    }
}
