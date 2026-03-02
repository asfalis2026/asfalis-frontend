package com.yourname.womensafety.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.womensafety.data.AppServiceLocator
import com.yourname.womensafety.data.network.dto.FaqItem
import com.yourname.womensafety.data.repository.NetworkResult
import com.yourname.womensafety.data.repository.SupportRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class HelpUiState {
    data object Loading : HelpUiState()
    data class Success(val faqs: List<FaqItem>) : HelpUiState()
    data class Error(val message: String) : HelpUiState()
}

sealed class TicketUiState {
    data object Idle : TicketUiState()
    data object Submitting : TicketUiState()
    data object Submitted : TicketUiState()
    data class Error(val message: String) : TicketUiState()
}

@OptIn(FlowPreview::class)
class HelpSupportViewModel(
    private val supportRepository: SupportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HelpUiState>(HelpUiState.Loading)
    val uiState: StateFlow<HelpUiState> = _uiState

    private val _ticketState = MutableStateFlow<TicketUiState>(TicketUiState.Idle)
    val ticketState: StateFlow<TicketUiState> = _ticketState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    init {
        // Debounce search — wait 400ms after user stops typing then hit API
        viewModelScope.launch {
            _searchQuery
                .debounce(400L)
                .collectLatest { query ->
                    fetchFaqs(query.takeIf { it.isNotBlank() })
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private suspend fun fetchFaqs(search: String? = null) {
        _uiState.value = HelpUiState.Loading
        when (val result = supportRepository.getFaqs(search)) {
            is NetworkResult.Success -> _uiState.value = HelpUiState.Success(result.data)
            is NetworkResult.Error   -> _uiState.value = HelpUiState.Error(result.message)
            is NetworkResult.Loading -> Unit
        }
    }

    fun submitTicket(subject: String, message: String) {
        viewModelScope.launch {
            _ticketState.value = TicketUiState.Submitting
            when (val result = supportRepository.createTicket(subject, message)) {
                is NetworkResult.Success -> _ticketState.value = TicketUiState.Submitted
                is NetworkResult.Error   -> _ticketState.value = TicketUiState.Error(result.message)
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun resetTicketState() {
        _ticketState.value = TicketUiState.Idle
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HelpSupportViewModel(AppServiceLocator.supportRepository) as T
            }
        }
    }
}
