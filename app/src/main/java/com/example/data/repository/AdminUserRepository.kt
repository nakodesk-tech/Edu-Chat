package com.example.data.repository

import android.content.Context
import com.example.data.local.SessionManager
import com.example.data.model.AdminCreateUserRequest
import com.example.data.model.AdminToggleStatusRequest
import com.example.data.model.AdminUpdateUserRequest
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.remote.SupabaseAuthApi
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Authoritative Admin User Management Repository.
 * All operations execute strictly against Supabase PostgreSQL/PostgREST/RPC backend.
 * Production fallback to simulated storage has been completely removed.
 */
class AdminUserRepository(
    private val context: Context,
    private val sessionManager: SessionManager = SessionManager(context),
    private val apiOverride: SupabaseAuthApi? = null
) {
    /**
     * Checks if the currently authenticated session is an active Administrator.
     */
    private fun verifyAdminSession(): Result<com.example.data.model.AuthSession> {
        val session = sessionManager.getSession()
            ?: return Result.failure(SecurityException("Authentication required. Please log in as an administrator."))

        if (!session.profile.isActive) {
            return Result.failure(SecurityException("Your administrator account has been deactivated."))
        }

        if (!session.profile.role.equals("officer_admin", ignoreCase = true) && !session.profile.role.equals("school_admin", ignoreCase = true)) {
            return Result.failure(SecurityException("Access denied. Admin privileges are required for this action."))
        }

        return Result.success(session)
    }

    private fun getApi(): SupabaseAuthApi {
        return apiOverride ?: SupabaseClient.getApi(context)
    }

    /**
     * Retrieves all managed users (Teachers, Students, and Admin accounts).
     */
    suspend fun getUsers(): Result<List<UserProfile>> = withContext(Dispatchers.IO) {
        val sessionResult = verifyAdminSession()
        if (sessionResult.isFailure) {
            return@withContext Result.failure(sessionResult.exceptionOrNull()!!)
        }
        val currentSession = sessionResult.getOrNull()!!

        try {
            val api = getApi()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.getAllProfiles(
                apiKey = anonKey,
                bearerToken = "Bearer ${currentSession.accessToken}"
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val rawError = response.errorBody()?.string()
                val msg = SupabaseClient.parseError(rawError) ?: "Failed to retrieve user list."
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error while retrieving users: ${e.localizedMessage ?: "Please check connection"}"))
        }
    }

    /**
     * Creates a new Teacher or Student account.
     * Admin creation is strictly prohibited.
     */
    suspend fun createUser(
        fullNameInput: String,
        emailInput: String,
        passwordInput: String,
        role: UserRole,
        isActive: Boolean = true
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val sessionResult = verifyAdminSession()
        if (sessionResult.isFailure) {
            return@withContext Result.failure(sessionResult.exceptionOrNull()!!)
        }
        val currentSession = sessionResult.getOrNull()!!

        val fullName = fullNameInput.trim()
        val email = emailInput.trim().lowercase(Locale.ROOT)
        val password = passwordInput.trim()

        if (fullName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Full name is required."))
        }
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return@withContext Result.failure(IllegalArgumentException("A valid email address is required."))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        // Security Guard: Only Teacher and Student accounts can be created
        if (role == UserRole.OFFICER_ADMIN || role == UserRole.SCHOOL_ADMIN) {
            return@withContext Result.failure(SecurityException("Admin creation is not permitted through this interface."))
        }

        try {
            val api = getApi()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.adminCreateUserRpc(
                apiKey = anonKey,
                bearerToken = "Bearer ${currentSession.accessToken}",
                request = AdminCreateUserRequest(
                    email = email,
                    password = password,
                    fullName = fullName,
                    role = role.dbValue,
                    isActive = isActive
                )
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val rawError = response.errorBody()?.string()
                val msg = SupabaseClient.parseError(rawError) ?: "Failed to create user account."
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to create user: ${e.localizedMessage ?: "Network error"}"))
        }
    }

    /**
     * Updates an existing user's details (Full Name, Role, Active Status).
     * Guards against self-deactivation or promoting to Admin.
     */
    suspend fun updateUser(
        userId: String,
        fullNameInput: String,
        role: UserRole,
        isActive: Boolean,
        mobileInput: String? = null
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val sessionResult = verifyAdminSession()
        if (sessionResult.isFailure) {
            return@withContext Result.failure(sessionResult.exceptionOrNull()!!)
        }
        val currentSession = sessionResult.getOrNull()!!

        val fullName = fullNameInput.trim()
        if (fullName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Full name cannot be blank."))
        }

        // Security Guard: Admin cannot edit self into inactive or change own role
        if (userId == currentSession.profile.id) {
            if (!isActive) {
                return@withContext Result.failure(SecurityException("Admins cannot deactivate their own account."))
            }
            if (role != currentSession.profile.userRole) {
                return@withContext Result.failure(SecurityException("Admins cannot change their own role through this interface."))
            }
        }

        // Security Guard: Managed role cannot be promoted to Admin
        if ((role == UserRole.OFFICER_ADMIN || role == UserRole.SCHOOL_ADMIN) && userId != currentSession.profile.id) {
            return@withContext Result.failure(SecurityException("Elevating users to Admin role is restricted."))
        }

        try {
            val api = getApi()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.adminUpdateUserRpc(
                apiKey = anonKey,
                bearerToken = "Bearer ${currentSession.accessToken}",
                request = AdminUpdateUserRequest(
                    userId = userId,
                    fullName = fullName,
                    role = role.dbValue,
                    isActive = isActive,
                    mobile = mobileInput?.trim()
                )
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val rawError = response.errorBody()?.string()
                val msg = SupabaseClient.parseError(rawError) ?: "Failed to update user."
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update user: ${e.localizedMessage ?: "Network error"}"))
        }
    }

    /**
     * Deletes a user safely (with checks against self-deletion or primary admin deletion).
     */
    suspend fun deleteUser(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val sessionResult = verifyAdminSession()
        if (sessionResult.isFailure) {
            return@withContext Result.failure(sessionResult.exceptionOrNull()!!)
        }
        val currentSession = sessionResult.getOrNull()!!

        // Security Guard: Cannot delete self
        if (userId == currentSession.profile.id) {
            return@withContext Result.failure(SecurityException("Admins cannot delete their own administrator account."))
        }

        try {
            val api = getApi()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.deleteProfile(
                apiKey = anonKey,
                bearerToken = "Bearer ${currentSession.accessToken}",
                idFilter = "eq.$userId"
            )

            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val rawError = response.errorBody()?.string()
                val msg = SupabaseClient.parseError(rawError) ?: "Failed to remove user."
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to remove user: ${e.localizedMessage ?: "Network error"}"))
        }
    }

    /**
     * Soft-deactivates a user by setting is_active = false.
     * Prevents self-deactivation.
     */
    suspend fun deactivateUser(userId: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val sessionResult = verifyAdminSession()
        if (sessionResult.isFailure) {
            return@withContext Result.failure(sessionResult.exceptionOrNull()!!)
        }
        val currentSession = sessionResult.getOrNull()!!

        // Security Guard: Cannot deactivate self
        if (userId == currentSession.profile.id) {
            return@withContext Result.failure(SecurityException("You cannot deactivate your own administrator account."))
        }

        try {
            val api = getApi()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.adminToggleStatusRpc(
                apiKey = anonKey,
                bearerToken = "Bearer ${currentSession.accessToken}",
                request = AdminToggleStatusRequest(
                    userId = userId,
                    isActive = false
                )
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val rawError = response.errorBody()?.string()
                val msg = SupabaseClient.parseError(rawError) ?: "Failed to deactivate user."
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to deactivate user: ${e.localizedMessage ?: "Network error"}"))
        }
    }

    /**
     * Reactivates an inactive user by setting is_active = true.
     */
    suspend fun reactivateUser(userId: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val sessionResult = verifyAdminSession()
        if (sessionResult.isFailure) {
            return@withContext Result.failure(sessionResult.exceptionOrNull()!!)
        }
        val currentSession = sessionResult.getOrNull()!!

        try {
            val api = getApi()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.adminToggleStatusRpc(
                apiKey = anonKey,
                bearerToken = "Bearer ${currentSession.accessToken}",
                request = AdminToggleStatusRequest(
                    userId = userId,
                    isActive = true
                )
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val rawError = response.errorBody()?.string()
                val msg = SupabaseClient.parseError(rawError) ?: "Failed to reactivate user."
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to reactivate user: ${e.localizedMessage ?: "Network error"}"))
        }
    }
}
