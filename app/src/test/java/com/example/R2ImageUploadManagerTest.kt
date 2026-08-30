package com.example

import android.content.Context
import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionManager
import com.example.data.model.AuthSession
import com.example.data.model.R2UploadUrlRequest
import com.example.data.model.R2UploadUrlResponse
import com.example.data.model.UserProfile
import com.example.data.remote.R2UploadApi
import com.example.data.repository.R2ImageUploadManager
import com.example.data.repository.R2StorageRepository
import com.example.util.ImagePreparationHelper
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response as OkHttpResponse
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowMimeTypeMap
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class R2ImageUploadManagerTest {

    private lateinit var context: Context
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sessionManager = SessionManager(context)
        sessionManager.clearSession()
        setTestSession()
    }

    private fun setTestSession() {
        sessionManager.saveSession(
            AuthSession(
                accessToken = "valid_supabase_user_token_abc",
                refreshToken = "valid_refresh_token_xyz",
                profile = UserProfile(
                    id = "teacher-uuid-1",
                    fullName = "Teacher Namrata",
                    email = "teacher@school.org",
                    role = "teacher",
                    schoolId = "school-uuid-1"
                )
            )
        )
    }

    private fun createSampleImageFile(
        format: Bitmap.CompressFormat,
        width: Int = 400,
        height: Int = 300,
        fileName: String = "test_image"
    ): File {
        val extension = when (format) {
            Bitmap.CompressFormat.PNG -> ".png"
            Bitmap.CompressFormat.WEBP -> ".webp"
            else -> ".jpg"
        }
        val file = File(context.cacheDir, "$fileName$extension")
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        FileOutputStream(file).use { out ->
            bitmap.compress(format, 90, out)
            out.flush()
        }
        bitmap.recycle()
        return file
    }

    // 1. Valid JPEG preparation & validation
    @Test
    fun testImagePreparation_ValidJpeg() = runBlocking {
        val file = createSampleImageFile(Bitmap.CompressFormat.JPEG, 800, 600, "sample_jpeg")
        val uri = Uri.fromFile(file)

        val validationResult = ImagePreparationHelper.validateImage(context, uri)
        assertTrue("JPEG validation must succeed", validationResult.isSuccess)
        assertEquals("image/jpeg", validationResult.getOrNull())

        val prepResult = ImagePreparationHelper.prepareImage(context, uri)
        assertTrue(prepResult.isSuccess)
        val prepared = prepResult.getOrNull()
        assertNotNull(prepared)
        assertEquals("image/jpeg", prepared?.contentType)
        assertTrue(prepared!!.file.exists())
        assertTrue(prepared.fileSizeBytes > 0)
        assertTrue(prepared.width > 0 && prepared.height > 0)
    }

    // 2. Valid PNG preparation & validation
    @Test
    fun testImagePreparation_ValidPng() = runBlocking {
        val file = createSampleImageFile(Bitmap.CompressFormat.PNG, 500, 500, "sample_png")
        val uri = Uri.fromFile(file)

        val validationResult = ImagePreparationHelper.validateImage(context, uri)
        assertTrue("PNG validation must succeed", validationResult.isSuccess)
        assertEquals("image/png", validationResult.getOrNull())

        val prepResult = ImagePreparationHelper.prepareImage(context, uri)
        assertTrue(prepResult.isSuccess)
        val prepared = prepResult.getOrNull()
        assertNotNull(prepared)
        assertEquals("image/png", prepared?.contentType)
    }

    // 3. Valid WebP preparation & validation
    @Test
    fun testImagePreparation_ValidWebp() = runBlocking {
        val file = createSampleImageFile(Bitmap.CompressFormat.WEBP, 600, 400, "sample_webp")
        val uri = Uri.fromFile(file)

        val validationResult = ImagePreparationHelper.validateImage(context, uri)
        assertTrue("WebP validation must succeed", validationResult.isSuccess)
        assertEquals("image/webp", validationResult.getOrNull())

        val prepResult = ImagePreparationHelper.prepareImage(context, uri)
        assertTrue(prepResult.isSuccess)
        val prepared = prepResult.getOrNull()
        assertNotNull(prepared)
        assertEquals("image/webp", prepared?.contentType)
    }

    // 4. Unsupported MIME type (e.g. text/plain or non-image)
    @Test
    fun testImagePreparation_UnsupportedMimeType() = runBlocking {
        val textFile = File(context.cacheDir, "sample_doc.txt")
        textFile.writeText("This is plain text, not an image.")
        val uri = Uri.fromFile(textFile)

        val validationResult = ImagePreparationHelper.validateImage(context, uri)
        assertTrue("Non-image file must be rejected", validationResult.isFailure)
        assertTrue(validationResult.exceptionOrNull()?.message?.contains("Unsupported") == true)
    }

    // 5. Unreadable or non-existent Uri
    @Test
    fun testImagePreparation_UnreadableUri() = runBlocking {
        val missingUri = Uri.fromFile(File(context.cacheDir, "non_existent_file_xyz.jpg"))

        val validationResult = ImagePreparationHelper.validateImage(context, missingUri)
        assertTrue("Non-existent Uri must fail validation", validationResult.isFailure)
    }

    // 6. Oversized image scaling / inSampleSize calculation
    @Test
    fun testImagePreparation_ScalingCalculations() {
        val sampleSize = ImagePreparationHelper.calculateInSampleSize(
            width = 4000,
            height = 3000,
            reqWidth = 1920,
            reqHeight = 1920
        )
        assertTrue("Sample size should downsample oversized dimensions", sampleSize >= 2)
    }

    // 7. Full successful pipeline: Uri -> Preparation -> Presigned URL -> R2 HTTP PUT
    @Test
    fun testR2ImageUploadManager_FullPipelineSuccess() = runBlocking {
        val file = createSampleImageFile(Bitmap.CompressFormat.JPEG, 1200, 900, "homework_full")
        val uri = Uri.fromFile(file)

        var edgeFunctionCalled = false
        var r2BinaryPutCalled = false
        var leakedAuthHeader: String? = null

        val fakeApi = object : R2UploadApi {
            override suspend fun createUploadUrl(
                apiKey: String,
                bearerToken: String,
                request: R2UploadUrlRequest
            ): Response<R2UploadUrlResponse> {
                edgeFunctionCalled = true
                assertEquals("image/jpeg", request.contentType)
                return Response.success(
                    R2UploadUrlResponse(
                        uploadUrl = "https://r2.cloudflarestorage.com/bucket/uploads/${request.fileName}?X-Amz-Signature=mock",
                        publicUrl = "https://cdn.educhat.org/uploads/${request.fileName}",
                        key = "uploads/${request.fileName}"
                    )
                )
            }

            override suspend fun createUploadUrlWithCustomUrl(
                customUrl: String,
                apiKey: String,
                bearerToken: String,
                request: R2UploadUrlRequest
            ): Response<R2UploadUrlResponse> = createUploadUrl(apiKey, bearerToken, request)
        }

        val mockOkHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                if (req.method == "PUT") {
                    r2BinaryPutCalled = true
                    leakedAuthHeader = req.header("Authorization")
                }
                OkHttpResponse.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("".toResponseBody(null))
                    .build()
            }
            .build()

        val r2Repo = R2StorageRepository(
            context = context,
            api = fakeApi,
            sessionManager = sessionManager,
            uploadHttpClient = mockOkHttpClient
        )

        val uploadManager = R2ImageUploadManager(context, r2StorageRepository = r2Repo)
        val result = uploadManager.uploadImageFromUri(uri, customFileName = "test_custom_name")

        assertTrue("Upload pipeline must succeed", result.isSuccess)
        val uploadResult = result.getOrNull()
        assertNotNull(uploadResult)
        assertEquals(true, uploadResult?.isSuccess)
        assertEquals("image/jpeg", uploadResult?.contentType)
        assertTrue(uploadResult?.publicUrl?.contains("test_custom_name.jpg") == true)
        assertTrue(uploadResult?.objectKey?.contains("test_custom_name.jpg") == true)
        assertTrue(uploadResult!!.width > 0)
        assertTrue(uploadResult.height > 0)

        assertTrue("Edge function must be called", edgeFunctionCalled)
        assertTrue("R2 binary PUT must be called", r2BinaryPutCalled)
        assertNull("R2 presigned upload must never receive Supabase auth header", leakedAuthHeader)
    }

    // 8. Edge Function Failure Handling
    @Test
    fun testR2ImageUploadManager_EdgeFunctionFailure() = runBlocking {
        val file = createSampleImageFile(Bitmap.CompressFormat.PNG, 400, 300, "fail_edge")
        val uri = Uri.fromFile(file)

        val fakeApi = object : R2UploadApi {
            override suspend fun createUploadUrl(
                apiKey: String,
                bearerToken: String,
                request: R2UploadUrlRequest
            ): Response<R2UploadUrlResponse> {
                val errorBody = "{\"error\":\"Unauthorized or invalid session\"}"
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

        val r2Repo = R2StorageRepository(context, api = fakeApi, sessionManager = sessionManager)
        val uploadManager = R2ImageUploadManager(context, r2StorageRepository = r2Repo)

        val result = uploadManager.uploadImageFromUri(uri)
        assertTrue("Must fail gracefully on Edge Function error", result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("401") == true || result.exceptionOrNull()?.message?.contains("सत्र") == true)
    }

    // 9. R2 Binary Upload Network Failure
    @Test
    fun testR2ImageUploadManager_BinaryUploadFailure() = runBlocking {
        val file = createSampleImageFile(Bitmap.CompressFormat.WEBP, 400, 300, "fail_r2")
        val uri = Uri.fromFile(file)

        val fakeApi = object : R2UploadApi {
            override suspend fun createUploadUrl(
                apiKey: String,
                bearerToken: String,
                request: R2UploadUrlRequest
            ): Response<R2UploadUrlResponse> {
                return Response.success(
                    R2UploadUrlResponse(
                        uploadUrl = "https://r2.example.com/upload-fail",
                        publicUrl = "https://cdn.example.com/fail.webp"
                    )
                )
            }

            override suspend fun createUploadUrlWithCustomUrl(
                customUrl: String,
                apiKey: String,
                bearerToken: String,
                request: R2UploadUrlRequest
            ): Response<R2UploadUrlResponse> = createUploadUrl(apiKey, bearerToken, request)
        }

        val failingClient = OkHttpClient.Builder()
            .addInterceptor {
                throw IOException("Connection dropped while uploading to R2")
            }
            .build()

        val r2Repo = R2StorageRepository(
            context = context,
            api = fakeApi,
            sessionManager = sessionManager,
            uploadHttpClient = failingClient
        )
        val uploadManager = R2ImageUploadManager(context, r2StorageRepository = r2Repo)

        val result = uploadManager.uploadImageFromUri(uri)
        assertTrue("Must fail on network disconnection", result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    // 10. Retry behavior produces new presigned URL
    @Test
    fun testR2ImageUploadManager_RetryAcquiresFreshPresignedUrl() = runBlocking {
        val file = createSampleImageFile(Bitmap.CompressFormat.JPEG, 500, 400, "retry_image")
        val uri = Uri.fromFile(file)

        var requestCount = 0
        val fakeApi = object : R2UploadApi {
            override suspend fun createUploadUrl(
                apiKey: String,
                bearerToken: String,
                request: R2UploadUrlRequest
            ): Response<R2UploadUrlResponse> {
                requestCount++
                return if (requestCount == 1) {
                    Response.error(500, "{\"error\":\"Temporary server busy\"}".toResponseBody("application/json".toMediaTypeOrNull()))
                } else {
                    Response.success(
                        R2UploadUrlResponse(
                            uploadUrl = "https://r2.example.com/fresh-signed-url",
                            publicUrl = "https://cdn.example.com/retry.jpg"
                        )
                    )
                }
            }

            override suspend fun createUploadUrlWithCustomUrl(
                customUrl: String,
                apiKey: String,
                bearerToken: String,
                request: R2UploadUrlRequest
            ): Response<R2UploadUrlResponse> = createUploadUrl(apiKey, bearerToken, request)
        }

        val mockOkHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                OkHttpResponse.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("".toResponseBody(null))
                    .build()
            }
            .build()

        val r2Repo = R2StorageRepository(
            context = context,
            api = fakeApi,
            sessionManager = sessionManager,
            uploadHttpClient = mockOkHttpClient
        )
        val uploadManager = R2ImageUploadManager(context, r2StorageRepository = r2Repo)

        // Attempt 1: Fails
        val firstAttempt = uploadManager.uploadImageFromUri(uri)
        assertTrue(firstAttempt.isFailure)
        assertEquals(1, requestCount)

        // Attempt 2 (Retry): Succeeds and calls API again for fresh signed URL
        val secondAttempt = uploadManager.uploadImageFromUri(uri)
        assertTrue(secondAttempt.isSuccess)
        assertEquals(2, requestCount)
        assertEquals("https://cdn.example.com/retry.jpg", secondAttempt.getOrNull()?.publicUrl)
    }
}
