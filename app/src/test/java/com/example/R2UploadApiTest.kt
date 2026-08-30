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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response

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
}
