package com.yourname.womensafety.data.network.api

import com.yourname.womensafety.data.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ContactsApiService {

    @GET("contacts")
    suspend fun getContacts(): Response<ApiResponse<List<TrustedContact>>>

    @POST("contacts")
    suspend fun addContact(
        @Body request: AddContactRequest
    ): Response<ApiResponse<TrustedContact>>

    @PUT("contacts/{id}")
    suspend fun updateContact(
        @Path("id") contactId: String,
        @Body request: UpdateContactRequest
    ): Response<ApiResponse<TrustedContact>>

    @DELETE("contacts/{id}")
    suspend fun deleteContact(
        @Path("id") contactId: String
    ): Response<ApiResponse<Unit>>

    @PUT("contacts/{id}/primary")
    suspend fun setPrimaryContact(
        @Path("id") contactId: String
    ): Response<ApiResponse<TrustedContact>>
}
