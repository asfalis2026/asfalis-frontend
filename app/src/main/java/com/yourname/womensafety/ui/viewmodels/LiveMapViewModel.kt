package com.yourname.womensafety.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.womensafety.data.AppServiceLocator
import com.yourname.womensafety.data.network.dto.SharedContact
import com.yourname.womensafety.data.repository.LocationRepository
import com.yourname.womensafety.data.repository.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LiveMapUiState(
    val isLoadingLocation: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val isSharing: Boolean = false,
    val sharedWith: List<SharedContact> = emptyList(),
    val isTogglingShare: Boolean = false,
    val errorMessage: String? = null
)

class LiveMapViewModel(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveMapUiState())
    val uiState: StateFlow<LiveMapUiState> = _uiState

    /** Called when we have device GPS — push coords to backend then pull current state. */
    fun updateAndLoad(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            locationRepository.updateLocation(latitude, longitude, isSharing = _uiState.value.isSharing)
            loadCurrentLocation()
        }
    }

    /** Pull current location & sharing state from backend. */
    fun loadCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingLocation = true, errorMessage = null)
            when (val result = locationRepository.getCurrentLocation()) {
                is NetworkResult.Success -> {
                    val loc = result.data
                    _uiState.value = _uiState.value.copy(
                        isLoadingLocation = false,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        address = loc.address,
                        isSharing = loc.isSharing
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingLocation = false,
                        errorMessage = result.message
                    )
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun startSharing() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTogglingShare = true)
            when (val result = locationRepository.startSharing()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isTogglingShare = false,
                        isSharing = true,
                        sharedWith = result.data.sharedWith
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isTogglingShare = false,
                        errorMessage = result.message
                    )
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun stopSharing() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTogglingShare = true)
            when (locationRepository.stopSharing()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isTogglingShare = false,
                        isSharing = false,
                        sharedWith = emptyList()
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isTogglingShare = false,
                        errorMessage = "Failed to stop sharing"
                    )
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LiveMapViewModel(AppServiceLocator.locationRepository) as T
            }
        }
    }
}
