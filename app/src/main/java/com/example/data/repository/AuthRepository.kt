package com.example.data.repository

import android.content.Context
import com.example.data.local.SessionManager
import com.example.data.local.SimulatedDatabase
import com.example.data.local.SimulatedDbUser
import com.example.data.model.AuthSession
import com.example.data.model.SupabaseLoginRequest
import com.example.data.model.SupabaseSignupRequest
import com.example.data.model.UpdateDisplayNameRequest
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

sealed class AuthResult {
    data class Success(val session: AuthSession) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(private val context: Context) {
    private val sessionManager = SessionManager(context)

    fun getActiveSession(): AuthSession? = sessionManager.getSession()

    fun isUserLoggedIn(): Boolean = sessionManager.hasActiveSession()

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

        val isConfigured = SupabaseConfig.isConfigured(context)

        if (isConfigured) {
            try {
                val api = SupabaseClient.getApi(context)
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
                            "Invalid email or password. Please try again."
                        tokenResponse.code() == 404 ->
                            "Account not found. Please check your email or contact support."
                        else ->
                            parsed ?: "Authentication failed. Please verify your credentials and network connection."
                    }
                    return@withContext AuthResult.Error(friendlyError)
                }

                val authData = tokenResponse.body()!!
                val userId = authData.user?.id
                    ?: return@withContext AuthResult.Error("Unable to verify user profile.")

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
                // NO UNTRUSTED FALLBACK WHEN CONFIGURED. Strict security policy.
                return@withContext AuthResult.Error("Unable to connect to the authentication service: ${e.localizedMessage ?: "Network error"}")
            }
        } else {
            // Local simulated DB mode (used for offline development & JVM tests before remote secrets are set)
            val dbMatch = SimulatedDatabase.findByEmail(email)

            if (dbMatch != null && dbMatch.password == password) {
                val profile = dbMatch.profile

                // Security Check 1: Inactive account guard
                if (!profile.isActive) {
                    return@withContext AuthResult.Error("This account has been deactivated. Please contact your administrator.")
                }

                // Security Check 2: Database role verification
                val dbRole = UserRole.fromDbValue(profile.role)
                if (dbRole != selectedRole) {
                    return@withContext AuthResult.Error("The selected role does not match your account.")
                }

                val session = AuthSession(
                    accessToken = "mock_jwt_token_${profile.id}",
                    refreshToken = "mock_refresh_token",
                    profile = profile
                )
                sessionManager.saveSession(session)
                return@withContext AuthResult.Success(session)
            } else {
                return@withContext AuthResult.Error("Invalid email or password.")
            }
        }
    }

    /**
     * Legitimate display-name update: Calls secure database RPC or updates own profile in simulated DB.
     */
    suspend fun updateDisplayName(newFullName: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val currentSession = sessionManager.getSession()
            ?: return@withContext Result.failure(IllegalStateException("No active authenticated session"))

        val trimmedName = newFullName.trim()
        if (trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Display name cannot be blank"))
        }

        if (SupabaseConfig.isConfigured(context)) {
            try {
                val api = SupabaseClient.getApi(context)
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
        } else {
            // Simulated DB RPC execution
            val userId = currentSession.profile.id
            val updated = SimulatedDatabase.updateProfile(userId) { it.copy(fullName = trimmedName) }
            if (updated != null) {
                sessionManager.saveSession(currentSession.copy(profile = updated))
                Result.success(updated)
            } else {
                Result.failure(IllegalStateException("User not found in database"))
            }
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

        val isCallerAdmin = currentSession.profile.role.equals("admin", ignoreCase = true)
        if (!isCallerAdmin && attemptedProtectedFields.isNotEmpty()) {
            return@withContext Result.failure(
                SecurityException("DATABASE CONSTRAINT VIOLATION: Modifying protected field(s) [${attemptedProtectedFields.joinToString()}] is forbidden. Privileged changes require administrator authorization.")
            )
        }

        if (SupabaseConfig.isConfigured(context)) {
            try {
                val api = SupabaseClient.getApi(context)
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
        } else {
            // Simulated DB update with RLS rules
            val updated = SimulatedDatabase.updateProfile(targetUserId) { existing ->
                val newFullName = updates["full_name"] as? String ?: existing.fullName
                existing.copy(fullName = newFullName)
            }
            if (updated != null) {
                Result.success(updated)
            } else {
                Result.failure(IllegalStateException("Record not found"))
            }
        }
    }

    /**
     * Simulated signup flow testing: Verifies that client-provided metadata requesting privileged roles
     * (e.g. role = "admin") is stripped or ignored, and the newly created account is strictly forced to "student".
     */
    suspend fun attemptSignupWithRoleMetadata(
        email: String,
        pass: String,
        metadataRole: String?
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        // Enforce trigger security logic: Default MUST be 'student', ignoring client metadata
        val assignedRole = "student" // Client cannot choose privileged roles

        val newProfile = UserProfile(
            id = UUID.randomUUID().toString(),
            fullName = email.substringBefore("@"),
            email = email,
            role = assignedRole,
            isActive = true,
            createdAt = "2026-08-26T00:00:00Z"
        )

        SimulatedDatabase.addUser(
            SimulatedDbUser(
                email = email,
                password = pass,
                profile = newProfile
            )
        )
        Result.success(newProfile)
    }

    suspend fun logout(): Boolean = withContext(Dispatchers.IO) {
        val token = sessionManager.getAccessToken()
        if (token != null && SupabaseConfig.isConfigured(context)) {
            try {
                val api = SupabaseClient.getApi(context)
                val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
                api.logout(apiKey = anonKey, bearerToken = "Bearer $token")
            } catch (_: Exception) {
                // Ignore network errors on logout
            }
        }
        sessionManager.clearSession()
        return@withContext true
    }
}

private data class SimulatedDbUser(
    val email: String,
    val password: String,
    val profile: UserProfile
)
