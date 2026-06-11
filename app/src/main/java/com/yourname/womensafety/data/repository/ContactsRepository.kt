package com.yourname.womensafety.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yourname.womensafety.data.local.AppCache
import com.yourname.womensafety.data.network.api.ContactsApiService
import com.yourname.womensafety.data.network.dto.*

class ContactsRepository(
    private val contactsApi: ContactsApiService,
    private val appCache: AppCache
) : BaseRepository() {

    private val gson = Gson()
    private val contactsListType = object : TypeToken<List<TrustedContact>>() {}.type

    // ── Cache helpers ─────────────────────────────────────────────────────────

    /** Returns the cached contacts list, or null if cache is empty. */
    suspend fun getCachedContacts(): List<TrustedContact>? {
        val json = appCache.getContactsJson() ?: return null
        return try { gson.fromJson(json, contactsListType) } catch (e: Exception) { null }
    }

    private suspend fun cacheIsFresh(): Boolean =
        AppCache.isFresh(appCache.getContactsCachedAt(), AppCache.TTL_CONTACTS_MS)

    private suspend fun persistToCache(contacts: List<TrustedContact>) {
        appCache.saveContacts(gson.toJson(contacts))
    }

    // ── API calls ─────────────────────────────────────────────────────────────

    /**
     * Fetches contacts from the network, updates the cache, and returns the result.
     * For cache-first loading, call [getCachedContacts] first, then call this in background.
     */
    suspend fun getContacts(): NetworkResult<List<TrustedContact>> {
        val result = safeApiCall { contactsApi.getContacts() }
        if (result is NetworkResult.Success) persistToCache(result.data)
        return result
    }

    suspend fun addContact(request: AddContactRequest): NetworkResult<AddContactResponse> {
        val result = safeApiCall { contactsApi.addContact(request) }
        // Invalidate cache so next load fetches fresh list
        if (result is NetworkResult.Success) appCache.saveContacts("")
        return result
    }

    suspend fun verifyContactOtp(request: VerifyContactOtpRequest): NetworkResult<TrustedContact> {
        val result = safeApiCall { contactsApi.verifyContactOtp(request) }
        if (result is NetworkResult.Success) appCache.saveContacts("")
        return result
    }

    suspend fun resendContactOtp(request: ResendContactOtpRequest): NetworkResult<ResendContactOtpResponse> {
        return safeApiCall { contactsApi.resendContactOtp(request) }
    }

    suspend fun updateContact(
        contactId: String,
        request: UpdateContactRequest
    ): NetworkResult<TrustedContact> {
        val result = safeApiCall { contactsApi.updateContact(contactId, request) }
        if (result is NetworkResult.Success) appCache.saveContacts("")
        return result
    }

    suspend fun deleteContact(contactId: String): NetworkResult<Unit> {
        val result = safeApiCall { contactsApi.deleteContact(contactId) }
        if (result is NetworkResult.Success) appCache.saveContacts("")
        return result
    }

    suspend fun setPrimaryContact(contactId: String): NetworkResult<TrustedContact> {
        val result = safeApiCall { contactsApi.setPrimaryContact(contactId) }
        if (result is NetworkResult.Success) appCache.saveContacts("")
        return result
    }
}
