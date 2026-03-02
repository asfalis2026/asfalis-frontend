package com.yourname.womensafety.data.network.api

import com.yourname.womensafety.data.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface SupportApiService {

    @GET("support/faq")
    suspend fun getFaqs(
        @Query("search") search: String? = null
    ): Response<ApiResponse<List<FaqItem>>>

    @POST("support/ticket")
    suspend fun createTicket(
        @Body request: CreateTicketRequest
    ): Response<ApiResponse<TicketData>>

    @GET("support/tickets")
    suspend fun getTickets(): Response<ApiResponse<List<TicketData>>>
}
