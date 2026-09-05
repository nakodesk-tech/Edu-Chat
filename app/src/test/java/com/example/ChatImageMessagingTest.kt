package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionManager
import com.example.data.model.AuthSession
import com.example.data.model.ChatMessage
import com.example.data.model.GroupType
import com.example.data.model.R2UploadUrlRequest
import com.example.data.model.R2UploadUrlResponse
import com.example.data.model.School
import com.example.data.model.SendGroupMessageRequest
import com.example.data.model.UserProfile
import com.example.data.remote.R2UploadApi
import com.example.data.repository.GroupRepository
import com.example.data.repository.R2ImageUploadManager
import com.example.data.repository.R2StorageRepository
import com.example.ui.chat.ChatGroupViewModel
import com.example.ui.chat.ChatImageUploadState
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
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
import org.robolectric.annotation.Config
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChatImageMessagingTest {

    private lateinit var application: Application
    private lateinit var context: Context
    private lateinit var sessionManager: SessionManager
    private lateinit var fakeDatabaseEngine: FakeSupabaseDatabaseEngine
    private lateinit var groupRepository: GroupRepository

    private val schoolId = "s0000000-0001-4000-8000-000000000001"
    private val school = School(
        id = schoolId,
        name = "Model High School",
        code = "SCH001",
        isActive = true
    )

    private val teacherUser = UserProfile(
        id = "teacher-img-01",
        fullName = "Sanjay Pawar",
        email = "sanjay.pawar@school.edu",
        mobile = "9812345678",
        role = "teacher",
        schoolId = schoolId,
        isActive = true
    )

    private val studentUser = UserProfile(
        id = "student-img-01",
        fullName = "Aniket Kale",
        email = "aniket.kale@school.edu",
        mobile = "9812345679",
        role = "student",
        schoolId = schoolId,
        isActive = true
    )

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        context = application
        sessionManager = SessionManager(context)
        sessionManager.clearSession()

        fakeDatabaseEngine = FakeSupabaseDatabaseEngine()
        fakeDatabaseEngine.addSchool(school)
        fakeDatabaseEngine.addProfile(teacherUser)
        fakeDatabaseEngine.addProfile(studentUser)

        groupRepository = GroupRepository(context, sessionManager, fakeDatabaseEngine)

        sessionManager.saveSession(
            AuthSession(
                accessToken = teacherUser.id,
                refreshToken = "mock-refresh",
                profile = teacherUser
            )
        )
    }

    private fun createSampleImageUri(): Uri {
        val testFile = File(context.cacheDir, "test_chat_image.jpg")
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        FileOutputStream(testFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return Uri.fromFile(testFile)
    }

    private fun createFakeR2Repo(isSuccess: Boolean): R2StorageRepository {
        val fakeApi = object : R2UploadApi {
            override suspend fun createUploadUrl(
                apiKey: String,
                bearerToken: String,
                request: R2UploadUrlRequest
            ): Response<R2UploadUrlResponse> {
                return if (isSuccess) {
                    Response.success(
                        R2UploadUrlResponse(
                            uploadUrl = "https://r2.cloudflare.com/bucket/uploads/${request.fileName}?sign=123",
                            publicUrl = "https://pub-r2.educhat.edu/uploads/${request.fileName}",
                            key = "uploads/${request.fileName}"
                        )
                    )
                } else {
                    Response.error(500, "Edge function error".toResponseBody(null))
                }
            }

            override suspend fun createUploadUrlWithCustomUrl(
                customUrl: String,
                apiKey: String,
                bearerToken: String,
                request: R2UploadUrlRequest
            ): Response<R2UploadUrlResponse> = createUploadUrl(apiKey, bearerToken, request)

            override suspend fun getDownloadUrl(
                apiKey: String,
                bearerToken: String,
                request: com.example.data.model.R2DownloadUrlRequest
            ): Response<com.example.data.model.R2DownloadUrlResponse> {
                return if (isSuccess) {
                    Response.success(
                        com.example.data.model.R2DownloadUrlResponse(
                            downloadUrl = "https://pub-r2.educhat.edu/${request.objectKey}",
                            objectKey = request.objectKey,
                            expiresIn = 3600
                        )
                    )
                } else {
                    Response.error(500, "Download URL error".toResponseBody(null))
                }
            }
        }

        val fakeClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                if (isSuccess) {
                    OkHttpResponse.Builder()
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("".toResponseBody(null))
                        .build()
                } else {
                    OkHttpResponse.Builder()
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .code(500)
                        .message("Upload Failed")
                        .body("Upload Failed".toResponseBody(null))
                        .build()
                }
            }
            .build()

        return R2StorageRepository(
            context = context,
            api = fakeApi,
            sessionManager = sessionManager,
            uploadHttpClient = fakeClient
        )
    }

    @Test
    fun testChatMessageMoshiSerializationWithImageFields() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(ChatMessage::class.java)

        val json = """
            {
                "id": "msg-123",
                "group_id": "grp-456",
                "sender_id": "user-789",
                "content": "Check this image",
                "message_type": "image",
                "media_url": "https://pub-r2.educhat.edu/groups/grp-456/doc.jpg",
                "created_at": "2026-08-31T10:00:00Z",
                "is_deleted": false
            }
        """.trimIndent()

        val msg = adapter.fromJson(json)
        assertNotNull(msg)
        assertEquals("msg-123", msg?.id)
        assertEquals("image", msg?.messageType)
        assertEquals("https://pub-r2.educhat.edu/groups/grp-456/doc.jpg", msg?.mediaUrl)
        assertTrue(msg?.isImageMessage == true)
        assertEquals("Check this image", msg?.content)
    }

    @Test
    fun testChatMessageMoshiDefaultsForLegacyTextMessage() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(ChatMessage::class.java)

        val json = """
            {
                "id": "msg-legacy",
                "group_id": "grp-456",
                "sender_id": "user-789",
                "content": "Hello legacy text",
                "created_at": "2026-08-31T10:00:00Z"
            }
        """.trimIndent()

        val msg = adapter.fromJson(json)
        assertNotNull(msg)
        assertEquals("text", msg?.messageType)
        assertNull(msg?.mediaUrl)
        assertFalse(msg?.isImageMessage == true)
    }

    @Test
    fun testSendGroupMessageRequestMoshiSerialization() {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(SendGroupMessageRequest::class.java)

        val req = SendGroupMessageRequest(
            groupId = "grp-001",
            content = "diagram caption",
            messageType = "image",
            mediaUrl = "https://pub-r2.educhat.edu/diagram.png"
        )
        val json = adapter.toJson(req)
        assertTrue(json.contains(""""group_id":"grp-001""""))
        assertTrue(json.contains(""""message_type":"image""""))
        assertTrue(json.contains(""""media_url":"https://pub-r2.educhat.edu/diagram.png""""))
    }

    @Test
    fun testDirectRepositorySendImageMessage() = runBlocking {
        val group = groupRepository.createGroup("10th Physics", GroupType.TEACHER.name).getOrNull()
        assertNotNull(group)

        val uploadUrl = "https://pub-r2.educhat.edu/uploads/photo.jpg"
        val sendRes = groupRepository.sendGroupMessage(
            groupId = group!!.id,
            content = "Classroom diagram",
            messageType = "image",
            mediaUrl = uploadUrl
        )
        assertTrue(sendRes.isSuccess)
        val msg = sendRes.getOrNull()
        assertNotNull(msg)
        assertEquals("image", msg?.messageType)
        assertEquals(uploadUrl, msg?.mediaUrl)
        assertTrue(msg?.isImageMessage == true)

        val messages = groupRepository.getGroupMessages(group.id).getOrThrow()
        assertEquals(1, messages.size)
        assertEquals("image", messages[0].messageType)
        assertEquals(uploadUrl, messages[0].mediaUrl)
    }

    @Test
    fun testUploadManagerEndToEndImageUpload() = runBlocking {
        val r2Repo = createFakeR2Repo(isSuccess = true)
        val uploadManager = R2ImageUploadManager(context, r2Repo)
        val imageUri = createSampleImageUri()

        val uploadResult = uploadManager.uploadImageFromUri(imageUri)
        assertTrue(uploadResult.isSuccess)
        val r2File = uploadResult.getOrNull()
        assertNotNull(r2File)
        assertTrue(r2File?.publicUrl?.startsWith("https://pub-r2.educhat.edu/uploads/") == true)
    }

    @Test
    fun testUploadManagerHandlesFailureGracefully() = runBlocking {
        val r2Repo = createFakeR2Repo(isSuccess = false)
        val uploadManager = R2ImageUploadManager(context, r2Repo)
        val imageUri = createSampleImageUri()

        val uploadResult = uploadManager.uploadImageFromUri(imageUri)
        assertFalse(uploadResult.isSuccess)
    }

    @Test
    fun testChatGroupViewModelAcceptsExplicitRepositoryTestDouble() = runBlocking {
        val r2Repo = createFakeR2Repo(isSuccess = true)
        val uploadManager = R2ImageUploadManager(context, r2Repo)
        val viewModel = ChatGroupViewModel(
            application = application,
            groupRepo = groupRepository,
            r2UploadManager = uploadManager,
            sessionManager = sessionManager
        )
        assertNotNull(viewModel)
        assertNotNull(viewModel.uiState.value)
        assertEquals(teacherUser.id, viewModel.uiState.value.currentProfile?.id)
    }

    @Test
    fun testChatGroupViewModel_SingleArgConstructorInstantiation() {
        val constructor = ChatGroupViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull("ChatGroupViewModel must have a single Application constructor", constructor)
        val viewModel = constructor.newInstance(application)
        assertNotNull(viewModel)
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun testOfficerAdminGroupChat_SendImage_R2Upload_StoreMediaRef_ResolveMediaUrl() = runBlocking {
        val officerUser = UserProfile(
            id = "officer-admin-uuid-1",
            fullName = "Shri Rajesh Patil",
            email = "rajesh.patil@education.gov.in",
            mobile = "9823001122",
            role = "officer_admin",
            schoolId = null,
            isActive = true
        )
        fakeDatabaseEngine.addProfile(officerUser)

        sessionManager.saveSession(
            AuthSession(
                accessToken = officerUser.id,
                refreshToken = "officer-refresh-token",
                profile = officerUser
            )
        )

        val group = groupRepository.createGroup(
            name = "District Education Officers",
            groupType = GroupType.ADMINISTRATIVE.dbValue
        ).getOrNull()
        assertNotNull(group)

        val r2Repo = createFakeR2Repo(isSuccess = true)
        val uploadManager = R2ImageUploadManager(context, r2Repo)
        val viewModel = ChatGroupViewModel(
            application = application,
            groupRepo = groupRepository,
            r2UploadManager = uploadManager,
            sessionManager = sessionManager
        )

        assertEquals(officerUser.id, viewModel.uiState.value.currentProfile?.id)

        val sampleUri = createSampleImageUri()
        val uploadResult = uploadManager.uploadImageFromUri(
            uri = sampleUri,
            groupId = group!!.id,
            customFileName = "circular_notice.jpg"
        )
        assertTrue("Upload must succeed", uploadResult.isSuccess)
        val binaryResult = uploadResult.getOrNull()
        assertNotNull(binaryResult)
        assertNotNull(binaryResult?.objectKey)
        val expectedObjectKey = binaryResult!!.objectKey!!
        assertTrue(expectedObjectKey.isNotEmpty())

        val sendRes = groupRepository.sendGroupMessage(
            groupId = group.id,
            content = "कृपया संलग्न परिपत्रक तपासावे.",
            messageType = "image",
            mediaUrl = expectedObjectKey
        )
        assertTrue("Image message must send successfully", sendRes.isSuccess)
        val sentMsg = sendRes.getOrNull()
        assertNotNull(sentMsg)
        assertEquals("image", sentMsg?.messageType)
        assertEquals(expectedObjectKey, sentMsg?.mediaUrl)
        assertTrue(sentMsg?.isImageMessage == true)
        assertEquals("कृपया संलग्न परिपत्रक तपासावे.", sentMsg?.content)

        val fetchedMessages = groupRepository.getGroupMessages(group.id).getOrThrow()
        assertEquals(1, fetchedMessages.size)
        val storedMsg = fetchedMessages[0]
        assertEquals(expectedObjectKey, storedMsg.mediaUrl)
        assertEquals("image", storedMsg.messageType)
        assertTrue(storedMsg.isImageMessage)

        val resolvedDirect = viewModel.resolveMediaUrl(group.id, "https://legacy.educhat.edu/img/photo.png")
        assertEquals("https://legacy.educhat.edu/img/photo.png", resolvedDirect)

        val resolvedSignedUrl = viewModel.resolveMediaUrl(group.id, storedMsg.mediaUrl!!)
        assertNotNull(resolvedSignedUrl)
        assertTrue(resolvedSignedUrl!!.contains(expectedObjectKey))
    }

    @Test
    fun testOfficerAdminCanSendImageToExistingGroupWithoutExplicitMembership() = runBlocking {
        // 1. Teacher creates an active teacher group
        sessionManager.saveSession(
            AuthSession(
                accessToken = teacherUser.id,
                refreshToken = "teacher-refresh-token",
                profile = teacherUser
            )
        )
        val createResult = groupRepository.createGroup(
            name = "Grade 10 Science",
            groupType = GroupType.TEACHER.dbValue
        )
        val classGroup = createResult.getOrThrow()
        assertNotNull(classGroup)

        // 2. Officer Admin logs in (Officer Admin has no explicit row in groupMembers)
        val officerUser = UserProfile(
            id = "officer-admin-uuid-2",
            fullName = "Shri Suresh Jadhav",
            email = "suresh.jadhav@education.gov.in",
            mobile = "9823005566",
            role = "officer_admin",
            schoolId = null,
            isActive = true
        )
        fakeDatabaseEngine.addProfile(officerUser)
        sessionManager.saveSession(
            AuthSession(
                accessToken = officerUser.id,
                refreshToken = "officer-refresh-token-2",
                profile = officerUser
            )
        )

        val r2Repo = createFakeR2Repo(isSuccess = true)
        val uploadManager = R2ImageUploadManager(context, r2Repo)
        val viewModel = ChatGroupViewModel(
            application = application,
            groupRepo = groupRepository,
            r2UploadManager = uploadManager,
            sessionManager = sessionManager
        )

        // Verify Officer Admin can access group details
        val groupDetailsRes = groupRepository.getGroupDetails(classGroup.id)
        assertTrue("Officer Admin must be able to view group details", groupDetailsRes.isSuccess)

        // 3. Officer Admin uploads image to R2 for the class group
        val sampleUri = createSampleImageUri()
        val uploadResult = uploadManager.uploadImageFromUri(
            uri = sampleUri,
            groupId = classGroup.id,
            customFileName = "inspection_report.jpg"
        )
        assertTrue("Officer Admin R2 upload must succeed", uploadResult.isSuccess)
        val uploadedKey = uploadResult.getOrThrow().objectKey
        assertNotNull(uploadedKey)

        // 4. Officer Admin sends the image message
        val sendRes = groupRepository.sendGroupMessage(
            groupId = classGroup.id,
            content = "वार्षिक तपासणी अहवाल जोडला आहे.",
            messageType = "image",
            mediaUrl = uploadedKey
        )
        assertTrue("Officer Admin image message send must succeed", sendRes.isSuccess)
        val sent = sendRes.getOrThrow()
        assertEquals("image", sent.messageType)
        assertEquals(uploadedKey, sent.mediaUrl)

        // 5. Verify messages in group include Officer Admin's image message
        val messages = groupRepository.getGroupMessages(classGroup.id).getOrThrow()
        assertTrue(messages.any { it.id == sent.id && it.mediaUrl == uploadedKey && it.isImageMessage })
    }
}
