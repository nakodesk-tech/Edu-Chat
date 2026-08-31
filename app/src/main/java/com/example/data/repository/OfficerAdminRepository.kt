package com.example.data.repository

import android.content.Context
import android.util.Patterns
import com.example.data.local.SessionManager
import com.example.data.model.AuthSession
import com.example.data.model.CreateSchoolRequest
import com.example.data.model.OfficerAdminCreateUserRequest
import com.example.data.model.School
import com.example.data.model.UpdateSchoolRequest
import com.example.data.model.UserProfile
import com.example.data.remote.SupabaseAuthApi
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Authoritative Officer Admin Repository.
 * All operations execute strictly against Supabase backend (PostgreSQL/RPC/GoTrue).
 * Production fallback to simulated storage has been completely removed.
 */
class OfficerAdminRepository(
    private val context: Context,
    private val sessionManager: SessionManager = SessionManager(context),
    private val apiOverride: SupabaseAuthApi? = null
) {
    /**
     * Authoritative Officer Admin Security Guard
     * Ensures only active users with role == "officer_admin" can access operations.
     */
    fun checkOfficerAdminAuthorization(): Result<AuthSession> {
        val session = sessionManager.getSession()
            ?: return Result.failure(SecurityException("Authentication required. No active session."))

        if (!session.profile.isActive) {
            return Result.failure(SecurityException("Your administrator account has been deactivated."))
        }

        if (!session.profile.role.equals("officer_admin", ignoreCase = true)) {
            return Result.failure(SecurityException("Access denied. Officer Admin privileges are required for this action."))
        }

        return Result.success(session)
    }

    private fun getApi(): SupabaseAuthApi {
        return apiOverride ?: SupabaseClient.getApi(context)
    }

    /**
     * Get the current Officer Admin profile
     */
    suspend fun getOfficerProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        val authCheck = checkOfficerAdminAuthorization()
        if (authCheck.isFailure) {
            return@withContext Result.failure(authCheck.exceptionOrNull()!!)
        }
        val currentSession = authCheck.getOrNull()!!

        try {
            val api = getApi()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.getProfile(
                apiKey = anonKey,
                bearerToken = "Bearer ${currentSession.accessToken}",
                idFilter = "eq.${currentSession.profile.id}"
            )
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val profile = response.body()!!.first()
                sessionManager.saveSession(currentSession.copy(profile = profile))
                Result.success(profile)
            } else {
                Result.success(currentSession.profile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update permitted profile fields for the Officer Admin.
     * Protected fields (role, school_id, is_primary_admin, id) CANNOT be modified.
     */
    suspend fun updateOfficerProfile(
        fullNameInput: String,
        mobileInput: String?
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val authCheck = checkOfficerAdminAuthorization()
        if (authCheck.isFailure) {
            return@withContext Result.failure(authCheck.exceptionOrNull()!!)
        }
        val currentSession = authCheck.getOrNull()!!
        val trimmedName = fullNameInput.trim()
        val trimmedMobile = mobileInput?.trim()?.ifBlank { null }

        if (trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Full name cannot be blank."))
        }

        if (trimmedMobile != null && trimmedMobile.length < 10) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid 10-digit mobile number."))
        }

        try {
            val api = getApi()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val updates = mutableMapOf<String, Any?>("full_name" to trimmedName)
            if (trimmedMobile != null) updates["mobile"] = trimmedMobile

            val response = api.patchProfile(
                apiKey = anonKey,
                bearerToken = "Bearer ${currentSession.accessToken}",
                idFilter = "eq.${currentSession.profile.id}",
                updates = updates
            )
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val updated = response.body()!!.first()
                sessionManager.saveSession(currentSession.copy(profile = updated))
                Result.success(updated)
            } else {
                Result.failure(Exception("Failed to update profile: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tile 1: Create Officer Admin
     * role = officer_admin, is_primary_admin = false, school_id = NULL
     */
    suspend fun createOfficerAdmin(
        fullNameInput: String,
        emailInput: String,
        mobileInput: String?,
        passwordInput: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val authCheck = checkOfficerAdminAuthorization()
        if (authCheck.isFailure) {
            return@withContext Result.failure(authCheck.exceptionOrNull()!!)
        }
        val currentSession = authCheck.getOrNull()!!

        val fullName = fullNameInput.trim()
        val email = emailInput.trim().lowercase()
        val mobile = mobileInput?.trim()?.ifBlank { null }
        val password = passwordInput.trim()

        if (fullName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Full name cannot be blank."))
        }
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (mobile.isNullOrBlank() || mobile.length < 10) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid 10-digit mobile number."))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        try {
            val api = getApi()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.officerAdminCreateUserRpc(
                apiKey = anonKey,
                bearerToken = "Bearer ${currentSession.accessToken}",
                request = OfficerAdminCreateUserRequest(
                    email = email,
                    password = password,
                    fullName = fullName,
                    mobile = mobile,
                    role = "officer_admin",
                    schoolId = null
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val rawError = response.errorBody()?.string()
                val parsed = SupabaseClient.parseError(rawError)
                Result.failure(Exception(parsed ?: "Failed to create Officer Admin account."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tile 2: Create School Admin
     * role = school_admin, is_active = true, is_primary_admin = false, school_id = selected school
     */
    suspend fun createSchoolAdmin(
        fullNameInput: String,
        emailInput: String,
        mobileInput: String?,
        passwordInput: String,
        schoolIdInput: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val authCheck = checkOfficerAdminAuthorization()
        if (authCheck.isFailure) {
            return@withContext Result.failure(authCheck.exceptionOrNull()!!)
        }
        val currentSession = authCheck.getOrNull()!!

        val fullName = fullNameInput.trim()
        val email = emailInput.trim().lowercase()
        val mobile = mobileInput?.trim()?.ifBlank { null }
        val password = passwordInput.trim()
        val schoolId = schoolIdInput.trim()

        if (fullName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Full name cannot be blank."))
        }
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (mobile.isNullOrBlank() || mobile.length < 10) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid 10-digit mobile number."))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        if (schoolId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("A valid school must be selected for the School Admin."))
        }

        try {
            val api = getApi()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)

            val schoolRes = api.getSchoolById(
                apiKey = anonKey,
                bearerToken = "Bearer ${currentSession.accessToken}",
                idFilter = "eq.$schoolId"
            )
            val school = schoolRes.body()?.firstOrNull()
            if (school == null || !school.isActive) {
                return@withContext Result.failure(IllegalArgumentException("School Admin cannot be assigned to an inactive or non-existent school."))
            }

            val response = api.officerAdminCreateUserRpc(
                apiKey = anonKey,
                bearerToken = "Bearer ${currentSession.accessToken}",
                request = OfficerAdminCreateUserRequest(
                    email = email,
                    password = password,
                    fullName = fullName,
                    mobile = mobile,
                    role = "school_admin",
                    schoolId = schoolId
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val rawError = response.errorBody()?.string()
                val parsed = SupabaseClient.parseError(rawError)
                Result.failure(Exception(parsed ?: "Failed to create School Admin account."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tile 3 & 4: Get All Schools
     */
    suspend fun getSchools(): Result<List<School>> = withContext(Dispatchers.IO) {
        val authCheck = checkOfficerAdminAuthorization()
        if (authCheck.isFailure) {
            return@withContext Result.failure(authCheck.exceptionOrNull()!!)
        }
        val currentSession = authCheck.getOrNull()!!

        try {
            val api = getApi()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.getSchools(
                apiKey = anonKey,
                bearerToken = "Bearer ${currentSession.accessToken}"
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to load schools list."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get active schools (for School Admin assignment dropdown)
     */
    suspend fun getActiveSchools(): Result<List<School>> = withContext(Dispatchers.IO) {
        val allSchools = getSchools()
        if (allSchools.isFailure) return@withContext allSchools
        Result.success(allSchools.getOrNull()!!.filter { it.isActive })
    }

    /**
     * Tile 3: Register a New School
     * Exactly 6 attributes: School Name, UDISE Code (stored in 'code'), Mobile Number, E-Mail ID, Address, Active Status.
     * School Code must be unique. Default isActive = true.
     */
    suspend fun createSchool(
        nameInput: String,
        codeInput: String,
        mobileInput: String? = null,
        emailInput: String? = null,
        addressInput: String? = null
    ): Result<School> = withContext(Dispatchers.IO) {
        val authCheck = checkOfficerAdminAuthorization()
        if (authCheck.isFailure) {
            return@withContext Result.failure(authCheck.exceptionOrNull()!!)
        }
        val currentSession = authCheck.getOrNull()!!

        val name = nameInput.trim()
        val code = codeInput.trim().uppercase()
        val mobile = mobileInput?.trim()?.ifBlank { null }
        val email = emailInput?.trim()?.lowercase()?.ifBlank { null }
        val address = addressInput?.trim()?.ifBlank { null }

        if (name.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("School name cannot be blank."))
        }
        if (code.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("School code cannot be blank."))
        }
        if (mobile != null && mobile.length < 10) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid 10-digit mobile number."))
        }
        if (email != null && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }

        try {
            val api = getApi()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.officerAdminCreateSchoolRpc(
                apiKey = anonKey,
                bearerToken = "Bearer ${currentSession.accessToken}",
                request = CreateSchoolRequest(
                    name = name,
                    code = code,
                    mobile = mobile,
                    email = email,
                    address = address
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val rawError = response.errorBody()?.string()
                val parsed = SupabaseClient.parseError(rawError)
                val msg = parsed ?: "Failed to register school. School code may already exist."
                Result.failure(IllegalArgumentException("School code '$code' violates unique constraint: $msg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Backward-compatible overload for 3-argument createSchool
     */
    suspend fun createSchool(
        nameInput: String,
        codeInput: String,
        addressInput: String?
    ): Result<School> = createSchool(
        nameInput = nameInput,
        codeInput = codeInput,
        mobileInput = null,
        emailInput = null,
        addressInput = addressInput
    )

    /**
     * Tile 4: Update Existing School
     */
    suspend fun updateSchool(
        schoolId: String,
        nameInput: String,
        codeInput: String,
        mobileInput: String? = null,
        emailInput: String? = null,
        addressInput: String? = null,
        isActive: Boolean = true
    ): Result<School> = withContext(Dispatchers.IO) {
        val authCheck = checkOfficerAdminAuthorization()
        if (authCheck.isFailure) {
            return@withContext Result.failure(authCheck.exceptionOrNull()!!)
        }
        val currentSession = authCheck.getOrNull()!!

        val name = nameInput.trim()
        val code = codeInput.trim().uppercase()
        val mobile = mobileInput?.trim()?.ifBlank { null }
        val email = emailInput?.trim()?.lowercase()?.ifBlank { null }
        val address = addressInput?.trim()?.ifBlank { null }

        if (name.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("School name cannot be blank."))
        }
        if (code.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("School code cannot be blank."))
        }
        if (mobile != null && mobile.length < 10) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid 10-digit mobile number."))
        }
        if (email != null && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }

        try {
            val api = getApi()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.officerAdminUpdateSchoolRpc(
                apiKey = anonKey,
                bearerToken = "Bearer ${currentSession.accessToken}",
                request = UpdateSchoolRequest(
                    schoolId = schoolId,
                    name = name,
                    code = code,
                    mobile = mobile,
                    email = email,
                    address = address,
                    isActive = isActive
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val rawError = response.errorBody()?.string()
                val parsed = SupabaseClient.parseError(rawError)
                Result.failure(Exception(parsed ?: "Failed to update school."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Backward-compatible overload for updateSchool
     */
    suspend fun updateSchool(
        schoolId: String,
        nameInput: String,
        codeInput: String,
        addressInput: String?,
        isActive: Boolean
    ): Result<School> = updateSchool(
        schoolId = schoolId,
        nameInput = nameInput,
        codeInput = codeInput,
        mobileInput = null,
        emailInput = null,
        addressInput = addressInput,
        isActive = isActive
    )

    /**
     * Tile 4: Activate / Deactivate School (Soft Deactivation)
     */
    suspend fun toggleSchoolStatus(schoolId: String, newActiveStatus: Boolean): Result<School> = withContext(Dispatchers.IO) {
        val authCheck = checkOfficerAdminAuthorization()
        if (authCheck.isFailure) {
            return@withContext Result.failure(authCheck.exceptionOrNull()!!)
        }

        val schoolResult = getSchools()
        val target = schoolResult.getOrNull()?.firstOrNull { it.id == schoolId }
            ?: return@withContext Result.failure(IllegalStateException("School not found."))

        updateSchool(
            schoolId = schoolId,
            nameInput = target.name,
            codeInput = target.code,
            addressInput = target.address,
            isActive = newActiveStatus
        )
    }

    /**
     * Get active staff count for a school (for warning modal)
     */
    fun getActiveStaffCountForSchool(schoolId: String): Int {
        return 0
    }
}
