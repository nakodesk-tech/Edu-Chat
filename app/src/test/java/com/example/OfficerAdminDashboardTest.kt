package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionManager
import com.example.data.local.SimulatedDatabase
import com.example.data.model.AuthSession
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.remote.SupabaseConfig
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthResult
import com.example.data.repository.OfficerAdminRepository
import kotlinx.coroutines.runBlocking
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfficerAdminDashboardTest {

    private lateinit var context: Context
    private lateinit var authRepository: AuthRepository
    private lateinit var officerAdminRepository: OfficerAdminRepository
    private lateinit var sessionManager: SessionManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        SimulatedDatabase.reset()
        sessionManager = SessionManager(context)
        sessionManager.clearSession()
        authRepository = AuthRepository(context)
        officerAdminRepository = OfficerAdminRepository(context)
    }

    // 1. Primary Officer Admin can access dashboard
    @Test
    fun primary_officer_admin_can_access_dashboard() = runBlocking {
        val loginResult = authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        assertTrue(loginResult is AuthResult.Success)

        val authCheck = officerAdminRepository.checkOfficerAdminAuthorization()
        assertTrue("Primary Officer Admin authorization must succeed", authCheck.isSuccess)
        val profile = authCheck.getOrNull()!!.profile
        assertEquals("officer_admin", profile.role)
        assertTrue(profile.isPrimaryAdmin)
        assertTrue(profile.isActive)
        assertNull(profile.schoolId)
    }

    // 2. Normal Officer Admin can access dashboard
    @Test
    fun normal_officer_admin_can_access_dashboard() = runBlocking {
        // Create normal officer admin first
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        val createResult = officerAdminRepository.createOfficerAdmin(
            fullNameInput = "Sub-divisional Officer Patil",
            emailInput = "officer.patil@educhat.edu",
            mobileInput = "9823011223",
            passwordInput = "officerpass123"
        )
        assertTrue("Creating normal officer admin succeeds", createResult.isSuccess)

        // Login as normal officer admin
        val loginResult = authRepository.login("officer.patil@educhat.edu", "officerpass123", UserRole.OFFICER_ADMIN)
        assertTrue(loginResult is AuthResult.Success)

        val authCheck = officerAdminRepository.checkOfficerAdminAuthorization()
        assertTrue("Normal Officer Admin authorization must succeed", authCheck.isSuccess)
        val profile = authCheck.getOrNull()!!.profile
        assertEquals("officer_admin", profile.role)
        assertFalse("Normal officer admin is not primary", profile.isPrimaryAdmin)
        assertTrue(profile.isActive)
        assertNull("Officer admin has school_id = NULL", profile.schoolId)
    }

    // 3. School Admin cannot access Officer Admin dashboard
    @Test
    fun school_admin_cannot_access_officer_admin_dashboard() = runBlocking {
        // Create a School Admin
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        val schoolId = "s0000000-0001-4000-8000-000000000001"
        officerAdminRepository.createSchoolAdmin(
            fullNameInput = "Principal Deshmukh",
            emailInput = "principal@educhat.edu",
            mobileInput = "9822099887",
            passwordInput = "schooladmin123",
            schoolIdInput = schoolId
        )

        // Login as School Admin
        authRepository.login("principal@educhat.edu", "schooladmin123", UserRole.SCHOOL_ADMIN)

        val authCheck = officerAdminRepository.checkOfficerAdminAuthorization()
        assertTrue("School Admin must be denied Officer Admin access", authCheck.isFailure)
        assertTrue(authCheck.exceptionOrNull() is SecurityException)
    }

    // 4. Teacher cannot access Officer Admin dashboard
    @Test
    fun teacher_cannot_access_officer_admin_dashboard() = runBlocking {
        authRepository.login("teacher@educhat.edu", "password123", UserRole.TEACHER)

        val authCheck = officerAdminRepository.checkOfficerAdminAuthorization()
        assertTrue("Teacher must be denied Officer Admin access", authCheck.isFailure)
        assertTrue(authCheck.exceptionOrNull() is SecurityException)
    }

    // 5. Student cannot access Officer Admin dashboard
    @Test
    fun student_cannot_access_officer_admin_dashboard() = runBlocking {
        authRepository.login("student@educhat.edu", "password123", UserRole.STUDENT)

        val authCheck = officerAdminRepository.checkOfficerAdminAuthorization()
        assertTrue("Student must be denied Officer Admin access", authCheck.isFailure)
        assertTrue(authCheck.exceptionOrNull() is SecurityException)
    }

    // 6. Officer Admin can create Officer Admin
    @Test
    fun officer_admin_can_create_officer_admin() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)

        val result = officerAdminRepository.createOfficerAdmin(
            fullNameInput = "Education Officer Kulkarni",
            emailInput = "kulkarni@educhat.edu",
            mobileInput = "9822044556",
            passwordInput = "securepass123"
        )
        assertTrue("Officer Admin creation must succeed", result.isSuccess)
        val created = result.getOrNull()!!
        assertEquals("kulkarni@educhat.edu", created.email)
        assertEquals("officer_admin", created.role)
        assertEquals("9822044556", created.mobile)
        assertFalse("Newly created Officer Admin is_primary_admin MUST be false", created.isPrimaryAdmin)
        assertNull("Officer Admin school_id MUST be NULL", created.schoolId)
        assertTrue("Officer Admin is_active MUST be true", created.isActive)
    }

    // 7. Newly created Officer Admin has is_primary_admin = false & school_id = NULL
    @Test
    fun newly_created_officer_admin_has_primary_false_and_null_school_id() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)

        val result = officerAdminRepository.createOfficerAdmin(
            fullNameInput = "Officer Shinde",
            emailInput = "shinde@educhat.edu",
            mobileInput = "9822066778",
            passwordInput = "shindepass123"
        )
        assertTrue(result.isSuccess)
        val profile = result.getOrNull()!!
        assertFalse("is_primary_admin must be false", profile.isPrimaryAdmin)
        assertNull("school_id must be NULL", profile.schoolId)
    }

    // 8. Officer Admin can create School Admin
    @Test
    fun officer_admin_can_create_school_admin() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)

        val schoolId = "s0000000-0001-4000-8000-000000000001"
        val result = officerAdminRepository.createSchoolAdmin(
            fullNameInput = "Headmaster Joshi",
            emailInput = "joshi.hm@educhat.edu",
            mobileInput = "9822011990",
            passwordInput = "joshipass123",
            schoolIdInput = schoolId
        )

        assertTrue("School Admin creation must succeed", result.isSuccess)
        val created = result.getOrNull()!!
        assertEquals("joshi.hm@educhat.edu", created.email)
        assertEquals("school_admin", created.role)
        assertEquals(schoolId, created.schoolId)
        assertFalse(created.isPrimaryAdmin)
        assertTrue(created.isActive)
    }

    // 9. School Admin receives exactly one valid school assignment (cannot be empty or inactive)
    @Test
    fun school_admin_requires_valid_active_school_assignment() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)

        // Empty school assignment must fail
        val emptySchoolResult = officerAdminRepository.createSchoolAdmin(
            fullNameInput = "No School Admin",
            emailInput = "noschool@educhat.edu",
            mobileInput = "9822011990",
            passwordInput = "joshipass123",
            schoolIdInput = ""
        )
        assertTrue("Empty school ID must fail", emptySchoolResult.isFailure)

        // Inactive school assignment must fail
        val inactiveSchoolId = "s0000000-0003-4000-8000-000000000003"
        val inactiveSchoolResult = officerAdminRepository.createSchoolAdmin(
            fullNameInput = "Inactive School Admin",
            emailInput = "inactiveschool@educhat.edu",
            mobileInput = "9822011990",
            passwordInput = "joshipass123",
            schoolIdInput = inactiveSchoolId
        )
        assertTrue("Inactive school assignment must fail", inactiveSchoolResult.isFailure)
    }

    // 10. Officer Admin can create School with all 6 required fields
    @Test
    fun officer_admin_can_create_school() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)

        val result = officerAdminRepository.createSchool(
            nameInput = "नवीन प्राथमिक शाळा, कोल्हापूर (Kolhapur Model School)",
            codeInput = "27260100504",
            mobileInput = "9822123456",
            emailInput = "kolhapur.04@educhat.edu",
            addressInput = "शाहूपुरी, कोल्हापूर, महाराष्ट्र ४१६००१"
        )
        assertTrue("School creation must succeed", result.isSuccess)
        val school = result.getOrNull()!!
        assertEquals("27260100504", school.code)
        assertEquals("नवीन प्राथमिक शाळा, कोल्हापूर (Kolhapur Model School)", school.name)
        assertEquals("9822123456", school.mobile)
        assertEquals("kolhapur.04@educhat.edu", school.email)
        assertEquals("शाहूपुरी, कोल्हापूर, महाराष्ट्र ४१६००१", school.address)
        assertTrue("Default is_active must be true", school.isActive)
    }

    // 11. Duplicate school code is rejected by database unique constraint
    @Test
    fun duplicate_school_code_is_rejected() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)

        val duplicateResult = officerAdminRepository.createSchool(
            nameInput = "Duplicate School",
            codeInput = "SCH-PUN-001", // Existing Pune school code
            mobileInput = "9822000000",
            emailInput = "dup@educhat.edu",
            addressInput = "Pune"
        )
        assertTrue("Duplicate school code must be rejected", duplicateResult.isFailure)
        assertTrue(duplicateResult.exceptionOrNull() is IllegalArgumentException)
        assertTrue(duplicateResult.exceptionOrNull()!!.message!!.contains("unique"))
    }

    // 12. Officer Admin can edit School with all fields
    @Test
    fun officer_admin_can_edit_school() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)

        val schoolId = "s0000000-0001-4000-8000-000000000001"
        val result = officerAdminRepository.updateSchool(
            schoolId = schoolId,
            nameInput = "जिल्हा परिषद आदर्श शाळा, पुणे",
            codeInput = "27251401599",
            mobileInput = "9822099999",
            emailInput = "updated.pune@educhat.edu",
            addressInput = "शिवाजीनगर विस्तारित, पुणे ४११०१६",
            isActive = true
        )
        assertTrue("School update must succeed", result.isSuccess)
        val updated = result.getOrNull()!!
        assertEquals("जिल्हा परिषद आदर्श शाळा, पुणे", updated.name)
        assertEquals("27251401599", updated.code)
        assertEquals("9822099999", updated.mobile)
        assertEquals("updated.pune@educhat.edu", updated.email)
        assertEquals("शिवाजीनगर विस्तारित, पुणे ४११०१६", updated.address)
        assertTrue(updated.isActive)
    }

    // 12b. Invalid mobile or email in School Registration is rejected
    @Test
    fun invalid_mobile_or_email_for_school_is_rejected() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)

        val badMobileResult = officerAdminRepository.createSchool(
            nameInput = "Test School",
            codeInput = "27299900001",
            mobileInput = "123", // invalid mobile length
            emailInput = "valid@educhat.edu",
            addressInput = "Pune"
        )
        assertTrue("Invalid mobile must fail", badMobileResult.isFailure)
        assertTrue(badMobileResult.exceptionOrNull() is IllegalArgumentException)

        val badEmailResult = officerAdminRepository.createSchool(
            nameInput = "Test School",
            codeInput = "27299900001",
            mobileInput = "9822000000",
            emailInput = "not-an-email", // invalid email format
            addressInput = "Pune"
        )
        assertTrue("Invalid email must fail", badEmailResult.isFailure)
        assertTrue(badEmailResult.exceptionOrNull() is IllegalArgumentException)
    }

    // 12c. Blank school name or blank UDISE code in School Registration is rejected
    @Test
    fun blank_name_or_code_in_school_registration_is_rejected() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)

        val blankNameResult = officerAdminRepository.createSchool(
            nameInput = "   ",
            codeInput = "27299900002",
            mobileInput = "9822000000",
            emailInput = "school@educhat.edu",
            addressInput = "Pune"
        )
        assertTrue("Blank school name must fail", blankNameResult.isFailure)
        assertTrue(blankNameResult.exceptionOrNull() is IllegalArgumentException)

        val blankCodeResult = officerAdminRepository.createSchool(
            nameInput = "Valid School Name",
            codeInput = "   ",
            mobileInput = "9822000000",
            emailInput = "school@educhat.edu",
            addressInput = "Pune"
        )
        assertTrue("Blank school code must fail", blankCodeResult.isFailure)
        assertTrue(blankCodeResult.exceptionOrNull() is IllegalArgumentException)
    }

    // 12d. Inactive Officer Admin cannot register a school
    @Test
    fun inactive_officer_admin_cannot_register_school() = runBlocking {
        // Create an inactive officer admin session
        sessionManager.saveSession(
            AuthSession(
                accessToken = "inactive_officer_token",
                refreshToken = "inactive_refresh",
                profile = UserProfile(
                    id = "inactive-officer-id",
                    fullName = "Inactive Officer",
                    email = "inactive.officer@educhat.edu",
                    role = "officer_admin",
                    isActive = false,
                    isPrimaryAdmin = false,
                    schoolId = null
                )
            )
        )

        val result = officerAdminRepository.createSchool(
            nameInput = "Unauthorized School",
            codeInput = "27299900003",
            mobileInput = "9822000000",
            emailInput = "unauth@educhat.edu",
            addressInput = "Pune"
        )
        assertTrue("Inactive Officer Admin must be rejected from creating school", result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("deactivated", ignoreCase = true))
    }

    // 13. Officer Admin can deactivate/reactivate School (soft deactivation)
    @Test
    fun officer_admin_can_deactivate_and_reactivate_school() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)

        val schoolId = "s0000000-0002-4000-8000-000000000002"
        val deactivateResult = officerAdminRepository.toggleSchoolStatus(schoolId, false)
        assertTrue("Deactivation must succeed", deactivateResult.isSuccess)
        assertFalse("School isActive must be false", deactivateResult.getOrNull()!!.isActive)

        val reactivateResult = officerAdminRepository.toggleSchoolStatus(schoolId, true)
        assertTrue("Reactivation must succeed", reactivateResult.isSuccess)
        assertTrue("School isActive must be true", reactivateResult.getOrNull()!!.isActive)
    }

    // 14. Primary Officer Admin profile editing (updates permitted fields only)
    @Test
    fun officer_admin_profile_editing_updates_only_permitted_fields() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)

        val updateResult = officerAdminRepository.updateOfficerProfile(
            fullNameInput = "Chief Education Officer Maharashtra",
            mobileInput = "9800998877"
        )
        assertTrue("Profile update must succeed", updateResult.isSuccess)
        val profile = updateResult.getOrNull()!!
        assertEquals("Chief Education Officer Maharashtra", profile.fullName)
        assertEquals("9800998877", profile.mobile)
        assertEquals("officer_admin", profile.role)
        assertTrue(profile.isPrimaryAdmin)
        assertNull(profile.schoolId)
    }

    // 15. Non-Officer-Admin direct calls to privileged operations are rejected
    @Test
    fun non_officer_admin_direct_calls_are_rejected() = runBlocking {
        // Teacher tries direct call
        authRepository.login("teacher@educhat.edu", "password123", UserRole.TEACHER)

        val createOfficerResult = officerAdminRepository.createOfficerAdmin("Hacked Admin", "hack@admin.com", "9822000000", "pass123")
        assertTrue("Teacher creating Officer Admin must fail", createOfficerResult.isFailure)
        assertTrue(createOfficerResult.exceptionOrNull() is SecurityException)

        val createSchoolResult = officerAdminRepository.createSchool("Hacked School", "HACK-01", "Nowhere")
        assertTrue("Teacher creating School must fail", createSchoolResult.isFailure)
        assertTrue(createSchoolResult.exceptionOrNull() is SecurityException)

        val getSchoolsResult = officerAdminRepository.getSchools()
        assertTrue("Teacher fetching Schools must fail", getSchoolsResult.isFailure)
        assertTrue(getSchoolsResult.exceptionOrNull() is SecurityException)
    }

    // 16. Old generic "admin" role remains rejected
    @Test
    fun old_generic_admin_role_remains_rejected() = runBlocking {
        sessionManager.saveSession(
            AuthSession(
                accessToken = "legacy_token",
                refreshToken = "legacy_refresh",
                profile = UserProfile(
                    id = "legacy-admin-id",
                    fullName = "Legacy Admin",
                    email = "legacy@admin.edu",
                    role = "admin", // Old generic role
                    isActive = true
                )
            )
        )

        val authCheck = officerAdminRepository.checkOfficerAdminAuthorization()
        assertTrue("Old generic admin role must be rejected", authCheck.isFailure)
        assertTrue(authCheck.exceptionOrNull() is SecurityException)
    }

    // 17. No privileged secret exists in Android client
    @Test
    fun verify_no_service_role_or_privileged_secret_exists() {
        val apiKey = SupabaseConfig.getSupabaseAnonKey(context)
        assertFalse("Client must NOT contain service_role key", apiKey.contains("service_role", ignoreCase = true))
        assertFalse("Client must NOT contain secret key", apiKey.contains("secret", ignoreCase = true))
    }

    // 18. Officer Admin Login -> Logout -> Immediate Re-login without restart
    @Test
    fun officer_admin_login_logout_immediate_relogin_without_restart() = runBlocking {
        // Step 1: Officer Admin login succeeds
        val login1 = authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        assertTrue("First login succeeds", login1 is AuthResult.Success)
        val authCheck1 = officerAdminRepository.checkOfficerAdminAuthorization()
        assertTrue("Officer authorized", authCheck1.isSuccess)

        // Step 2: Tap Logout
        authRepository.logout()
        assertFalse("Logged out", authRepository.isUserLoggedIn())
        assertNull("Session cleared", sessionManager.getSession())

        // Step 3 & 4: Immediate Login again with same valid credentials without app restart
        val login2 = authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        assertTrue("Immediate second login succeeds", login2 is AuthResult.Success)
        assertTrue("User is logged in", authRepository.isUserLoggedIn())

        // Step 5: Dashboard authorization and profile retrieval work immediately
        val authCheck2 = officerAdminRepository.checkOfficerAdminAuthorization()
        assertTrue("Officer authorization succeeds on immediate re-login", authCheck2.isSuccess)
        val profile2 = authCheck2.getOrNull()!!.profile
        assertEquals("admin@educhat.edu", profile2.email)
        assertEquals("officer_admin", profile2.role)
    }

    // 19. Officer Admin can retrieve registered users list with all roles
    @Test
    fun officer_admin_can_retrieve_registered_users_list() = runBlocking {
        val loginResult = authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        assertTrue(loginResult is AuthResult.Success)

        val adminUserRepo = com.example.data.repository.AdminUserRepository(context)
        val usersResult = adminUserRepo.getUsers()
        assertTrue("Admin can retrieve all registered users", usersResult.isSuccess)
        val users = usersResult.getOrNull()!!
        assertTrue("User list should not be empty", users.isNotEmpty())

        // Verify users of different roles exist
        assertTrue("Should have officer admin", users.any { it.role.equals("officer_admin", ignoreCase = true) })
        assertTrue("Should have teacher", users.any { it.role.equals("teacher", ignoreCase = true) })
        assertTrue("Should have student", users.any { it.role.equals("student", ignoreCase = true) })
    }

    // 20. Officer Admin can update user details
    @Test
    fun officer_admin_can_update_user_details() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        val adminUserRepo = com.example.data.repository.AdminUserRepository(context)

        // Find teacher user to update
        val users = adminUserRepo.getUsers().getOrNull()!!
        val teacherUser = users.first { it.role.equals("teacher", ignoreCase = true) }

        val updateResult = adminUserRepo.updateUser(
            userId = teacherUser.id,
            fullNameInput = "Updated Teacher Name",
            role = UserRole.TEACHER,
            isActive = true,
            mobileInput = "9822000000"
        )
        assertTrue("Update user succeeds", updateResult.isSuccess)
        val updated = updateResult.getOrNull()!!
        assertEquals("Updated Teacher Name", updated.fullName)
        assertEquals("9822000000", updated.mobile)
    }

    // 21. Officer Admin can toggle user active/inactive status
    @Test
    fun officer_admin_can_toggle_user_status() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        val adminUserRepo = com.example.data.repository.AdminUserRepository(context)

        val users = adminUserRepo.getUsers().getOrNull()!!
        val studentUser = users.first { it.role.equals("student", ignoreCase = true) }

        // Deactivate
        val deactResult = adminUserRepo.deactivateUser(studentUser.id)
        assertTrue("Deactivation succeeds", deactResult.isSuccess)
        val deactivated = deactResult.getOrNull()!!
        assertFalse(deactivated.isActive)

        // Reactivate
        val reactResult = adminUserRepo.reactivateUser(studentUser.id)
        assertTrue("Reactivation succeeds", reactResult.isSuccess)
        val reactivated = reactResult.getOrNull()!!
        assertTrue(reactivated.isActive)
    }

    // 22. Officer Admin can delete non-primary user and cannot delete self
    @Test
    fun officer_admin_user_deletion_safety() = runBlocking {
        val loginResult = authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        val session = (loginResult as AuthResult.Success).session
        val adminUserRepo = com.example.data.repository.AdminUserRepository(context)

        // Attempting to delete own account must fail
        val selfDeleteResult = adminUserRepo.deleteUser(session.profile.id)
        assertTrue("Self deletion must be rejected", selfDeleteResult.isFailure)

        // Deleting a non-admin user succeeds
        val users = adminUserRepo.getUsers().getOrNull()!!
        val target = users.first { it.role.equals("student", ignoreCase = true) }
        val deleteResult = adminUserRepo.deleteUser(target.id)
        assertTrue("Deleting non-admin user succeeds", deleteResult.isSuccess)

        val remainingUsers = adminUserRepo.getUsers().getOrNull()!!
        assertFalse("User should no longer exist in list", remainingUsers.any { it.id == target.id })
    }
}
