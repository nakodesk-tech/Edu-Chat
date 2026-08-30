package com.example.data.repository

import android.content.Context
import com.example.data.local.SessionManager
import com.example.data.model.R2UploadUrlRequest
import com.example.data.model.R2UploadUrlResponse
import com.example.data.remote.R2UploadApi
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class R2StorageRepository(
    private val context: Context,
    private val api: R2UploadApi = SupabaseClient.getR2Api(context),
    private val sessionManager: SessionManager = SessionManager(context)
) {
    suspend fun createUploadUrl(
        fileName: String,
        contentType: String
    ): Result<R2UploadUrlResponse> = withContext(Dispatchers.IO) {
        val trimmedFileName = fileName.trim()
        val trimmedContentType = contentType.trim()

        if (trimmedFileName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("File name cannot be blank."))
        }
        if (trimmedContentType.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Content type cannot be blank."))
        }

        val accessToken = sessionManager.getAccessToken()
        if (accessToken.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("No active authenticated session. Please login again."))
        }

        val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
        val request = R2UploadUrlRequest(
            fileName = trimmedFileName,
            contentType = trimmedContentType
        )

        try {
            val response = api.createUploadUrl(
                apiKey = anonKey,
                bearerToken = "Bearer $accessToken",
                request = request
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (!body.error.isNullOrBlank()) {
                    Result.failure(Exception(body.error))
                } else if (body.effectiveUploadUrl.isNullOrBlank()) {
                    Result.failure(Exception("Edge function returned empty upload URL."))
                } else {
                    Result.success(body)
                }
            } else {
                val errorBodyStr = response.errorBody()?.string()
                val parsedError = SupabaseClient.parseError(errorBodyStr)
                val errorMessage = when (response.code()) {
                    401 -> "सत्र कालबाह्य झाले आहे. कृपया पुन्हा लॉगिन करा (401 Unauthorized)."
                    403 -> "या क्रियेसाठी परवानगी नाही (403 Forbidden)."
                    404 -> "R2 Edge Function आढळले नाही (404 Not Found)."
                    else -> parsedError ?: "अपलोड URL तयार करण्यात अयशस्वी (${response.code()} ${response.message()})."
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
