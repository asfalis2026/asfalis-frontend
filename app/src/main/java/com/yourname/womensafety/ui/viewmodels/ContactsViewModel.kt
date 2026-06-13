package com.yourname.womensafety.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.womensafety.data.AppServiceLocator
import com.yourname.womensafety.data.network.dto.AddContactRequest
import com.yourname.womensafety.data.network.dto.TrustedContact
import com.yourname.womensafety.data.network.dto.UpdateContactRequest
import com.yourname.womensafety.data.network.dto.VerifyContactOtpRequest
import com.yourname.womensafety.data.network.dto.ResendContactOtpRequest
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

    /** True only during a background network refresh after cached data is shown. */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    data class OtpVerificationData(
        val contactId: String,
        val phone: String,
        val name: String,
        val expiresInSeconds: Int
    )
    private val _pendingOtpVerification = MutableStateFlow<OtpVerificationData?>(null)
    val pendingOtpVerification: StateFlow<OtpVerificationData?> = _pendingOtpVerification

    /**
     * Load contacts on creation using cache-first strategy:
     * 1. Show cached data immediately (no spinner, instant render)
     * 2. Refresh from network in background (silently update list)
     */
    init {
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch {
            // Step 1: Populate from cache instantly (no loading indicator needed)
            val cached = contactsRepository.getCachedContacts()
            if (!cached.isNullOrEmpty()) {
                _contacts.value = cached
                // Show subtle loading only for background refresh, not initial load
                _isLoading.value = false
            } else {
                // No cache — show full loading indicator
                _isLoading.value = true
            }

            // Step 2: Background network refresh
            when (val result = contactsRepository.getContacts()) {
                is NetworkResult.Success -> {
                    _contacts.value = result.data
                    _isLoading.value = false
                }
                is NetworkResult.Error -> {
                    if (cached.isNullOrEmpty()) {
                        // Only show error if we have nothing to display
                        _errorMessage.value = result.message
                    }
                    _isLoading.value = false
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    /**
     * Adds a new trusted contact.
     * @param email Optional email address for the contact.
     */
    fun addContact(name: String, phone: String, relationship: String? = null, email: String? = null) {
        viewModelScope.launch {
            if (_contacts.value.size >= 3) {
                _errorMessage.value = "You can add up to 3 trusted contacts maximum."
                return@launch
            }
            _isLoading.value = true
            Log.d("ContactsViewModel", "addContact: name=$name phone=$phone email=$email")
            val request = AddContactRequest(
                name         = name,
                phone        = phone,
                email        = email,
                relationship = relationship,
                isPrimary    = false
            )
            when (val result = contactsRepository.addContact(request)) {
                is NetworkResult.Success -> {
                    Log.d("ContactsViewModel", "addContact success: contactId=${result.data.contactId}")
                    _pendingOtpVerification.value = OtpVerificationData(
                        contactId        = result.data.contactId,
                        phone            = result.data.phone,
                        name             = name,
                        expiresInSeconds = result.data.expiresInSeconds
                    )
                    _isLoading.value = false
                }
                is NetworkResult.Error -> {
                    Log.e("ContactsViewModel", "addContact error: ${result.message}")
                    _errorMessage.value = result.message
                    _isLoading.value = false
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun verifyContactOtp(contactId: String, otpCode: String, isPrimary: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d("ContactsViewModel", "verifyContactOtp: contactId=$contactId isPrimary=$isPrimary")
            val request = VerifyContactOtpRequest(contactId, otpCode, isPrimary)
            when (val result = contactsRepository.verifyContactOtp(request)) {
                is NetworkResult.Success -> {
                    Log.d("ContactsViewModel", "verifyContactOtp success: isVerified=${result.data.isVerified}")
                    loadContacts()
                }
                is NetworkResult.Error -> {
                    Log.e("ContactsViewModel", "verifyContactOtp error: ${result.message}")
                    _errorMessage.value = result.message
                    _isLoading.value = false
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun resendContactOtp(contactId: String, onSuccess: (Int) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val request = ResendContactOtpRequest(contactId)
            when (val result = contactsRepository.resendContactOtp(request)) {
                is NetworkResult.Success -> {
                    onSuccess(result.data.expiresInSeconds)
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

    fun clearOtpVerification() { _pendingOtpVerification.value = null }

    fun dismissInvite() { /* No longer used */ }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            // Optimistically remove from UI immediately
            _contacts.value = _contacts.value.filter { it.id != contactId }
            when (contactsRepository.deleteContact(contactId)) {
                is NetworkResult.Success -> Unit  // UI already updated
                is NetworkResult.Error   -> {
                    _errorMessage.value = "Failed to delete contact"
                    loadContacts()  // Revert by reloading
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun setPrimaryContact(contactId: String) {
        viewModelScope.launch {
            when (val result = contactsRepository.setPrimaryContact(contactId)) {
                is NetworkResult.Success -> {
                    _contacts.value = _contacts.value.map { contact ->
                        contact.copy(isPrimary = contact.id == contactId)
                    }
                }
                is NetworkResult.Error -> { _errorMessage.value = result.message }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    /**
     * Updates a contact's name, relationship, and optional email.
     * @param email Optional new email address.
     */
    fun updateContact(
        contactId: String,
        name: String?,
        phone: String?,
        relationship: String?,
        email: String? = null
    ) {
        viewModelScope.launch {
            // Optimistic update
            _contacts.value = _contacts.value.map {
                if (it.id == contactId) {
                    it.copy(
                        name = name ?: it.name,
                        phone = phone ?: it.phone,
                        relationship = relationship ?: it.relationship,
                        email = if (email == "") null else email ?: it.email // handle clear email
                    )
                } else it
            }

            val request = UpdateContactRequest(name, phone, relationship, email)
            when (val result = contactsRepository.updateContact(contactId, request)) {
                is NetworkResult.Success -> {
                    _contacts.value = _contacts.value.map {
                        if (it.id == contactId) result.data else it
                    }
                }
                is NetworkResult.Error   -> {
                    _errorMessage.value = result.message
                    loadContacts() // revert on error
                }
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
