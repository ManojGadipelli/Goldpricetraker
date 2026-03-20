package com.goldprice.india

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldprice.india.data.City
import com.goldprice.india.data.GoldData
import com.goldprice.india.data.GoldRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class GoldUiState {
    object Loading : GoldUiState()
    data class Success(val data: GoldData) : GoldUiState()
    data class Error(val message: String) : GoldUiState()
}

enum class PriceUnit(val label: String, val multiplier: Double) {
    PER_GRAM("Per Gram", 1.0),
    PER_10_GRAM("Per 10g", 10.0),
    PER_TOLA("Per Tola", 11.664),
}

class GoldViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<GoldUiState>(GoldUiState.Loading)
    val uiState: StateFlow<GoldUiState> = _uiState.asStateFlow()

    private val _selectedCity = MutableStateFlow(GoldRepository.getCities().first())
    val selectedCity: StateFlow<City> = _selectedCity.asStateFlow()

    private val _includeGst = MutableStateFlow(false)
    val includeGst: StateFlow<Boolean> = _includeGst.asStateFlow()

    private val _priceUnit = MutableStateFlow(PriceUnit.PER_GRAM)
    val priceUnit: StateFlow<PriceUnit> = _priceUnit.asStateFlow()

    val cities = GoldRepository.getCities()

    init {
        fetchPrices()
    }

    fun fetchPrices() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = GoldUiState.Loading
            val result = GoldRepository.fetchGoldPrices(_selectedCity.value.slug)
            result.fold(
                onSuccess = { data ->
                    if (data.prices.isEmpty()) {
                        _uiState.value = GoldUiState.Error("No price data found. Please try again.")
                    } else {
                        _uiState.value = GoldUiState.Success(data)
                    }
                },
                onFailure = { e ->
                    _uiState.value = GoldUiState.Error(e.message ?: "Failed to fetch prices")
                }
            )
        }
    }

    fun selectCity(city: City) {
        _selectedCity.value = city
        fetchPrices()
    }

    fun toggleGst() {
        _includeGst.value = !_includeGst.value
    }

    fun selectUnit(unit: PriceUnit) {
        _priceUnit.value = unit
    }

    fun computePrice(basePerGram: Long, includeGst: Boolean, unit: PriceUnit): Long {
        var price = basePerGram * unit.multiplier
        if (includeGst) price *= 1.03
        return price.toLong()
    }
}
