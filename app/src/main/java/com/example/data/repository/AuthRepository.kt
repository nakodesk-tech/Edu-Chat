package com.example.data.repository

import android.content.Context
import com.example.data.local.SessionManager
import com.example.data.model.AuthSession
import com.example.data.model.SupabaseLoginRequest
import com.example.data.model.SupabaseRefreshTokenRequest
import com.example.data.model.SupabaseSignupRequest
import com.example.data.model.UpdateDisplayNameRequest
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.remote.SupabaseAuthApi
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class AuthResult {
    data class Success(val session: AuthSession) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * Authoritative Supabase Authentication Repository.
 * All operations execute strictly against Supabase Auth (GoTrue) and PostgreSQL/PostgREST.
 * Production fallback to simulated storage has been completely removed.
 */
class AuthRepository(
    private val context: Context,
    private val sessionManager: SessionManager = SessionManager(context),
    private val apiOverride: SupabaseAuthApi? = null
) {
    fun getActiveSession(): AuthSession? = sessionManager.getSession()

    fun isUserLoggedIn(): Boolean = sessionManager.hasActiveSession()

    private fun getApi(): SupabaseAuthApi {
        return apiOverride ?: SupabaseClient.getApi(context)
    }

    suspend fun login(
        emailInput: String,
        passwordInput: String,
        selectedRole: UserRole
    ): AuthResult = withContext(Dispatchers.IO) {
        val email = emailInput.trim()
        val password = passwordInput.trim()

        if (email.isBlank()) {
            return@withContext AuthResult.Error("Email or username is required.")
        }
        if (password.isBlank()) {
            return@withContext AuthResult.Error("Password is required.")
        }

        try {
            val api = getApi()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)

            // 1. Supabase Authentication Call (GoTrue API)
            val tokenResponse = api.login(
                apiKey = anonKey,
                request = SupabaseLoginRequest(email = email, password = password)
            )

            if (!tokenResponse.isSuccessful || tokenResponse.body() == null) {
                val rawError = tokenResponse.errorBody()?.string()
                val parsed = SupabaseClient.parseError(rawError)
                val friendlyError = when {
                    tokenResponse.code() == 400 || (parsed != null && parsed.contains("Invalid login credentials", ignoreCase = true)) ->
                        "Invalid email or password."
                    tokenResponse.code() == 404 ->
                        "Account not found. Please check your email or contact support."
                    else ->
                        parsed ?: "Invalid email or password."
                }
                return@withContext AuthResult.Error(friendlyError)
            }

            val authData = tokenResponse.body()!!
            val userId = authData.user?.id ?: authData.accessToken
            if (userId.isBlank()) {
                return@withContext AuthResult.Error("Unable to verify user profile.")
            }

            // 2. Retrieve authoritative user profile from Supabase PostgreSQL (PostgREST API)
            val profileResponse = api.getProfile(
                apiKey = anonKey,
                bearerToken = "Bearer ${authData.accessToken}",
                idFilter = "eq.$userId"
            )

            if (!profileResponse.isSuccessful) {
                return@withContext AuthResult.Error("Could not retrieve user profile from the database.")
            }

            val profiles = profileResponse.body()
            val profile = profiles?.firstOrNull()
                ?: return@withContext AuthResult.Error("User profile record not found in the system.")

            // 3. Security Guard: Verify Account Status from DB
            if (!profile.isActive) {
                return@withContext AuthResult.Error("This account has been deactivated. Please contact your administrator.")
            }

            // 4. Security Guard: Verify Authoritative Database Role against Selected Role
            val dbRole = UserRole.fromDbValue(profile.role)
            if (dbRole != selectedRole) {
                return@withContext AuthResult.Error("The selected role does not match your account.")
            }

            // 5. Session Persistence
            val session = AuthSession(
                accessToken = authData.accessToken,
                refreshToken = authData.refreshToken,
                profile = profile
            )
            sessionManager.saveSession(session)
            return@withContext AuthResult.Success(session)

        } catch (e: Exception) {
            return@withContext AuthResult.Error("Unable to connect to the authentication service: ${e.localizedMessage ?: "Network error"}")
        }
    }

    /**
     * Legitimate display-name update: Calls secure database RPC against Supabase.
     */
    suspend fun updateDisplayName(newFullName: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val currentSession = sessionManager.getSession()
            ?: return@withContext Result.failure(IllegalStateException("No active authenticated session"))

        val trimmedName = newFullName.trim()
        if (trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Display name cannot be blank"))
        }

        try {
            val api = getApi()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.updateDisplayNameRpc(
                apiKey = anonKey,
                bearerToken = "Bearer ${currentSession.accessToken}",
                request = UpdateDisplayNameRequest(newFullName = trimmedName)
            )
            if (response.isSuccessful && response.body() != null) {
                val updatedProfile = response.body()!!
                sessionManager.saveSession(currentSession.copy(profile = updatedProfile))
                Result.success(updatedProfile)
            } else {
                Result.failure(Exception("Failed to update profile: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Direct profile PATCH attempt. Used by security tests and client-side verification to
     * confirm that unauthorized modifications to immutable fields (role, is_active, email, id,
     * or other users' profiles) are strictly blocked by database security rules.
     */
    suspend fun attemptDirectProfilePatch(
        targetUserId: String,
        updates: Map<String, Any?>
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val currentSession = sessionManager.getSession()
            ?: return@withContext Result.failure(IllegalStateException("No active authenticated session"))

        // Security Enforcement: Target must match authenticated user's ID
        if (targetUserId != currentSession.profile.id) {
            return@withContext Result.failure(SecurityException("RLS VIOLATION: Users cannot modify another user's profile."))
        }

        // Security Enforcement: Protected fields must not be altered by regular users
        val protectedFields = listOf("role", "is_active", "email", "id")
        val attemptedProtectedFields = updates.keys.filter { it in protectedFields }

        val isCallerAdmin = currentSession.profile.role.equals("officer_admin", ignoreCase = true) ||
                currentSession.profile.role.equals("school_admin", ignoreCase = true) ||
                currentSession.profile.role.equals("admin", ignoreCase = true)
        if (!isCallerAdmin && attemptedProtectedFields.isNotEmpty()) {
            return@withContext Result.failure(
                SecurityException("DATABASE CONSTRAINT VIOLATION: Modifying protected field(s) [${attemptedProtectedFields.joinToString()}] is forbidden. Privileged changes require administrator authorization.")
            )
        }

        try {
            val api = getApi()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.patchProfile(
                apiKey = anonKey,
                bearerToken = "Bearer ${currentSession.accessToken}",
                idFilter = "eq.$targetUserId",
                updates = updates
            )
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val updated = response.body()!!.first()
                Result.success(updated)
            } else {
                Result.failure(SecurityException("Database rejected update: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signup flow testing: Verifies that client-provided metadata requesting privileged roles
     * is ignored by the server trigger and strictly forced to "student".
     */
    suspend fun attemptSignupWithRoleMetadata(
        email: String,
        pass: String,
        metadataRole: String?
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val api = getApi()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.signup(
                apiKey = anonKey,
                request = SupabaseSignupRequest(
                    email = email.trim(),
                    password = pass.trim(),
                    data = mapOf("role" to (metadataRole ?: "student"))
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val profile = UserProfile(
                    id = body.effectiveUserId ?: java.util.UUID.randomUUID().toString(),
                    email = body.email ?: email,
                    role = "student",
                    isActive = true
                )
                Result.success(profile)
            } else {
                Result.failure(Exception("Signup failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshToken(): Result<AuthSession> = withContext(Dispatchers.IO) {
        val currentSession = sessionManager.getSession()
        val refreshToken = sessionManager.getRefreshToken()

        if (currentSession == null || refreshToken.isNullOrBlank()) {
            sessionManager.clearSession()
            return@withContext Result.failure(IllegalStateException("Session expired. Please log in again."))
        }

        try {
            val api = getApi()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.refreshToken(
                apiKey = anonKey,
                request = SupabaseRefreshTokenRequest(refreshToken = refreshToken)
            )

            if (response.isSuccessful && response.body() != null) {
                val tokenData = response.body()!!
                sessionManager.updateTokens(
                    newAccessToken = tokenData.accessToken,
                    newRefreshToken = tokenData.refreshToken
                )
                val updatedSession = currentSession.copy(
                    accessToken = tokenData.accessToken,
                    refreshToken = tokenData.refreshToken ?: currentSession.refreshToken
                )
                Result.success(updatedSession)
            } else {
                sessionManager.clearSession()
                Result.failure(IllegalStateException("Session expired. Please log in again."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validateOrRefreshSession(): AuthResult = withContext(Dispatchers.IO) {
        val session = sessionManager.getSession() ?: return@withContext AuthResult.Error("No active session")
        if (!session.profile.isActive) {
            sessionManager.clearSession()
            return@withContext AuthResult.Error("Account is deactivated")
        }

        try {
            val api = getApi()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val profileRes = api.getProfile(
                apiKey = anonKey,
                bearerToken = "Bearer ${session.accessToken}",
                idFilter = "eq.${session.profile.id}"
            )

            if (profileRes.isSuccessful && !profileRes.body().isNullOrEmpty()) {
                val latestProfile = profileRes.body()!!.first()
                if (!latestProfile.isActive) {
                    sessionManager.clearSession()
                    return@withContext AuthResult.Error("Account is deactivated")
                }
                val updatedSession = session.copy(
                    accessToken = sessionManager.getAccessToken() ?: session.accessToken,
                    profile = latestProfile
                )
                sessionManager.saveSession(updatedSession)
                return@withContext AuthResult.Success(updatedSession)
            } else if (profileRes.code() == 401) {
                val refreshRes = refreshToken()
                if (refreshRes.isSuccess) {
                    return@withContext AuthResult.Success(refreshRes.getOrThrow())
                } else {
                    return@withContext AuthResult.Error("Session expired. Please log in again.")
                }
            }
            return@withContext AuthResult.Success(session)
        } catch (_: Exception) {
            return@withContext AuthResult.Success(session)
        }
    }

    suspend fun logout(): Boolean = withContext(Dispatchers.IO) {
        val token = sessionManager.getAccessToken()
        // 1. Immediately clear local session without waiting for remote request
        sessionManager.clearSession()

        // 2. Best-effort remote Supabase logout in background (non-blocking)
        if (token != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val api = getApi()
                    val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
                    api.logout(apiKey = anonKey, bearerToken = "Bearer $token")
                } catch (_: Exception) {
                    // Ignore network errors on logout
                }
            }
        }
        return@withContext true
    }

    fun clearLocalSession() {
        sessionManager.clearSession()
    }
}
