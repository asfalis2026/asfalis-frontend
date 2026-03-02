package com.yourname.womensafety.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.womensafety.data.AppServiceLocator
import com.yourname.womensafety.data.network.dto.AddContactRequest
import com.yourname.womensafety.data.network.dto.TrustedContact
import com.yourname.womensafety.data.network.dto.UpdateContactRequest
import com.yourname.womensafety.data.repository.ContactsRepository
import com.yourname.womensafety.data.repository.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val contactsRepository: ContactsRepository
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<TrustedContact>>(emptyList())
    val contacts: StateFlow<List<TrustedContact>> = _contacts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    /** Set after a successful addContact — UI should show the invite dialog. */
    private val _pendingInviteContact = MutableStateFlow<TrustedContact?>(null)
    val pendingInviteContact: StateFlow<TrustedContact?> = _pendingInviteContact

    fun loadContacts() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = contactsRepository.getContacts()) {
                is NetworkResult.Success -> {
                    _contacts.value = result.data
                    _isLoading.value = false
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = result.message
                    _isLoading.value = false
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun addContact(name: String, phone: String, relationship: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            val request = AddContactRequest(name, phone, relationship)
            when (val result = contactsRepository.addContact(request)) {
                is NetworkResult.Success -> {
                    _contacts.value = _contacts.value + result.data
                    _pendingInviteContact.value = result.data  // trigger invite dialog
                    _isLoading.value = false
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = result.message
                    _isLoading.value = false
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun dismissInvite() { _pendingInviteContact.value = null }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            when (contactsRepository.deleteContact(contactId)) {
                is NetworkResult.Success -> {
                    _contacts.value = _contacts.value.filter { it.id != contactId }
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = "Failed to delete contact"
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun setPrimaryContact(contactId: String) {
        viewModelScope.launch {
            when (val result = contactsRepository.setPrimaryContact(contactId)) {
                is NetworkResult.Success -> {
                    // Update local list: set the returned contact as primary, clear others
                    _contacts.value = _contacts.value.map { contact ->
                        contact.copy(isPrimary = contact.id == contactId)
                    }
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = result.message
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun updateContact(contactId: String, name: String?, phone: String?, relationship: String?) {
        viewModelScope.launch {
            val request = UpdateContactRequest(name, phone, relationship)
            when (val result = contactsRepository.updateContact(contactId, request)) {
                is NetworkResult.Success -> {
                    _contacts.value = _contacts.value.map {
                        if (it.id == contactId) result.data else it
                    }
                }
                is NetworkResult.Error -> _errorMessage.value = result.message
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun clearError() { _errorMessage.value = null }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ContactsViewModel(AppServiceLocator.contactsRepository) as T
            }
        }
    }
}
