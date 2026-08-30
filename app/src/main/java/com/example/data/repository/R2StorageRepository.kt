package com.example.data.repository

import android.content.Context
import com.example.data.local.SessionManager
import com.example.data.model.R2BinaryUploadResult
import com.example.data.model.R2UploadUrlRequest
import com.example.data.model.R2UploadUrlResponse
import com.example.data.remote.R2UploadApi
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

class R2StorageRepository(
    private val context: Context,
    private val api: R2UploadApi = SupabaseClient.getR2Api(context),
    private val sessionManager: SessionManager = SessionManager(context),
    private val uploadHttpClient: OkHttpClient = defaultUploadClient
) {
    companion object {
        private val defaultUploadClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
        }
    }

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

    /**
     * Streams binary data to the presigned R2 upload URL via HTTP PUT.
     *
     * @param uploadUrl The presigned upload URL from R2UploadUrlResponse.
     * @param contentType The exact Content-Type specified when generating the presigned URL.
     * @param contentLength Optional content length in bytes if known (improves HTTP chunking/streaming).
     * @param objectKey Optional object key identifier.
     * @param publicUrl Optional public CDN URL for the uploaded object.
     * @param inputStreamProvider Lambda producing an open InputStream (closed automatically after write).
     */
    suspend fun uploadBinaryStream(
        uploadUrl: String,
        contentType: String,
        contentLength: Long = -1L,
        objectKey: String? = null,
        publicUrl: String? = null,
        inputStreamProvider: () -> InputStream
    ): Result<R2BinaryUploadResult> = withContext(Dispatchers.IO) {
        val trimmedUrl = uploadUrl.trim()
        val trimmedContentType = contentType.trim()

        if (trimmedUrl.isBlank() || (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://"))) {
            return@withContext Result.failure(IllegalArgumentException("Invalid or empty upload URL."))
        }
        if (trimmedContentType.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Content-Type cannot be blank."))
        }

        val mediaType = trimmedContentType.toMediaTypeOrNull()
        val requestBody = object : RequestBody() {
            override fun contentType() = mediaType
            override fun contentLength(): Long = contentLength
            override fun writeTo(sink: BufferedSink) {
                val stream = inputStreamProvider()
                try {
                    sink.writeAll(stream.source())
                } finally {
                    try {
                        stream.close()
                    } catch (_: Exception) {}
                }
            }
        }

        val putRequest = Request.Builder()
            .url(trimmedUrl)
            .put(requestBody)
            .header("Content-Type", trimmedContentType)
            .build()

        try {
            uploadHttpClient.newCall(putRequest).execute().use { response ->
                val code = response.code
                if (response.isSuccessful || code in 200..299) {
                    Result.success(
                        R2BinaryUploadResult(
                            isSuccess = true,
                            httpCode = code,
                            objectKey = objectKey,
                            publicUrl = publicUrl
                        )
                    )
                } else {
                    val errorMsg = "R2 upload failed with HTTP $code: ${response.message}"
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Convenience method to upload a local File directly to R2.
     */
    suspend fun uploadBinaryFile(
        uploadUrl: String,
        contentType: String,
        file: File,
        objectKey: String? = null,
        publicUrl: String? = null
    ): Result<R2BinaryUploadResult> {
        if (!file.exists() || !file.canRead()) {
            return Result.failure(IllegalArgumentException("File does not exist or is not readable: ${file.name}"))
        }
        return uploadBinaryStream(
            uploadUrl = uploadUrl,
            contentType = contentType,
            contentLength = file.length(),
            objectKey = objectKey,
            publicUrl = publicUrl,
            inputStreamProvider = { file.inputStream() }
        )
    }

    /**
     * Convenience method to upload a ByteArray directly to R2.
     */
    suspend fun uploadBinaryBytes(
        uploadUrl: String,
        contentType: String,
        bytes: ByteArray,
        objectKey: String? = null,
        publicUrl: String? = null
    ): Result<R2BinaryUploadResult> {
        return uploadBinaryStream(
            uploadUrl = uploadUrl,
            contentType = contentType,
            contentLength = bytes.size.toLong(),
            objectKey = objectKey,
            publicUrl = publicUrl,
            inputStreamProvider = { bytes.inputStream() }
        )
    }
}

