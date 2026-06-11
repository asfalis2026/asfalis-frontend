package com.yourname.womensafety.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yourname.womensafety.data.local.AppCache
import com.yourname.womensafety.data.network.api.SupportApiService
import com.yourname.womensafety.data.network.dto.CreateTicketRequest
import com.yourname.womensafety.data.network.dto.FaqItem
import com.yourname.womensafety.data.network.dto.TicketData

class SupportRepository(
    private val supportApi: SupportApiService,
    private val appCache: AppCache
) : BaseRepository() {

    private val gson = Gson()
    private val faqsListType = object : TypeToken<List<FaqItem>>() {}.type

    // ── Cache helpers ─────────────────────────────────────────────────────────

    /** Returns cached FAQs list, or null if not cached. Only valid for empty search query. */
    suspend fun getCachedFaqs(): List<FaqItem>? {
        val json = appCache.getFaqsJson() ?: return null
        return try { gson.fromJson(json, faqsListType) } catch (e: Exception) { null }
    }

    suspend fun faqCacheIsFresh(): Boolean =
        AppCache.isFresh(appCache.getFaqsCachedAt(), AppCache.TTL_FAQS_MS)

    // ── API calls ─────────────────────────────────────────────────────────────

    /**
     * Fetches FAQs from the network.
     * For empty [search] queries, also persists to cache for instant future loads.
     */
    suspend fun getFaqs(search: String? = null): NetworkResult<List<FaqItem>> {
        val result = safeApiCall { supportApi.getFaqs(search.takeIf { !it.isNullOrBlank() }) }
        // Only cache unfiltered results (empty search = full FAQ list)
        if (result is NetworkResult.Success && search.isNullOrBlank()) {
            appCache.saveFaqs(gson.toJson(result.data))
        }
        return result
    }

    suspend fun createTicket(subject: String, message: String): NetworkResult<TicketData> {
        return safeApiCall { supportApi.createTicket(CreateTicketRequest(subject, message)) }
    }

    suspend fun getTickets(): NetworkResult<List<TicketData>> {
        return safeApiCall { supportApi.getTickets() }
    }
}
