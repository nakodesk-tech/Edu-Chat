package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionManager
import com.example.data.model.AuthSession
import com.example.data.model.R2UploadUrlRequest
import com.example.data.model.R2UploadUrlResponse
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.remote.R2UploadApi
import com.example.data.remote.SupabaseClient
import com.example.data.repository.R2StorageRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response as OkHttpResponse
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class R2UploadApiTest {

    private lateinit var context: Context
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sessionManager = SessionManager(context)
        sessionManager.clearSession()
    }

    private fun setTestSession(token: String = "test_valid_access_token_123") {
        sessionManager.saveSession(
            AuthSession(
                accessToken = token,
                refreshToken = "test_refresh_token_456",
                profile = UserProfile(
                    id = "user-123-uuid",
                    fullName = "Test Teacher",
                    email = "teacher@school.edu",
                    role = "teacher",
                    schoolId = "school-123-uuid"
                )
            )
        )
    }

    // 1. Verify R2UploadUrlRequest serialization contains fileName and contentType
    @Test
    fun testR2UploadUrlRequest_SerializationContainsFileNameAndContentType() {
        val request = R2UploadUrlRequest(
            fileName = "homework_photo.jpg",
            contentType = "image/jpeg"
        )
        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter(R2UploadUrlRequest::class.java)
        val json = adapter.toJson(request)

        assertTrue(json.contains("\"fileName\":\"homework_photo.jpg\""))
        assertTrue(json.contains("\"contentType\":\"image/jpeg\""))

        val deserialized = adapter.fromJson(json)
        assertNotNull(deserialized)
        assertEquals("homework_photo.jpg", deserialized?.fileName)
        assertEquals("image/jpeg", deserialized?.contentType)
    }

    // 2. Verify R2UploadUrlResponse deserialization and effective URL helpers
    @Test
    fun testR2UploadUrlResponse_DeserializationAndUrlHelpers() {
        val jsonResponse = """
            {
                "uploadUrl": "https://r2.cloudflarestorage.com/educhat-bucket/uploads/homework_photo.jpg?X-Amz-Signature=xyz123",
                "publicUrl": "https://cdn.educhat.app/uploads/homework_photo.jpg",
                "key": "uploads/homework_photo.jpg",
                "expiresIn": 3600
            }
        """.trimIndent()

        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter(R2UploadUrlResponse::class.java)
        val response = adapter.fromJson(jsonResponse)

        assertNotNull(response)
        assertEquals("https://r2.cloudflarestorage.com/educhat-bucket/uploads/homework_photo.jpg?X-Amz-Signature=xyz123", response?.effectiveUploadUrl)
        assertEquals("https://cdn.educhat.app/uploads/homework_photo.jpg", response?.effectivePublicUrl)
        assertEquals(3600L, response?.expiresIn)
    }

    // 3. Successful upload URL request
    @Test
    fun testR2StorageRepository_SuccessfulUploadUrlCreation() = runBlocking {
        setTestSession("valid_bearer_token")

        val fakeApi = object : R2UploadApi {
            override suspend fun createUploadUrl(
                apiKey: String,
                bearerToken: String,
                request: R2UploadUrlRequest
            ): Response<R2UploadUrlResponse> {
                assertEquals("Bearer valid_bearer_token", bearerToken)
                assertEquals("assignment_doc.pdf", request.fileName)
                assertEquals("application/pdf", request.contentType)

                val body = R2UploadUrlResponse(
                    uploadUrl = "https://r2.example.com/upload-signed-url",
                    publicUrl = "https://cdn.example.com/assignment_doc.pdf",
                    key = "assignment_doc.pdf"
                )
                return Response.success(body)
            }

            override suspend fun createUploadUrlWithCustomUrl(
                customUrl: String,
                apiKey: String,
                bearerToken: String,
                request: R2UploadUrlRequest
            ): Response<R2UploadUrlResponse> = createUploadUrl(apiKey, bearerToken, request)
        }

        val repository = R2StorageRepository(context, api = fakeApi, sessionManager = sessionManager)
        val result = repository.createUploadUrl(
            fileName = "assignment_doc.pdf",
            contentType = "application/pdf"
        )

        assertTrue("Upload URL creation must succeed", result.isSuccess)
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals("https://r2.example.com/upload-signed-url", response?.effectiveUploadUrl)
        assertEquals("https://cdn.example.com/assignment_doc.pdf", response?.effectivePublicUrl)
    }

    // 4. Unauthorized response (HTTP 401)
    @Test
    fun testR2StorageRepository_UnauthorizedResponse401() = runBlocking {
        setTestSession("expired_token")

        val fakeApi = object : R2UploadApi {
            override suspend fun createUploadUrl(
                apiKey: String,
                bearerToken: String,
                request: R2UploadUrlRequest
            ): Response<R2UploadUrlResponse> {
                val errorBody = "{\"message\":\"Invalid JWT token\",\"error\":\"Unauthorized\"}"
                    .toResponseBody("application/json".toMediaTypeOrNull())
                return Response.error(401, errorBody)
            }

            override suspend fun createUploadUrlWithCustomUrl(
                customUrl: String,
                apiKey: String,
                bearerToken: String,
                request: R2UploadUrlRequest
            ): Response<R2UploadUrlResponse> = createUploadUrl(apiKey, bearerToken, request)
        }

        val repository = R2StorageRepository(context, api = fakeApi, sessionManager = sessionManager)
        val result = repository.createUploadUrl("photo.png", "image/png")

        assertTrue("Should return failure on 401", result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("401") == true || exception?.message?.contains("Unauthorized") == true)
    }

    // 5. Malformed or server error response
    @Test
    fun testR2StorageRepository_ServerError500OrMalformed() = runBlocking {
        setTestSession("valid_token")

        val fakeApi = object : R2UploadApi {
            override suspend fun createUploadUrl(
                apiKey: String,
                bearerToken: String,
                request: R2UploadUrlRequest
            ): Response<R2UploadUrlResponse> {
                val errorBody = "{\"error\":\"R2 bucket connection error\"}"
                    .toResponseBody("application/json".toMediaTypeOrNull())
                return Response.error(500, errorBody)
            }

            override suspend fun createUploadUrlWithCustomUrl(
                customUrl: String,
                apiKey: String,
                bearerToken: String,
                request: R2UploadUrlRequest
            ): Response<R2UploadUrlResponse> = createUploadUrl(apiKey, bearerToken, request)
        }

        val repository = R2StorageRepository(context, api = fakeApi, sessionManager = sessionManager)
        val result = repository.createUploadUrl("notes.pdf", "application/pdf")

        assertTrue("Should fail on server error", result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
    }

    // 6. Validation: Blank filename or contentType must fail immediately
    @Test
    fun testR2StorageRepository_InputValidation() = runBlocking {
        setTestSession("valid_token")

        val repository = R2StorageRepository(context, sessionManager = sessionManager)

        val blankFileResult = repository.createUploadUrl("", "image/jpeg")
        assertTrue(blankFileResult.isFailure)
        assertTrue(blankFileResult.exceptionOrNull() is IllegalArgumentException)

        val blankContentResult = repository.createUploadUrl("image.png", "   ")
        assertTrue(blankContentResult.isFailure)
        assertTrue(blankContentResult.exceptionOrNull() is IllegalArgumentException)
    }

    // 7. Missing session must fail with IllegalStateException
    @Test
    fun testR2StorageRepository_MissingSessionFails() = runBlocking {
        sessionManager.clearSession()

        val repository = R2StorageRepository(context, sessionManager = sessionManager)
        val result = repository.createUploadUrl("photo.png", "image/png")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    // --- R2 STEP 2: BINARY UPLOAD TESTS ---

    // 8. Successful HTTP 200 upload
    @Test
    fun testR2BinaryUpload_Successful200() = runBlocking {
        val testBytes = "mock-image-binary-data-123".toByteArray(Charsets.UTF_8)
        var interceptedMethod = ""
        var interceptedUrl = ""
        var interceptedContentType = ""
        var interceptedAuthHeader: String? = null
        var interceptedApiKeyHeader: String? = null
        var capturedBodyBytes: ByteArray? = null

        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                interceptedMethod = request.method
                interceptedUrl = request.url.toString()
                interceptedContentType = request.header("Content-Type") ?: ""
                interceptedAuthHeader = request.header("Authorization")
                interceptedApiKeyHeader = request.header("apikey")

                val buffer = Buffer()
                request.body?.writeTo(buffer)
                capturedBodyBytes = buffer.readByteArray()

                OkHttpResponse.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("".toResponseBody("text/plain".toMediaTypeOrNull()))
                    .build()
            }
            .build()

        val repository = R2StorageRepository(
            context = context,
            sessionManager = sessionManager,
            uploadHttpClient = mockClient
        )

        val uploadUrl = "https://r2.cloudflarestorage.com/bucket/uploads/test.jpg?X-Amz-Signature=sig123"
        val result = repository.uploadBinaryStream(
            uploadUrl = uploadUrl,
            contentType = "image/jpeg",
            contentLength = testBytes.size.toLong(),
            objectKey = "uploads/test.jpg",
            publicUrl = "https://cdn.example.com/uploads/test.jpg",
            inputStreamProvider = { ByteArrayInputStream(testBytes) }
        )

        assertTrue(result.isSuccess)
        val uploadResult = result.getOrNull()
        assertNotNull(uploadResult)
        assertEquals(true, uploadResult?.isSuccess)
        assertEquals(200, uploadResult?.httpCode)
        assertEquals("uploads/test.jpg", uploadResult?.objectKey)
        assertEquals("https://cdn.example.com/uploads/test.jpg", uploadResult?.publicUrl)

        // Verify request specifics
        assertEquals("PUT", interceptedMethod)
        assertEquals(uploadUrl, interceptedUrl)
        assertEquals("image/jpeg", interceptedContentType)
        assertNull("R2 presigned PUT must not leak Authorization header", interceptedAuthHeader)
        assertNull("R2 presigned PUT must not leak apikey header", interceptedApiKeyHeader)
        assertArrayEquals(testBytes, capturedBodyBytes)
    }

    // 9. Successful HTTP 201 and 204 uploads
    @Test
    fun testR2BinaryUpload_Successful201And204() = runBlocking {
        for (statusCode in listOf(201, 204)) {
            val mockClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    OkHttpResponse.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(statusCode)
                        .message("Success $statusCode")
                        .body("".toResponseBody(null))
                        .build()
                }
                .build()

            val repository = R2StorageRepository(
                context = context,
                sessionManager = sessionManager,
                uploadHttpClient = mockClient
            )

            val result = repository.uploadBinaryBytes(
                uploadUrl = "https://r2.example.com/upload",
                contentType = "image/png",
                bytes = "png-content".toByteArray()
            )

            assertTrue("HTTP $statusCode should be considered successful", result.isSuccess)
            assertEquals(statusCode, result.getOrNull()?.httpCode)
        }
    }

    // 10. HTTP 403 / 500 failure
    @Test
    fun testR2BinaryUpload_HttpFailure() = runBlocking {
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                OkHttpResponse.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(403)
                    .message("Forbidden - Signature Expired")
                    .body("Signature has expired".toResponseBody("text/plain".toMediaTypeOrNull()))
                    .build()
            }
            .build()

        val repository = R2StorageRepository(
            context = context,
            sessionManager = sessionManager,
            uploadHttpClient = mockClient
        )

        val result = repository.uploadBinaryBytes(
            uploadUrl = "https://r2.example.com/upload-expired",
            contentType = "image/webp",
            bytes = "webp-bytes".toByteArray()
        )

        assertTrue("HTTP 403 should return failure", result.isFailure)
        val error = result.exceptionOrNull()
        assertNotNull(error)
        assertTrue(error?.message?.contains("403") == true)
    }

    // 11. Network IO Failure
    @Test
    fun testR2BinaryUpload_NetworkFailure() = runBlocking {
        val mockClient = OkHttpClient.Builder()
            .addInterceptor {
                throw IOException("Connection timed out to R2 endpoint")
            }
            .build()

        val repository = R2StorageRepository(
            context = context,
            sessionManager = sessionManager,
            uploadHttpClient = mockClient
        )

        val result = repository.uploadBinaryBytes(
            uploadUrl = "https://r2.example.com/timeout",
            contentType = "image/jpeg",
            bytes = "image-data".toByteArray()
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    // 12. Invalid or empty upload URL validation
    @Test
    fun testR2BinaryUpload_InvalidUploadUrlValidation() = runBlocking {
        val repository = R2StorageRepository(context, sessionManager = sessionManager)

        val emptyUrlResult = repository.uploadBinaryBytes(
            uploadUrl = "",
            contentType = "image/jpeg",
            bytes = byteArrayOf(1, 2, 3)
        )
        assertTrue(emptyUrlResult.isFailure)
        assertTrue(emptyUrlResult.exceptionOrNull() is IllegalArgumentException)

        val nonHttpUrlResult = repository.uploadBinaryBytes(
            uploadUrl = "ftp://invalid-url",
            contentType = "image/jpeg",
            bytes = byteArrayOf(1, 2, 3)
        )
        assertTrue(nonHttpUrlResult.isFailure)
        assertTrue(nonHttpUrlResult.exceptionOrNull() is IllegalArgumentException)
    }

    // 13. Blank Content-Type validation
    @Test
    fun testR2BinaryUpload_BlankContentTypeValidation() = runBlocking {
        val repository = R2StorageRepository(context, sessionManager = sessionManager)

        val result = repository.uploadBinaryBytes(
            uploadUrl = "https://r2.example.com/upload",
            contentType = "   ",
            bytes = byteArrayOf(1, 2, 3)
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    // 14. Convenience File upload
    @Test
    fun testR2BinaryUpload_FileConvenienceMethod() = runBlocking {
        val tempFile = File.createTempFile("test_r2_upload", ".tmp", context.cacheDir)
        try {
            tempFile.writeText("sample-file-bytes-content")

            var uploadedContent = ""
            val mockClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val buffer = Buffer()
                    chain.request().body?.writeTo(buffer)
                    uploadedContent = buffer.readUtf8()

                    OkHttpResponse.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("".toResponseBody(null))
                        .build()
                }
                .build()

            val repository = R2StorageRepository(
                context = context,
                sessionManager = sessionManager,
                uploadHttpClient = mockClient
            )

            val result = repository.uploadBinaryFile(
                uploadUrl = "https://r2.example.com/upload-file",
                contentType = "image/png",
                file = tempFile,
                objectKey = "file.png"
            )

            assertTrue(result.isSuccess)
            assertEquals("sample-file-bytes-content", uploadedContent)
        } finally {
            tempFile.delete()
        }
    }
}

