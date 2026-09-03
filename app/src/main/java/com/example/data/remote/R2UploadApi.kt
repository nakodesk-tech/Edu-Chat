package com.example.data.remote

import com.example.data.model.R2DownloadUrlRequest
import com.example.data.model.R2DownloadUrlResponse
import com.example.data.model.R2UploadUrlRequest
import com.example.data.model.R2UploadUrlResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface R2UploadApi {
    @POST("functions/v1/r2-create-upload-url")
    @Headers("Content-Type: application/json")
    suspend fun createUploadUrl(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body request: R2UploadUrlRequest
    ): Response<R2UploadUrlResponse>

    @POST
    @Headers("Content-Type: application/json")
    suspend fun createUploadUrlWithCustomUrl(
        @Url customUrl: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body request: R2UploadUrlRequest
    ): Response<R2UploadUrlResponse>

    @POST("functions/v1/r2-get-download-url")
    @Headers("Content-Type: application/json")
    suspend fun getDownloadUrl(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body request: R2DownloadUrlRequest
    ): Response<R2DownloadUrlResponse> = Response.error(
        501,
        okhttp3.ResponseBody.create(null, "Not implemented in default interface stub")
    )
}
