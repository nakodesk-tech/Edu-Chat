package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionManager
import com.example.data.model.UserRole
import com.example.data.remote.SupabaseClient
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthResult
import com.example.data.repository.OfficerAdminRepository
import com.example.data.repository.SchoolRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SchoolSecurityRlsTest {

    private lateinit var context: Context
    private lateinit var authRepository: AuthRepository
    private lateinit var officerAdminRepository: OfficerAdminRepository
    private lateinit var schoolRepository: SchoolRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var fakeApi: FakeSupabaseDatabaseEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fakeApi = FakeSupabaseDatabaseEngine()
        SupabaseClient.testApiOverride = fakeApi
        sessionManager = SessionManager(context)
        sessionManager.clearSession()
        authRepository = AuthRepository(context, sessionManager, fakeApi)
        officerAdminRepository = OfficerAdminRepository(context, sessionManager, fakeApi)
        schoolRepository = SchoolRepository(context, sessionManager, fakeApi)
    }

    @After
    fun tearDown() {
        SupabaseClient.reset()
    }

    // 1. Officer Admin can view all schools
    @Test
    fun officer_admin_can_view_all_schools() = runBlocking {
        val loginResult = authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        assertTrue(loginResult is AuthResult.Success)

        val schoolsResult = schoolRepository.getAccessibleSchools()
        assertTrue("Officer admin must be able to view schools", schoolsResult.isSuccess)
        val schools = schoolsResult.getOrNull()!!
        assertEquals("Officer admin must see all 3 registered schools", 3, schools.size)
    }

    // 2. School Admin can view only assigned school
    @Test
    fun school_admin_can_view_only_assigned_school() = runBlocking {
        // Create a School Admin assigned to Pune school (s0000000-0001-4000-8000-000000000001)
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        val puneSchoolId = "s0000000-0001-4000-8000-000000000001"
        officerAdminRepository.createSchoolAdmin(
            fullNameInput = "Principal Deshmukh",
            emailInput = "principal.pune@educhat.edu",
            mobileInput = "9822099887",
            passwordInput = "schooladmin123",
            schoolIdInput = puneSchoolId
        )

        // Login as School Admin
        authRepository.login("principal.pune@educhat.edu", "schooladmin123", UserRole.SCHOOL_ADMIN)

        val schoolsResult = schoolRepository.getAccessibleSchools()
        assertTrue("School Admin can query accessible schools", schoolsResult.isSuccess)
        val schools = schoolsResult.getOrNull()!!
        assertEquals("School Admin must see ONLY 1 school", 1, schools.size)
        assertEquals(puneSchoolId, schools.first().id)
        assertEquals("SCH-PUN-001", schools.first().code)
    }

    // 3. Teacher can view only assigned school
    @Test
    fun teacher_can_view_only_assigned_school() = runBlocking {
        val loginResult = authRepository.login("teacher@educhat.edu", "password123", UserRole.TEACHER)
        assertTrue(loginResult is AuthResult.Success)

        val schoolsResult = schoolRepository.getAccessibleSchools()
        assertTrue("Teacher can query accessible schools", schoolsResult.isSuccess)
        val schools = schoolsResult.getOrNull()!!
        assertEquals("Teacher must see ONLY 1 assigned school", 1, schools.size)
        assertEquals("s0000000-0001-4000-8000-000000000001", schools.first().id)
        assertEquals("SCH-PUN-001", schools.first().code)
    }

    // 4. Student can view only assigned school
    @Test
    fun student_can_view_only_assigned_school() = runBlocking {
        val loginResult = authRepository.login("student@educhat.edu", "password123", UserRole.STUDENT)
        assertTrue(loginResult is AuthResult.Success)

        val schoolsResult = schoolRepository.getAccessibleSchools()
        assertTrue("Student can query accessible schools", schoolsResult.isSuccess)
        val schools = schoolsResult.getOrNull()!!
        assertEquals("Student must see ONLY 1 assigned school", 1, schools.size)
        assertEquals("s0000000-0001-4000-8000-000000000001", schools.first().id)
        assertEquals("SCH-PUN-001", schools.first().code)
    }

    // 5. School Admin cannot query another school's details directly
    @Test
    fun school_admin_cannot_query_another_schools_details_directly() = runBlocking {
        // School Admin assigned to Pune school
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        val puneSchoolId = "s0000000-0001-4000-8000-000000000001"
        val thaneSchoolId = "s0000000-0002-4000-8000-000000000002"
        officerAdminRepository.createSchoolAdmin(
            fullNameInput = "Principal Deshmukh",
            emailInput = "principal.pune@educhat.edu",
            mobileInput = "9822099887",
            passwordInput = "schooladmin123",
            schoolIdInput = puneSchoolId
        )

        // Login as School Admin
        authRepository.login("principal.pune@educhat.edu", "schooladmin123", UserRole.SCHOOL_ADMIN)

        // 1. Can query their own school
        val ownSchoolResult = schoolRepository.getSchoolById(puneSchoolId)
        assertTrue("School Admin can query own school", ownSchoolResult.isSuccess)

        // 2. CANNOT query Thane school (another school)
        val otherSchoolResult = schoolRepository.getSchoolById(thaneSchoolId)
        assertTrue("School Admin querying another school must fail with SecurityException", otherSchoolResult.isFailure)
        assertTrue(otherSchoolResult.exceptionOrNull() is SecurityException)
    }

    // 6. Teacher cannot query another school's details directly
    @Test
    fun teacher_cannot_query_another_schools_details_directly() = runBlocking {
        authRepository.login("teacher@educhat.edu", "password123", UserRole.TEACHER)
        // Teacher is in Pune school (s0000000-0001-4000-8000-000000000001)
        val thaneSchoolId = "s0000000-0002-4000-8000-000000000002"

        val otherSchoolResult = schoolRepository.getSchoolById(thaneSchoolId)
        assertTrue("Teacher querying another school must fail with SecurityException", otherSchoolResult.isFailure)
        assertTrue(otherSchoolResult.exceptionOrNull() is SecurityException)
    }

    // 7. Student cannot query another school's details directly
    @Test
    fun student_cannot_query_another_schools_details_directly() = runBlocking {
        authRepository.login("student@educhat.edu", "password123", UserRole.STUDENT)
        // Student is in Pune school (s0000000-0001-4000-8000-000000000001)
        val nagpurSchoolId = "s0000000-0003-4000-8000-000000000003"

        val otherSchoolResult = schoolRepository.getSchoolById(nagpurSchoolId)
        assertTrue("Student querying another school must fail with SecurityException", otherSchoolResult.isFailure)
        assertTrue(otherSchoolResult.exceptionOrNull() is SecurityException)
    }

    // 8. School deletion cannot orphan users (ON DELETE RESTRICT / NO ACTION enforcement)
    @Test
    fun school_deletion_cannot_orphan_users() = runBlocking {
        val puneSchoolId = "s0000000-0001-4000-8000-000000000001"
        
        // Users exist in Pune School (teacher@educhat.edu and student@educhat.edu)
        val deleteAttempt = fakeApi.deleteSchoolWithRestrictCheck(puneSchoolId)
        assertTrue("Deleting school with existing users must fail", deleteAttempt.isFailure)
        assertTrue(
            "Exception must enforce foreign key constraint",
            deleteAttempt.exceptionOrNull() is IllegalStateException
        )
        assertTrue(
            deleteAttempt.exceptionOrNull()!!.message!!.contains("ON DELETE RESTRICT")
        )

        // School must still exist
        val school = fakeApi.findSchoolById(puneSchoolId)
        assertTrue("School must not be deleted or orphaned", school != null)
    }
}
