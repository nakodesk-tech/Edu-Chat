package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionManager
import com.example.data.model.UserRole
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
class AuthRepositoryTest {

    private lateinit var context: Context
    private lateinit var authRepository: AuthRepository
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sessionManager = SessionManager(context)
        sessionManager.clearSession()
        com.example.data.local.SimulatedDatabase.reset()
        authRepository = AuthRepository(context)
    }

    // 1. Student attempts to change own role to Admin → MUST FAIL.
    @Test
    fun student_attempts_to_change_own_role_to_admin_must_fail() = runBlocking {
        val loginResult = authRepository.login("student@educhat.edu", "password123", UserRole.STUDENT)
        assertTrue(loginResult is AuthResult.Success)
        val studentSession = (loginResult as AuthResult.Success).session

        val patchResult = authRepository.attemptDirectProfilePatch(
            targetUserId = studentSession.profile.id,
            updates = mapOf("role" to "admin")
        )

        assertTrue("Student changing own role to admin must fail", patchResult.isFailure)
        val exception = patchResult.exceptionOrNull()
        assertTrue(exception is SecurityException)
        assertTrue(exception?.message?.contains("forbidden", ignoreCase = true) == true)
    }

    // 2. Teacher attempts to change own role to Admin → MUST FAIL.
    @Test
    fun teacher_attempts_to_change_own_role_to_admin_must_fail() = runBlocking {
        val loginResult = authRepository.login("teacher@educhat.edu", "password123", UserRole.TEACHER)
        assertTrue(loginResult is AuthResult.Success)
        val teacherSession = (loginResult as AuthResult.Success).session

        val patchResult = authRepository.attemptDirectProfilePatch(
            targetUserId = teacherSession.profile.id,
            updates = mapOf("role" to "admin")
        )

        assertTrue("Teacher changing own role to admin must fail", patchResult.isFailure)
        val exception = patchResult.exceptionOrNull()
        assertTrue(exception is SecurityException)
        assertTrue(exception?.message?.contains("forbidden", ignoreCase = true) == true)
    }

    // 3. Student attempts to set is_active = false → MUST FAIL.
    @Test
    fun student_attempts_to_set_is_active_false_must_fail() = runBlocking {
        val loginResult = authRepository.login("student@educhat.edu", "password123", UserRole.STUDENT)
        assertTrue(loginResult is AuthResult.Success)
        val studentSession = (loginResult as AuthResult.Success).session

        val patchResult = authRepository.attemptDirectProfilePatch(
            targetUserId = studentSession.profile.id,
            updates = mapOf("is_active" to false)
        )

        assertTrue("Student setting is_active = false must fail", patchResult.isFailure)
        val exception = patchResult.exceptionOrNull()
        assertTrue(exception is SecurityException)
        assertTrue(exception?.message?.contains("is_active") == true || exception?.message?.contains("forbidden") == true)
    }

    // 4. Student attempts to set is_active = true → MUST FAIL.
    @Test
    fun student_attempts_to_set_is_active_true_must_fail() = runBlocking {
        val loginResult = authRepository.login("student@educhat.edu", "password123", UserRole.STUDENT)
        assertTrue(loginResult is AuthResult.Success)
        val studentSession = (loginResult as AuthResult.Success).session

        val patchResult = authRepository.attemptDirectProfilePatch(
            targetUserId = studentSession.profile.id,
            updates = mapOf("is_active" to true)
        )

        assertTrue("Student modifying is_active must fail", patchResult.isFailure)
        val exception = patchResult.exceptionOrNull()
        assertTrue(exception is SecurityException)
    }

    // 5. Student attempts to modify another user's profile → MUST FAIL.
    @Test
    fun student_attempts_to_modify_another_users_profile_must_fail() = runBlocking {
        val loginResult = authRepository.login("student@educhat.edu", "password123", UserRole.STUDENT)
        assertTrue(loginResult is AuthResult.Success)

        val teacherProfileId = "d0a1b2c3-0001-4000-8000-000000000001"
        val patchResult = authRepository.attemptDirectProfilePatch(
            targetUserId = teacherProfileId,
            updates = mapOf("full_name" to "Hacked Name")
        )

        assertTrue("Student modifying another user's profile must fail with RLS violation", patchResult.isFailure)
        val exception = patchResult.exceptionOrNull()
        assertTrue(exception is SecurityException)
        assertTrue(exception?.message?.contains("RLS VIOLATION", ignoreCase = true) == true)
    }

    // 6. Client attempts to create a new Admin through signup metadata → MUST FAIL (Role forced to student).
    @Test
    fun client_attempts_to_create_new_admin_through_signup_metadata_must_fail() = runBlocking {
        val signupResult = authRepository.attemptSignupWithRoleMetadata(
            email = "malicious_actor@educhat.edu",
            pass = "password123",
            metadataRole = "admin" // Untrusted client payload claiming admin privilege
        )

        assertTrue(signupResult.isSuccess)
        val profile = signupResult.getOrNull()
        assertNotNull(profile)
        // Enforce trigger security: Role MUST strictly be 'student', NOT 'admin'
        assertEquals("student", profile?.role)
        assertEquals(UserRole.STUDENT, profile?.userRole)
    }

    // 7. Legitimate profile display-name update → MUST WORK.
    @Test
    fun legitimate_profile_display_name_update_must_work() = runBlocking {
        val loginResult = authRepository.login("teacher@educhat.edu", "password123", UserRole.TEACHER)
        assertTrue(loginResult is AuthResult.Success)

        val updateResult = authRepository.updateDisplayName("Prof. Sarah Jenkins, Ph.D.")
        assertTrue("Legitimate display name update must succeed", updateResult.isSuccess)
        val updatedProfile = updateResult.getOrNull()
        assertNotNull(updatedProfile)
        assertEquals("Prof. Sarah Jenkins, Ph.D.", updatedProfile?.fullName)
        assertEquals("teacher", updatedProfile?.role) // Role remains intact
    }

    // 8. Legitimate login + database role verification → MUST WORK.
    @Test
    fun legitimate_login_and_database_role_verification_must_work() = runBlocking {
        val teacherResult = authRepository.login("teacher@educhat.edu", "password123", UserRole.TEACHER)
        assertTrue(teacherResult is AuthResult.Success)
        assertEquals("teacher", (teacherResult as AuthResult.Success).session.profile.role)

        val studentResult = authRepository.login("student@educhat.edu", "password123", UserRole.STUDENT)
        assertTrue(studentResult is AuthResult.Success)
        assertEquals("student", (studentResult as AuthResult.Success).session.profile.role)

        val adminResult = authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        assertTrue(adminResult is AuthResult.Success)
        assertEquals("officer_admin", (adminResult as AuthResult.Success).session.profile.role)
        assertTrue((adminResult as AuthResult.Success).session.profile.isPrimaryAdmin)
    }

    // 9. Existing role mismatch protection → MUST WORK.
    @Test
    fun existing_role_mismatch_protection_must_work() = runBlocking {
        // Attempting to log into teacher account with STUDENT selected
        val mismatchResult = authRepository.login("teacher@educhat.edu", "password123", UserRole.STUDENT)
        assertTrue(mismatchResult is AuthResult.Error)
        val error = mismatchResult as AuthResult.Error
        assertEquals("The selected role does not match your account.", error.message)
        assertFalse(authRepository.isUserLoggedIn())
    }

    // 10. Existing inactive-account protection → MUST WORK.
    @Test
    fun existing_inactive_account_protection_must_work() = runBlocking {
        val inactiveResult = authRepository.login("inactive@educhat.edu", "password123", UserRole.STUDENT)
        assertTrue(inactiveResult is AuthResult.Error)
        val error = inactiveResult as AuthResult.Error
        assertEquals("This account has been deactivated. Please contact your administrator.", error.message)
        assertFalse(authRepository.isUserLoggedIn())
    }

    @Test
    fun logout_clears_session_properly() = runBlocking {
        authRepository.login("teacher@educhat.edu", "password123", UserRole.TEACHER)
        assertTrue(authRepository.isUserLoggedIn())

        authRepository.logout()
        assertFalse(authRepository.isUserLoggedIn())
    }
}
