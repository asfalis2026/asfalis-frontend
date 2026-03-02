package com.yourname.womensafety.data.repository

import com.yourname.womensafety.data.network.api.SupportApiService
import com.yourname.womensafety.data.network.dto.CreateTicketRequest
import com.yourname.womensafety.data.network.dto.FaqItem
import com.yourname.womensafety.data.network.dto.TicketData

class SupportRepository(
    private val supportApi: SupportApiService
) : BaseRepository() {

    suspend fun getFaqs(search: String? = null): NetworkResult<List<FaqItem>> {
        return safeApiCall { supportApi.getFaqs(search.takeIf { !it.isNullOrBlank() }) }
    }

    suspend fun createTicket(subject: String, message: String): NetworkResult<TicketData> {
        return safeApiCall { supportApi.createTicket(CreateTicketRequest(subject, message)) }
    }

    suspend fun getTickets(): NetworkResult<List<TicketData>> {
        return safeApiCall { supportApi.getTickets() }
    }
}
