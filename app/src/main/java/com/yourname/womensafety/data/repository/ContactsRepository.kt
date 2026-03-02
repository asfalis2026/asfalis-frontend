package com.yourname.womensafety.data.repository

import com.yourname.womensafety.data.network.api.ContactsApiService
import com.yourname.womensafety.data.network.dto.*

class ContactsRepository(
    private val contactsApi: ContactsApiService
) : BaseRepository() {

    suspend fun getContacts(): NetworkResult<List<TrustedContact>> {
        return safeApiCall { contactsApi.getContacts() }
    }

    suspend fun addContact(request: AddContactRequest): NetworkResult<TrustedContact> {
        return safeApiCall { contactsApi.addContact(request) }
    }

    suspend fun updateContact(
        contactId: String,
        request: UpdateContactRequest
    ): NetworkResult<TrustedContact> {
        return safeApiCall { contactsApi.updateContact(contactId, request) }
    }

    suspend fun deleteContact(contactId: String): NetworkResult<Unit> {
        return safeApiCall { contactsApi.deleteContact(contactId) }
    }

    suspend fun setPrimaryContact(contactId: String): NetworkResult<TrustedContact> {
        return safeApiCall { contactsApi.setPrimaryContact(contactId) }
    }
}
