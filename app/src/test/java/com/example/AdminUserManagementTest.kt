package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionManager
import com.example.data.local.SimulatedDatabase
import com.example.data.model.UserRole
import com.example.data.repository.AdminUserRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AdminUserManagementTest {

    private lateinit var context: Context
    private lateinit var authRepository: AuthRepository
    private lateinit var adminRepository: AdminUserRepository
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sessionManager = SessionManager(context)
        sessionManager.clearSession()
        SimulatedDatabase.reset()
        authRepository = AuthRepository(context)
        adminRepository = AdminUserRepository(context)
    }

    // 1. Admin can open Admin Dashboard / view users
    @Test
    fun admin_can_view_all_users() = runBlocking {
        val loginResult = authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        assertTrue(loginResult is AuthResult.Success)

        val session = (loginResult as AuthResult.Success).session
        assertEquals("officer_admin", session.profile.role)
        assertTrue(session.profile.isPrimaryAdmin)
        assertTrue(session.profile.isActive)
        assertEquals(null, session.profile.schoolId)

        val usersResult = adminRepository.getUsers()
        assertTrue("Admin can retrieve users", usersResult.isSuccess)
        val userList = usersResult.getOrNull()
        assertNotNull(userList)
        assertTrue(userList!!.isNotEmpty())
    }

    // 2. Teacher cannot perform Admin operations - Explicit checks
    @Test
    fun unauthorized_user_cannot_create_user() = runBlocking {
        // Test with Teacher
        authRepository.login("teacher@educhat.edu", "password123", UserRole.TEACHER)
        val teacherCreateResult = adminRepository.createUser(
            fullNameInput = "Illegal User",
            emailInput = "illegal@educhat.edu",
            passwordInput = "password123",
            role = UserRole.STUDENT
        )
        assertTrue("Teacher creating user must fail", teacherCreateResult.isFailure)
        assertTrue(teacherCreateResult.exceptionOrNull() is SecurityException)

        // Test with Student
        authRepository.logout()
        authRepository.login("student@educhat.edu", "password123", UserRole.STUDENT)
        val studentCreateResult = adminRepository.createUser(
            fullNameInput = "Illegal User 2",
            emailInput = "illegal2@educhat.edu",
            passwordInput = "password123",
            role = UserRole.STUDENT
        )
        assertTrue("Student creating user must fail", studentCreateResult.isFailure)
        assertTrue(studentCreateResult.exceptionOrNull() is SecurityException)
    }

    @Test
    fun unauthorized_user_cannot_edit_another_user() = runBlocking {
        val targetId = "d0a1b2c3-0002-4000-8000-000000000002" // Student Alex

        // Test with Teacher
        authRepository.login("teacher@educhat.edu", "password123", UserRole.TEACHER)
        val teacherEditResult = adminRepository.updateUser(
            userId = targetId,
            fullNameInput = "Hacked Name",
            role = UserRole.STUDENT,
            isActive = true
        )
        assertTrue("Teacher editing another user must fail", teacherEditResult.isFailure)
        assertTrue(teacherEditResult.exceptionOrNull() is SecurityException)

        // Test with Student
        authRepository.logout()
        authRepository.login("student@educhat.edu", "password123", UserRole.STUDENT)
        val studentEditResult = adminRepository.updateUser(
            userId = "d0a1b2c3-0001-4000-8000-000000000001", // Teacher Jenkins
            fullNameInput = "Hacked Teacher Name",
            role = UserRole.TEACHER,
            isActive = true
        )
        assertTrue("Student editing another user must fail", studentEditResult.isFailure)
        assertTrue(studentEditResult.exceptionOrNull() is SecurityException)
    }

    @Test
    fun unauthorized_user_cannot_change_another_user_role() = runBlocking {
        val studentId = "d0a1b2c3-0002-4000-8000-000000000002"

        // Test with Teacher attempting to promote student or change role
        authRepository.login("teacher@educhat.edu", "password123", UserRole.TEACHER)
        val teacherRoleChangeResult = adminRepository.updateUser(
            userId = studentId,
            fullNameInput = "Alex Rivera",
            role = UserRole.TEACHER,
            isActive = true
        )
        assertTrue("Teacher changing another user's role must fail", teacherRoleChangeResult.isFailure)
        assertTrue(teacherRoleChangeResult.exceptionOrNull() is SecurityException)

        // Test with Student attempting to elevate to Teacher/Admin
        authRepository.logout()
        authRepository.login("student@educhat.edu", "password123", UserRole.STUDENT)
        val studentRoleChangeResult = adminRepository.updateUser(
            userId = studentId,
            fullNameInput = "Alex Rivera",
            role = UserRole.OFFICER_ADMIN,
            isActive = true
        )
        assertTrue("Student elevating role must fail", studentRoleChangeResult.isFailure)
        assertTrue(studentRoleChangeResult.exceptionOrNull() is SecurityException)
    }

    @Test
    fun unauthorized_user_cannot_deactivate_another_user() = runBlocking {
        val targetId = "d0a1b2c3-0002-4000-8000-000000000002"

        // Test with Teacher
        authRepository.login("teacher@educhat.edu", "password123", UserRole.TEACHER)
        val teacherDeactResult = adminRepository.deactivateUser(targetId)
        assertTrue("Teacher deactivating user must fail", teacherDeactResult.isFailure)
        assertTrue(teacherDeactResult.exceptionOrNull() is SecurityException)

        // Test with Student
        authRepository.logout()
        authRepository.login("student@educhat.edu", "password123", UserRole.STUDENT)
        val studentDeactResult = adminRepository.deactivateUser(targetId)
        assertTrue("Student deactivating user must fail", studentDeactResult.isFailure)
        assertTrue(studentDeactResult.exceptionOrNull() is SecurityException)
    }

    @Test
    fun unauthorized_user_cannot_reactivate_another_user() = runBlocking {
        val inactiveUserId = "d0a1b2c3-0004-4000-8000-000000000004"

        // Test with Teacher
        authRepository.login("teacher@educhat.edu", "password123", UserRole.TEACHER)
        val teacherReactivateResult = adminRepository.reactivateUser(inactiveUserId)
        assertTrue("Teacher reactivating user must fail", teacherReactivateResult.isFailure)
        assertTrue(teacherReactivateResult.exceptionOrNull() is SecurityException)

        // Test with Student
        authRepository.logout()
        authRepository.login("student@educhat.edu", "password123", UserRole.STUDENT)
        val studentReactivateResult = adminRepository.reactivateUser(inactiveUserId)
        assertTrue("Student reactivating user must fail", studentReactivateResult.isFailure)
        assertTrue(studentReactivateResult.exceptionOrNull() is SecurityException)
    }

    // 4. Admin can add Teacher
    @Test
    fun admin_can_add_teacher() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)

        val result = adminRepository.createUser(
            fullNameInput = "Dr. Alan Grant",
            emailInput = "agrant@educhat.edu",
            passwordInput = "teachpass123",
            role = UserRole.TEACHER,
            isActive = true
        )

        assertTrue("Admin can create teacher", result.isSuccess)
        val created = result.getOrNull()
        assertNotNull(created)
        assertEquals("Dr. Alan Grant", created?.fullName)
        assertEquals("agrant@educhat.edu", created?.email)
        assertEquals("teacher", created?.role)
        assertTrue(created!!.isActive)

        // Verify the newly created teacher can log in
        authRepository.logout()
        val teacherLogin = authRepository.login("agrant@educhat.edu", "teachpass123", UserRole.TEACHER)
        assertTrue("Newly created teacher can log in", teacherLogin is AuthResult.Success)
    }

    // 5. Admin can add Student
    @Test
    fun admin_can_add_student() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)

        val result = adminRepository.createUser(
            fullNameInput = "Maya Lin",
            emailInput = "mlin@educhat.edu",
            passwordInput = "studpass123",
            role = UserRole.STUDENT,
            isActive = true
        )

        assertTrue("Admin can create student", result.isSuccess)
        val created = result.getOrNull()
        assertNotNull(created)
        assertEquals("Maya Lin", created?.fullName)
        assertEquals("student", created?.role)
        assertTrue(created!!.isActive)

        // Verify the newly created student can log in
        authRepository.logout()
        val studentLogin = authRepository.login("mlin@educhat.edu", "studpass123", UserRole.STUDENT)
        assertTrue("Newly created student can log in", studentLogin is AuthResult.Success)
    }

    // 6. Admin cannot create another Officer Admin or School Admin
    @Test
    fun admin_cannot_create_another_admin() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)

        val result = adminRepository.createUser(
            fullNameInput = "Second Admin",
            emailInput = "admin2@educhat.edu",
            passwordInput = "adminpass123",
            role = UserRole.OFFICER_ADMIN,
            isActive = true
        )

        assertTrue("Admin creating another admin must fail", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is SecurityException)
        assertTrue(exception?.message?.contains("Admin creation is not permitted", ignoreCase = true) == true)
    }

    // 7. Admin can edit Teacher
    @Test
    fun admin_can_edit_teacher() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)

        val teacherId = "d0a1b2c3-0001-4000-8000-000000000001"
        val result = adminRepository.updateUser(
            userId = teacherId,
            fullNameInput = "Prof. Sarah Jenkins-Smith",
            role = UserRole.TEACHER,
            isActive = true
        )

        assertTrue("Admin can edit teacher", result.isSuccess)
        val updated = result.getOrNull()
        assertEquals("Prof. Sarah Jenkins-Smith", updated?.fullName)
        assertEquals("teacher", updated?.role)
    }

    // 8. Admin can edit Student
    @Test
    fun admin_can_edit_student() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)

        val studentId = "d0a1b2c3-0002-4000-8000-000000000002"
        val result = adminRepository.updateUser(
            userId = studentId,
            fullNameInput = "Alex Rivera Jr.",
            role = UserRole.STUDENT,
            isActive = true
        )

        assertTrue("Admin can edit student", result.isSuccess)
        val updated = result.getOrNull()
        assertEquals("Alex Rivera Jr.", updated?.fullName)
    }

    // 9. Admin can deactivate Teacher
    @Test
    fun admin_can_deactivate_teacher() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)

        val teacherId = "d0a1b2c3-0001-4000-8000-000000000001"
        val result = adminRepository.deactivateUser(teacherId)

        assertTrue("Admin can deactivate teacher", result.isSuccess)
        assertFalse(result.getOrNull()!!.isActive)

        // Confirm teacher is blocked from logging in
        authRepository.logout()
        val loginAttempt = authRepository.login("teacher@educhat.edu", "password123", UserRole.TEACHER)
        assertTrue(loginAttempt is AuthResult.Error)
        assertEquals(
            "This account has been deactivated. Please contact your administrator.",
            (loginAttempt as AuthResult.Error).message
        )
    }

    // 10. Admin can deactivate Student
    @Test
    fun admin_can_deactivate_student() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)

        val studentId = "d0a1b2c3-0002-4000-8000-000000000002"
        val result = adminRepository.deactivateUser(studentId)

        assertTrue("Admin can deactivate student", result.isSuccess)
        assertFalse(result.getOrNull()!!.isActive)

        // Confirm student is blocked from logging in
        authRepository.logout()
        val loginAttempt = authRepository.login("student@educhat.edu", "password123", UserRole.STUDENT)
        assertTrue(loginAttempt is AuthResult.Error)
    }

    // 11. Admin can reactivate Teacher/Student
    @Test
    fun admin_can_reactivate_user() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)

        // "inactive@educhat.edu" is initially deactivated
        val inactiveStudentId = "d0a1b2c3-0004-4000-8000-000000000004"
        val reactivateResult = adminRepository.reactivateUser(inactiveStudentId)

        assertTrue("Admin can reactivate user", reactivateResult.isSuccess)
        assertTrue(reactivateResult.getOrNull()!!.isActive)

        // Verify reactivated user can now log in
        authRepository.logout()
        val loginResult = authRepository.login("inactive@educhat.edu", "password123", UserRole.STUDENT)
        assertTrue("Reactivated user can log in", loginResult is AuthResult.Success)
    }

    // 12. Admin cannot deactivate self
    @Test
    fun admin_cannot_deactivate_self() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)

        val adminId = "d0a1b2c3-0003-4000-8000-000000000003"
        val deactivateResult = adminRepository.deactivateUser(adminId)

        assertTrue("Admin deactivating self must fail", deactivateResult.isFailure)
        assertTrue(deactivateResult.exceptionOrNull() is SecurityException)

        // Also test direct update with isActive = false on self
        val updateResult = adminRepository.updateUser(
            userId = adminId,
            fullNameInput = "System Administrator",
            role = UserRole.OFFICER_ADMIN,
            isActive = false
        )
        assertTrue("Admin updating self to inactive must fail", updateResult.isFailure)
        assertTrue(updateResult.exceptionOrNull() is SecurityException)
    }

    // 13. Admin cannot change own role
    @Test
    fun admin_cannot_change_own_role() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)

        val adminId = "d0a1b2c3-0003-4000-8000-000000000003"
        val updateResult = adminRepository.updateUser(
            userId = adminId,
            fullNameInput = "System Administrator",
            role = UserRole.STUDENT,
            isActive = true
        )
        assertTrue("Admin changing own role must fail", updateResult.isFailure)
        assertTrue(updateResult.exceptionOrNull() is SecurityException)
    }

    // Specific Primary Officer Admin Security Requirements Verification
    // 1. Admin + correct Primary Officer Admin credentials -> SUCCESS
    @Test
    fun primary_officer_admin_login_success() = runBlocking {
        val result = authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        assertTrue("Primary Officer Admin login must succeed", result is AuthResult.Success)
        val profile = (result as AuthResult.Success).session.profile
        assertEquals("officer_admin", profile.role)
        assertTrue(profile.isPrimaryAdmin)
        assertTrue(profile.isActive)
        assertEquals(null, profile.schoolId)
    }

    // 2. Teacher role selected + Officer Admin credentials -> REJECT
    @Test
    fun officer_admin_credentials_with_teacher_role_rejected() = runBlocking {
        val result = authRepository.login("admin@educhat.edu", "password123", UserRole.TEACHER)
        assertTrue("Officer Admin credentials with Teacher role must be rejected", result is AuthResult.Error)
        assertEquals("The selected role does not match your account.", (result as AuthResult.Error).message)
    }

    // 3. Student role selected + Officer Admin credentials -> REJECT
    @Test
    fun officer_admin_credentials_with_student_role_rejected() = runBlocking {
        val result = authRepository.login("admin@educhat.edu", "password123", UserRole.STUDENT)
        assertTrue("Officer Admin credentials with Student role must be rejected", result is AuthResult.Error)
        assertEquals("The selected role does not match your account.", (result as AuthResult.Error).message)
    }

    // 4. Incorrect credentials -> REJECT
    @Test
    fun incorrect_credentials_rejected() = runBlocking {
        val result = authRepository.login("admin@educhat.edu", "wrongpassword", UserRole.OFFICER_ADMIN)
        assertTrue("Incorrect credentials must be rejected", result is AuthResult.Error)
        assertEquals("Invalid email or password.", (result as AuthResult.Error).message)
    }

    // 5. Inactive account -> REJECT
    @Test
    fun inactive_account_rejected() = runBlocking {
        val result = authRepository.login("inactive@educhat.edu", "password123", UserRole.STUDENT)
        assertTrue("Inactive account must be rejected", result is AuthResult.Error)
        assertEquals("This account has been deactivated. Please contact your administrator.", (result as AuthResult.Error).message)
    }

    // 6. Client cannot change officer_admin to another role
    @Test
    fun client_cannot_change_officer_admin_role() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        val adminId = "d0a1b2c3-0003-4000-8000-000000000003"
        val result = adminRepository.updateUser(
            userId = adminId,
            fullNameInput = "Primary Officer Admin",
            role = UserRole.TEACHER,
            isActive = true
        )
        assertTrue("Changing officer_admin role must be rejected", result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    // 7. Client cannot change is_primary_admin / elevate regular users to primary admin
    @Test
    fun client_cannot_change_is_primary_admin() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        val teacherId = "d0a1b2c3-0001-4000-8000-000000000001"
        val result = adminRepository.updateUser(
            userId = teacherId,
            fullNameInput = "Prof. Sarah Jenkins",
            role = UserRole.OFFICER_ADMIN,
            isActive = true
        )
        assertTrue("Elevating user to Officer Admin must be rejected", result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    // 8. Primary Officer Admin school_id remains NULL
    @Test
    fun primary_officer_admin_has_null_school_id() = runBlocking {
        val loginResult = authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        val profile = (loginResult as AuthResult.Success).session.profile
        assertEquals("Primary Officer Admin school_id must be NULL", null, profile.schoolId)
    }

    // 9. Primary Officer Admin cannot be deactivated
    @Test
    fun primary_officer_admin_cannot_be_deactivated() = runBlocking {
        authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        val adminId = "d0a1b2c3-0003-4000-8000-000000000003"
        val result = adminRepository.deactivateUser(adminId)
        assertTrue("Deactivating Primary Officer Admin must be rejected", result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    // 10. Old generic "admin" role is not recognized for authorization
    @Test
    fun old_generic_admin_role_rejected() = runBlocking {
        sessionManager.saveSession(
            com.example.data.model.AuthSession(
                accessToken = "test_token",
                refreshToken = "test_refresh",
                profile = com.example.data.model.UserProfile(
                    id = "test-generic-admin",
                    fullName = "Old Admin",
                    email = "old@admin.com",
                    role = "admin", // Old generic role
                    isActive = true
                )
            )
        )
        val result = adminRepository.getUsers()
        assertTrue("Old generic admin role must be rejected for authorization", result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }
}
