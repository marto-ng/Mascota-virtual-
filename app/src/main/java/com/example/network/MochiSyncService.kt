package com.example.network

import com.example.data.PetState
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Url

interface MochiSyncService {
    @POST
    suspend fun uploadPetState(
        @Url url: String,
        @Body state: PetState
    ): Response<Map<String, Any>>

    @GET
    suspend fun downloadPetState(
        @Url url: String
    ): Response<PetState>
}
