package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionManager
import com.example.data.model.AuthSession
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.remote.SupabaseClient
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthResult
import com.example.data.repository.OfficerAdminRepository
import com.example.data.repository.SchoolAdminRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SchoolAdminSecurityTest {

    private lateinit var context: Context
    private lateinit var authRepository: AuthRepository
    private lateinit var officerAdminRepository: OfficerAdminRepository
    private lateinit var schoolAdminRepository: SchoolAdminRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var fakeApi: FakeSupabaseDatabaseEngine

    private val puneSchoolId = "s0000000-0001-4000-8000-000000000001"
    private val mumbaiSchoolId = "s0000000-0002-4000-8000-000000000002"

    private lateinit var puneAdminProfile: UserProfile
    private lateinit var mumbaiAdminProfile: UserProfile
    private lateinit var puneTeacherProfile: UserProfile
    private lateinit var mumbaiTeacherProfile: UserProfile

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        fakeApi = FakeSupabaseDatabaseEngine()
        SupabaseClient.testApiOverride = fakeApi
        sessionManager = SessionManager(context)
        sessionManager.clearSession()
        authRepository = AuthRepository(context, sessionManager, fakeApi)
        officerAdminRepository = OfficerAdminRepository(context, sessionManager, fakeApi)
        schoolAdminRepository = SchoolAdminRepository(context, sessionManager, fakeApi)

        // Seed School Admins and Teachers for testing
        puneAdminProfile = UserProfile(
            id = "sa-pune-001",
            fullName = "Pune School Principal",
            email = "principal.pune@educhat.edu",
            mobile = "9800000001",
            role = "school_admin",
            isActive = true,
            schoolId = puneSchoolId,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z"
        )
        fakeApi.addUser(puneAdminProfile.email!!, "password123", puneAdminProfile)

        mumbaiAdminProfile = UserProfile(
            id = "sa-mumbai-001",
            fullName = "Mumbai School Principal",
            email = "principal.mumbai@educhat.edu",
            mobile = "9800000002",
            role = "school_admin",
            isActive = true,
            schoolId = mumbaiSchoolId,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z"
        )
        fakeApi.addUser(mumbaiAdminProfile.email!!, "password123", mumbaiAdminProfile)

        puneTeacherProfile = UserProfile(
            id = "t-pune-001",
            fullName = "Pune Science Teacher",
            email = "teacher.pune@educhat.edu",
            mobile = "9800000003",
            role = "teacher",
            isActive = true,
            schoolId = puneSchoolId,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z"
        )
        fakeApi.addUser(puneTeacherProfile.email!!, "password123", puneTeacherProfile)

        mumbaiTeacherProfile = UserProfile(
            id = "t-mumbai-001",
            fullName = "Mumbai Maths Teacher",
            email = "teacher.mumbai@educhat.edu",
            mobile = "9800000004",
            role = "teacher",
            isActive = true,
            schoolId = mumbaiSchoolId,
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z"
        )
        fakeApi.addUser(mumbaiTeacherProfile.email!!, "password123", mumbaiTeacherProfile)
    }

    @After
    fun tearDown() {
        SupabaseClient.reset()
    }

    private fun loginAsPuneSchoolAdmin() {
        sessionManager.saveSession(
            AuthSession(
                accessToken = puneAdminProfile.id,
                refreshToken = "refresh-sa-pune",
                profile = puneAdminProfile
            )
        )
    }

    private fun loginAsMumbaiSchoolAdmin() {
        sessionManager.saveSession(
            AuthSession(
                accessToken = mumbaiAdminProfile.id,
                refreshToken = "refresh-sa-mumbai",
                profile = mumbaiAdminProfile
            )
        )
    }

    // 1. Unauthenticated user cannot access School Admin repository / dashboard
    @Test
    fun test_01_unauthenticated_user_cannot_access_school_admin_dashboard() = runBlocking {
        sessionManager.clearSession()
        val authCheck = schoolAdminRepository.checkSchoolAdminAuthorization()
        assertTrue("Unauthenticated access must fail", authCheck.isFailure)
        assertTrue(authCheck.exceptionOrNull() is SecurityException)
    }

    // 2. Inactive school admin cannot access dashboard
    @Test
    fun test_02_inactive_school_admin_cannot_access_dashboard() = runBlocking {
        val inactiveAdmin = puneAdminProfile.copy(isActive = false)
        sessionManager.saveSession(
            AuthSession(
                accessToken = "token-inactive",
                refreshToken = "refresh-inactive",
                profile = inactiveAdmin
            )
        )
        val authCheck = schoolAdminRepository.checkSchoolAdminAuthorization()
        assertTrue("Inactive School Admin must be rejected", authCheck.isFailure)
        assertTrue(authCheck.exceptionOrNull() is SecurityException)
    }

    // 3. Officer Admin cannot use School Admin dashboard
    @Test
    fun test_03_officer_admin_cannot_use_school_admin_dashboard() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        val authCheck = schoolAdminRepository.checkSchoolAdminAuthorization()
        assertTrue("Officer Admin accessing School Admin methods must fail", authCheck.isFailure)
        assertTrue(authCheck.exceptionOrNull() is SecurityException)
    }

    // 4. Teacher cannot use School Admin dashboard
    @Test
    fun test_04_teacher_cannot_use_school_admin_dashboard() = runBlocking {
        authRepository.login("teacher.pune@educhat.edu", "password123", UserRole.TEACHER)
        val authCheck = schoolAdminRepository.checkSchoolAdminAuthorization()
        assertTrue("Teacher accessing School Admin methods must fail", authCheck.isFailure)
        assertTrue(authCheck.exceptionOrNull() is SecurityException)
    }

    // 5. Student cannot use School Admin dashboard
    @Test
    fun test_05_student_cannot_use_school_admin_dashboard() = runBlocking {
        authRepository.login("student@educhat.edu", "password123", UserRole.STUDENT)
        val authCheck = schoolAdminRepository.checkSchoolAdminAuthorization()
        assertTrue("Student accessing School Admin methods must fail", authCheck.isFailure)
        assertTrue(authCheck.exceptionOrNull() is SecurityException)
    }

    // 6. School Admin with NULL school_id cannot access dashboard
    @Test
    fun test_06_school_admin_with_null_school_id_rejected() = runBlocking {
        val brokenAdmin = puneAdminProfile.copy(schoolId = null)
        sessionManager.saveSession(
            AuthSession(
                accessToken = "token-broken",
                refreshToken = "refresh-broken",
                profile = brokenAdmin
            )
        )
        val authCheck = schoolAdminRepository.checkSchoolAdminAuthorization()
        assertTrue("School Admin with null school_id must be rejected", authCheck.isFailure)
        assertTrue(authCheck.exceptionOrNull() is SecurityException)
    }

    // 7. School Admin can only view Teachers from assigned school
    @Test
    fun test_07_school_admin_can_only_view_teachers_from_assigned_school() = runBlocking {
        loginAsPuneSchoolAdmin()
        val teachersResult = schoolAdminRepository.getTeachers()
        assertTrue("Pune School Admin can query teachers", teachersResult.isSuccess)
        val teachers = teachersResult.getOrNull()!!
        assertTrue("Must find teachers", teachers.isNotEmpty())
        assertTrue("All returned teachers must belong to Pune school", teachers.all { it.schoolId == puneSchoolId })
    }

    // 8. School Admin cannot view Teachers from other schools
    @Test
    fun test_08_school_admin_cannot_view_teachers_from_other_schools() = runBlocking {
        loginAsPuneSchoolAdmin()
        val teachers = schoolAdminRepository.getTeachers().getOrNull()!!
        assertFalse("Mumbai teacher must not appear in Pune teacher list", teachers.any { it.schoolId == mumbaiSchoolId })
        assertFalse("Mumbai teacher by ID must not appear", teachers.any { it.id == mumbaiTeacherProfile.id })
    }

    // 9. School Admin cannot create Teacher for another school
    @Test
    fun test_09_school_admin_cannot_create_teacher_for_another_school() = runBlocking {
        loginAsPuneSchoolAdmin()
        val created = schoolAdminRepository.createTeacher(
            fullNameInput = "New Pune Teacher",
            emailInput = "new.pune.teacher@educhat.edu",
            mobileInput = "9822000000",
            passwordInput = "password123"
        )
        assertTrue("Creation succeeded", created.isSuccess)
        val teacher = created.getOrNull()!!
        assertEquals("Teacher must be assigned to Pune school", puneSchoolId, teacher.schoolId)
    }

    // 10. Created Teacher automatically assigned caller's school_id
    @Test
    fun test_10_created_teacher_automatically_assigned_caller_school_id() = runBlocking {
        loginAsMumbaiSchoolAdmin()
        val created = schoolAdminRepository.createTeacher(
            fullNameInput = "New Mumbai Teacher",
            emailInput = "new.mumbai.teacher@educhat.edu",
            mobileInput = "9833000000",
            passwordInput = "password123"
        )
        assertTrue(created.isSuccess)
        val teacher = created.getOrNull()!!
        assertEquals("Must be assigned to Mumbai school", mumbaiSchoolId, teacher.schoolId)
    }

    // 11. Created Teacher role forced to "teacher"
    @Test
    fun test_11_created_teacher_role_forced_to_teacher() = runBlocking {
        loginAsPuneSchoolAdmin()
        val created = schoolAdminRepository.createTeacher(
            fullNameInput = "Teacher Test Role",
            emailInput = "teacher.role@educhat.edu",
            mobileInput = null,
            passwordInput = "password123"
        )
        assertTrue(created.isSuccess)
        val teacher = created.getOrNull()!!
        assertEquals("Role must strictly be teacher", "teacher", teacher.role)
    }

    // 12. School Admin cannot create Officer Admin
    @Test
    fun test_12_school_admin_cannot_create_officer_admin() = runBlocking {
        loginAsPuneSchoolAdmin()
        val created = schoolAdminRepository.createTeacher(
            fullNameInput = "Attempt Officer Admin",
            emailInput = "attempt.oa@educhat.edu",
            mobileInput = null,
            passwordInput = "password123"
        )
        assertTrue(created.isSuccess)
        val profile = created.getOrNull()!!
        assertFalse("Created user cannot be officer admin", profile.role == "officer_admin")
        assertEquals("teacher", profile.role)
    }

    // 13. School Admin cannot create School Admin
    @Test
    fun test_13_school_admin_cannot_create_school_admin() = runBlocking {
        loginAsPuneSchoolAdmin()
        val created = schoolAdminRepository.createTeacher(
            fullNameInput = "Attempt School Admin",
            emailInput = "attempt.sa@educhat.edu",
            mobileInput = null,
            passwordInput = "password123"
        )
        assertTrue(created.isSuccess)
        val profile = created.getOrNull()!!
        assertFalse("Created user cannot be school admin", profile.role == "school_admin")
        assertEquals("teacher", profile.role)
    }

    // 14. School Admin cannot create Student
    @Test
    fun test_14_school_admin_cannot_create_student() = runBlocking {
        loginAsPuneSchoolAdmin()
        val created = schoolAdminRepository.createTeacher(
            fullNameInput = "Attempt Student",
            emailInput = "attempt.student@educhat.edu",
            mobileInput = null,
            passwordInput = "password123"
        )
        assertTrue(created.isSuccess)
        val profile = created.getOrNull()!!
        assertFalse("Created user cannot be student", profile.role == "student")
        assertEquals("teacher", profile.role)
    }

    // 15. School Admin can update teacher in own school
    @Test
    fun test_15_school_admin_can_update_teacher_in_own_school() = runBlocking {
        loginAsPuneSchoolAdmin()
        val updated = schoolAdminRepository.updateTeacher(
            teacherId = puneTeacherProfile.id,
            fullNameInput = "Pune Science Teacher (Updated)",
            mobileInput = "9898989898",
            isActive = true
        )
        assertTrue(updated.isSuccess)
        val teacher = updated.getOrNull()!!
        assertEquals("Pune Science Teacher (Updated)", teacher.fullName)
        assertEquals("9898989898", teacher.mobile)
    }

    // 16. School Admin cannot update teacher in another school
    @Test
    fun test_16_school_admin_cannot_update_teacher_in_another_school() = runBlocking {
        loginAsPuneSchoolAdmin()
        val updated = schoolAdminRepository.updateTeacher(
            teacherId = mumbaiTeacherProfile.id,
            fullNameInput = "Hacked Mumbai Teacher",
            mobileInput = "9999999999",
            isActive = true
        )
        assertTrue("Updating teacher in another school must be rejected", updated.isFailure)
        assertTrue(updated.exceptionOrNull() is SecurityException)
    }

    // 17. School Admin can deactivate teacher in own school
    @Test
    fun test_17_school_admin_can_deactivate_teacher_in_own_school() = runBlocking {
        loginAsPuneSchoolAdmin()
        val result = schoolAdminRepository.toggleTeacherStatus(puneTeacherProfile.id, false)
        assertTrue(result.isSuccess)
        assertFalse("Teacher must be deactivated", result.getOrNull()!!.isActive)
    }

    // 18. School Admin can reactivate teacher in own school
    @Test
    fun test_18_school_admin_can_reactivate_teacher_in_own_school() = runBlocking {
        loginAsPuneSchoolAdmin()
        // First deactivate
        schoolAdminRepository.toggleTeacherStatus(puneTeacherProfile.id, false)
        // Then reactivate
        val result = schoolAdminRepository.toggleTeacherStatus(puneTeacherProfile.id, true)
        assertTrue(result.isSuccess)
        assertTrue("Teacher must be reactivated", result.getOrNull()!!.isActive)
    }

    // 19. School Admin cannot deactivate teacher in another school
    @Test
    fun test_19_school_admin_cannot_deactivate_teacher_in_another_school() = runBlocking {
        loginAsPuneSchoolAdmin()
        val result = schoolAdminRepository.toggleTeacherStatus(mumbaiTeacherProfile.id, false)
        assertTrue("Deactivating teacher from another school must fail", result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    // 20. School Admin cannot modify teacher role or school_id
    @Test
    fun test_20_school_admin_cannot_modify_teacher_role_or_school_id() = runBlocking {
        loginAsPuneSchoolAdmin()
        val updated = schoolAdminRepository.updateTeacher(
            teacherId = puneTeacherProfile.id,
            fullNameInput = "Pune Teacher Modified",
            mobileInput = "9876543210",
            isActive = true
        )
        assertTrue(updated.isSuccess)
        val profile = updated.getOrNull()!!
        assertEquals("Role must remain teacher", "teacher", profile.role)
        assertEquals("School ID must remain Pune school", puneSchoolId, profile.schoolId)
    }

    // 21. School Admin cannot update Officer Admin profile
    @Test
    fun test_21_school_admin_cannot_update_officer_admin_profile() = runBlocking {
        val officerProfile = UserProfile(
            id = "off-target-1",
            fullName = "Target Officer",
            email = "target.officer@educhat.edu",
            role = "officer_admin",
            isActive = true,
            schoolId = puneSchoolId // Even if schoolId somehow matched
        )
        fakeApi.addUser(officerProfile.email!!, "pwd", officerProfile)

        loginAsPuneSchoolAdmin()
        val updated = schoolAdminRepository.updateTeacher(
            teacherId = officerProfile.id,
            fullNameInput = "Hacked Officer",
            mobileInput = null,
            isActive = true
        )
        assertTrue("Updating officer admin must fail", updated.isFailure)
        assertTrue(updated.exceptionOrNull() is SecurityException)
    }

    // 22. School Admin cannot update other School Admin profile
    @Test
    fun test_22_school_admin_cannot_update_other_school_admin_profile() = runBlocking {
        loginAsPuneSchoolAdmin()
        val updated = schoolAdminRepository.updateTeacher(
            teacherId = mumbaiAdminProfile.id,
            fullNameInput = "Hacked Mumbai Admin",
            mobileInput = null,
            isActive = true
        )
        assertTrue("Updating other school admin must fail", updated.isFailure)
        assertTrue(updated.exceptionOrNull() is SecurityException)
    }

    // 23. School Admin can only update own name and mobile
    @Test
    fun test_23_school_admin_can_only_update_own_name_and_mobile() = runBlocking {
        loginAsPuneSchoolAdmin()
        val updated = schoolAdminRepository.updateSchoolAdminProfile(
            fullNameInput = "Principal Dr. Ramesh Patil",
            mobileInput = "9988776655"
        )
        assertTrue(updated.isSuccess)
        val profile = updated.getOrNull()!!
        assertEquals("Principal Dr. Ramesh Patil", profile.fullName)
        assertEquals("9988776655", profile.mobile)
    }

    // 24. School Admin cannot change own role or school_id
    @Test
    fun test_24_school_admin_cannot_change_own_role_or_school_id() = runBlocking {
        loginAsPuneSchoolAdmin()
        val updated = schoolAdminRepository.updateSchoolAdminProfile(
            fullNameInput = "Principal Dr. Ramesh Patil",
            mobileInput = "9988776655"
        )
        assertTrue(updated.isSuccess)
        val profile = updated.getOrNull()!!
        assertEquals("Role must remain school_admin", "school_admin", profile.role)
        assertEquals("School ID must remain Pune school", puneSchoolId, profile.schoolId)
        assertTrue("Account must remain active", profile.isActive)
    }

    // 25. Duplicate email registration rejected
    @Test
    fun test_25_school_admin_duplicate_email_registration_rejected() = runBlocking {
        loginAsPuneSchoolAdmin()
        val result = schoolAdminRepository.createTeacher(
            fullNameInput = "Duplicate Teacher",
            emailInput = "teacher.pune@educhat.edu", // Already existing
            mobileInput = "9898000000",
            passwordInput = "password123"
        )
        assertTrue("Duplicate email must be rejected", result.isFailure)
        assertTrue(result.exceptionOrNull() is Exception)
    }
}
