package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionManager
import com.example.data.model.UserRole
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthResult
import com.example.ui.auth.AuthUiState
import com.example.ui.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
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

    // 11. Login → Logout → Login without app restart
    @Test
    fun test_login_logout_login_without_app_restart() = runBlocking {
        // Initial Login as Officer Admin
        val firstLogin = authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        assertTrue(firstLogin is AuthResult.Success)
        assertTrue(authRepository.isUserLoggedIn())
        assertEquals("officer_admin", sessionManager.getUserProfile()?.role)

        // Logout
        val logoutSuccess = authRepository.logout()
        assertTrue(logoutSuccess)
        assertFalse(authRepository.isUserLoggedIn())
        assertNull(sessionManager.getAccessToken())
        assertNull(sessionManager.getUserProfile())

        // Immediate Re-login without restarting app
        val secondLogin = authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        assertTrue(secondLogin is AuthResult.Success)
        assertTrue(authRepository.isUserLoggedIn())
        assertEquals("officer_admin", sessionManager.getUserProfile()?.role)
        assertEquals("admin@educhat.edu", sessionManager.getUserProfile()?.email)
    }

    // 12. Logout clears access token, refresh token and cached profile
    @Test
    fun test_logout_clears_access_token_refresh_token_and_cached_profile() = runBlocking {
        authRepository.login("teacher@educhat.edu", "password123", UserRole.TEACHER)
        assertNotNull(sessionManager.getAccessToken())
        assertNotNull(sessionManager.getRefreshToken())
        assertNotNull(sessionManager.getUserProfile())
        assertNotNull(sessionManager.getSession())

        authRepository.logout()

        assertNull("Access token must be null after logout", sessionManager.getAccessToken())
        assertNull("Refresh token must be null after logout", sessionManager.getRefreshToken())
        assertNull("User profile must be null after logout", sessionManager.getUserProfile())
        assertNull("Session must be null after logout", sessionManager.getSession())
        assertFalse("hasActiveSession must return false", sessionManager.hasActiveSession())
    }

    // 13. New login after logout reaches Authenticated in ViewModel
    @Test
    fun test_new_login_after_logout_reaches_authenticated_in_viewmodel() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = AuthViewModel(app)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        // Fill credentials & login
        viewModel.fillDemoCredentials("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        viewModel.login()

        var attempts = 0
        while (viewModel.uiState.value !is AuthUiState.Authenticated && attempts < 50) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            kotlinx.coroutines.delay(20)
            attempts++
        }

        // Wait/assert authenticated
        assertTrue("ViewModel should reach Authenticated", viewModel.uiState.value is AuthUiState.Authenticated)
        val firstSession = (viewModel.uiState.value as AuthUiState.Authenticated).session
        assertEquals(UserRole.OFFICER_ADMIN, firstSession.profile.userRole)

        // Logout
        viewModel.logout()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertEquals("State should immediately be Idle after logout", AuthUiState.Idle, viewModel.uiState.value)
        assertFalse(sessionManager.hasActiveSession())

        // Immediate Re-login without restart
        viewModel.fillDemoCredentials("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        viewModel.login()

        attempts = 0
        while (viewModel.uiState.value !is AuthUiState.Authenticated && attempts < 50) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            kotlinx.coroutines.delay(20)
            attempts++
        }

        assertTrue("ViewModel should reach Authenticated again on immediate re-login", viewModel.uiState.value is AuthUiState.Authenticated)
        val secondSession = (viewModel.uiState.value as AuthUiState.Authenticated).session
        assertEquals("admin@educhat.edu", secondSession.profile.email)
    }

    // 14. Role mismatch rejection still works after logout/login cycle
    @Test
    fun test_role_mismatch_rejection_after_logout_cycle() = runBlocking {
        authRepository.login("teacher@educhat.edu", "password123", UserRole.TEACHER)
        authRepository.logout()

        val mismatchLogin = authRepository.login("teacher@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        assertTrue(mismatchLogin is AuthResult.Error)
        assertEquals("The selected role does not match your account.", (mismatchLogin as AuthResult.Error).message)
        assertFalse(authRepository.isUserLoggedIn())
    }

    // 15. Inactive account rejection still works after logout/login cycle
    @Test
    fun test_inactive_account_rejection_after_logout_cycle() = runBlocking {
        authRepository.login("teacher@educhat.edu", "password123", UserRole.TEACHER)
        authRepository.logout()

        val inactiveLogin = authRepository.login("inactive@educhat.edu", "password123", UserRole.STUDENT)
        assertTrue(inactiveLogin is AuthResult.Error)
        assertEquals("This account has been deactivated. Please contact your administrator.", (inactiveLogin as AuthResult.Error).message)
        assertFalse(authRepository.isUserLoggedIn())
    }

    // 16. Refresh token updates tokens and preserves user profile
    @Test
    fun test_refresh_token_updates_tokens_and_preserves_profile() = runBlocking {
        val loginRes = authRepository.login("admin@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        assertTrue(loginRes is AuthResult.Success)
        val initialSession = (loginRes as AuthResult.Success).session

        val refreshResult = authRepository.refreshToken()
        assertTrue("refreshToken should succeed", refreshResult.isSuccess)
        val refreshedSession = refreshResult.getOrThrow()

        assertEquals(initialSession.profile.id, refreshedSession.profile.id)
        assertEquals(initialSession.profile.email, refreshedSession.profile.email)
        assertEquals(initialSession.profile.role, refreshedSession.profile.role)
        assertTrue(sessionManager.hasActiveSession())
    }

    // 17. Refresh token fails when no session exists
    @Test
    fun test_refresh_token_fails_when_no_session() = runBlocking {
        sessionManager.clearSession()
        val refreshResult = authRepository.refreshToken()
        assertTrue(refreshResult.isFailure)
        assertFalse(sessionManager.hasActiveSession())
    }

    // A. Valid Student email + valid password -> Student login succeeds
    @Test
    fun test_student_login_valid_credentials_success() = runBlocking {
        val result = authRepository.login("student@educhat.edu", "password123", UserRole.STUDENT)
        assertTrue("Student login with valid credentials should succeed", result is AuthResult.Success)
        val session = (result as AuthResult.Success).session
        assertEquals("student@educhat.edu", session.profile.email)
        assertEquals("student", session.profile.role)
        assertEquals(UserRole.STUDENT, session.profile.userRole)
        assertTrue(session.profile.isActive)
        assertTrue(sessionManager.hasActiveSession())
    }

    // B. Valid Student email + wrong password -> login fails
    @Test
    fun test_student_login_wrong_password_fails() = runBlocking {
        val result = authRepository.login("student@educhat.edu", "wrong_password", UserRole.STUDENT)
        assertTrue("Student login with wrong password should fail", result is AuthResult.Error)
        val error = result as AuthResult.Error
        assertEquals("Invalid email or password.", error.message)
        assertFalse(sessionManager.hasActiveSession())
    }

    // C. Student account + Officer Admin selected -> login fails with role mismatch
    @Test
    fun test_student_login_with_officer_admin_role_mismatch() = runBlocking {
        val result = authRepository.login("student@educhat.edu", "password123", UserRole.OFFICER_ADMIN)
        assertTrue("Student login as Officer Admin should fail with role mismatch", result is AuthResult.Error)
        val error = result as AuthResult.Error
        assertEquals("The selected role does not match your account.", error.message)
        assertFalse(sessionManager.hasActiveSession())
    }

    // D. Student account + School Admin selected -> login fails with role mismatch
    @Test
    fun test_student_login_with_school_admin_role_mismatch() = runBlocking {
        val result = authRepository.login("student@educhat.edu", "password123", UserRole.SCHOOL_ADMIN)
        assertTrue("Student login as School Admin should fail with role mismatch", result is AuthResult.Error)
        val error = result as AuthResult.Error
        assertEquals("The selected role does not match your account.", error.message)
        assertFalse(sessionManager.hasActiveSession())
    }

    // E. Student account + Teacher selected -> login fails with role mismatch
    @Test
    fun test_student_login_with_teacher_role_mismatch() = runBlocking {
        val result = authRepository.login("student@educhat.edu", "password123", UserRole.TEACHER)
        assertTrue("Student login as Teacher should fail with role mismatch", result is AuthResult.Error)
        val error = result as AuthResult.Error
        assertEquals("The selected role does not match your account.", error.message)
        assertFalse(sessionManager.hasActiveSession())
    }

    // F. Inactive Student -> login fails
    @Test
    fun test_inactive_student_login_fails() = runBlocking {
        val result = authRepository.login("inactive@educhat.edu", "password123", UserRole.STUDENT)
        assertTrue("Inactive student login should fail", result is AuthResult.Error)
        val error = result as AuthResult.Error
        assertEquals("This account has been deactivated. Please contact your administrator.", error.message)
        assertFalse(sessionManager.hasActiveSession())
    }

    // G. Valid Student -> session persistence and dashboard routing data intact
    @Test
    fun test_student_session_persistence_and_routing_data() = runBlocking {
        val result = authRepository.login("student@educhat.edu", "password123", UserRole.STUDENT)
        assertTrue(result is AuthResult.Success)

        // Verify fresh SessionManager instance retrieves student session without data loss
        val freshSessionManager = SessionManager(context)
        val savedSession = freshSessionManager.getSession()
        assertNotNull("Saved student session should not be null", savedSession)
        assertEquals(UserRole.STUDENT, savedSession?.profile?.userRole)
        assertEquals("student", savedSession?.profile?.role)
        assertTrue(savedSession?.profile?.isActive == true)
    }
}
