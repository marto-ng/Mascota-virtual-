package com.example.network

import com.example.data.PetState
import com.example.data.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Url

interface MochiSyncService {
    @PUT
    suspend fun uploadPetState(
        @Url url: String,
        @Body state: PetState
    ): Response<PetState>

    @GET
    suspend fun downloadPetState(
        @Url url: String
    ): Response<PetState>

    @PUT
    suspend fun uploadUser(
        @Url url: String,
        @Body user: User
    ): Response<User>

    @GET
    suspend fun downloadUser(
        @Url url: String
    ): Response<User>

    @GET
    suspend fun downloadAllUsers(
        @Url url: String
    ): Response<Map<String, User>>
}
