package com.example.data.repository

import android.content.Context
import android.util.Patterns
import com.example.data.local.SessionManager
import com.example.data.local.SimulatedDatabase
import com.example.data.local.SimulatedDbUser
import com.example.data.model.AuthSession
import com.example.data.model.CreateSchoolRequest
import com.example.data.model.OfficerAdminCreateUserRequest
import com.example.data.model.School
import com.example.data.model.UpdateOfficerProfileRequest
import com.example.data.model.UpdateSchoolRequest
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class OfficerAdminRepository(private val context: Context) {
    private val sessionManager = SessionManager(context)

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

    /**
     * Get the current Officer Admin profile
     */
    suspend fun getOfficerProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        val authCheck = checkOfficerAdminAuthorization()
        if (authCheck.isFailure) {
            return@withContext Result.failure(authCheck.exceptionOrNull()!!)
        }
        val currentSession = authCheck.getOrNull()!!

        if (SupabaseConfig.isConfigured(context)) {
            try {
                val api = SupabaseClient.getApi(context)
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
        } else {
            val dbMatch = SimulatedDatabase.findById(currentSession.profile.id)
            if (dbMatch != null) {
                sessionManager.saveSession(currentSession.copy(profile = dbMatch.profile))
                Result.success(dbMatch.profile)
            } else {
                Result.success(currentSession.profile)
            }
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

        if (SupabaseConfig.isConfigured(context)) {
            try {
                val api = SupabaseClient.getApi(context)
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
        } else {
            val updated = SimulatedDatabase.updateProfile(currentSession.profile.id) { existing ->
                existing.copy(
                    fullName = trimmedName,
                    mobile = trimmedMobile ?: existing.mobile
                )
            }
            if (updated != null) {
                sessionManager.saveSession(currentSession.copy(profile = updated))
                Result.success(updated)
            } else {
                Result.failure(IllegalStateException("User profile not found."))
            }
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

        if (SupabaseConfig.isConfigured(context)) {
            try {
                val api = SupabaseClient.getApi(context)
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
        } else {
            // Simulated DB creation
            if (SimulatedDatabase.findByEmail(email) != null) {
                return@withContext Result.failure(IllegalArgumentException("An account with this email address already exists."))
            }

            val newProfile = UserProfile(
                id = UUID.randomUUID().toString(),
                fullName = fullName,
                email = email,
                mobile = mobile,
                role = "officer_admin",
                isActive = true,
                isPrimaryAdmin = false, // Strictly false for newly created officer admins
                schoolId = null, // Strictly NULL for officer admins
                createdAt = "2026-08-26T12:00:00Z",
                updatedAt = "2026-08-26T12:00:00Z"
            )

            SimulatedDatabase.addUser(
                SimulatedDbUser(
                    email = email,
                    password = password,
                    profile = newProfile
                )
            )

            Result.success(newProfile)
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

        // Verify school existence and active status
        if (!SupabaseConfig.isConfigured(context)) {
            val school = SimulatedDatabase.findSchoolById(schoolId)
                ?: return@withContext Result.failure(IllegalArgumentException("Selected school does not exist."))
            if (!school.isActive) {
                return@withContext Result.failure(IllegalStateException("Cannot assign a School Admin to an inactive school. Please activate the school first."))
            }
        }

        if (SupabaseConfig.isConfigured(context)) {
            try {
                val api = SupabaseClient.getApi(context)
                val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
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
        } else {
            if (SimulatedDatabase.findByEmail(email) != null) {
                return@withContext Result.failure(IllegalArgumentException("An account with this email address already exists."))
            }

            val newProfile = UserProfile(
                id = UUID.randomUUID().toString(),
                fullName = fullName,
                email = email,
                mobile = mobile,
                role = "school_admin",
                isActive = true,
                isPrimaryAdmin = false,
                schoolId = schoolId,
                createdAt = "2026-08-26T12:00:00Z",
                updatedAt = "2026-08-26T12:00:00Z"
            )

            SimulatedDatabase.addUser(
                SimulatedDbUser(
                    email = email,
                    password = password,
                    profile = newProfile
                )
            )

            Result.success(newProfile)
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

        if (SupabaseConfig.isConfigured(context)) {
            try {
                val api = SupabaseClient.getApi(context)
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
        } else {
            Result.success(SimulatedDatabase.getAllSchools())
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
     * School Code must be unique (enforced by DB constraint / RPC). Default isActive = true.
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

        if (SupabaseConfig.isConfigured(context)) {
            try {
                val api = SupabaseClient.getApi(context)
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
                    Result.failure(Exception(parsed ?: "Failed to register school. School code may already exist."))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            // Check for duplicate school code in simulated database
            val existing = SimulatedDatabase.findSchoolByCode(code)
            if (existing != null) {
                return@withContext Result.failure(IllegalArgumentException("School code '$code' is already registered. School code must be unique."))
            }

            val newSchool = School(
                id = UUID.randomUUID().toString(),
                name = name,
                code = code,
                mobile = mobile,
                email = email,
                address = address,
                isActive = true,
                createdAt = "2026-08-26T12:00:00Z",
                updatedAt = "2026-08-26T12:00:00Z"
            )

            try {
                SimulatedDatabase.addSchool(newSchool)
                Result.success(newSchool)
            } catch (e: Exception) {
                Result.failure(e)
            }
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

        if (SupabaseConfig.isConfigured(context)) {
            try {
                val api = SupabaseClient.getApi(context)
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
        } else {
            val existingWithCode = SimulatedDatabase.findSchoolByCode(code)
            if (existingWithCode != null && existingWithCode.id != schoolId) {
                return@withContext Result.failure(IllegalArgumentException("School code '$code' is already in use by another school."))
            }

            try {
                val updated = SimulatedDatabase.updateSchool(schoolId) { current ->
                    current.copy(
                        name = name,
                        code = code,
                        mobile = mobile,
                        email = email,
                        address = address,
                        isActive = isActive,
                        updatedAt = "2026-08-26T12:00:00Z"
                    )
                }
                if (updated != null) {
                    Result.success(updated)
                } else {
                    Result.failure(IllegalStateException("School not found."))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
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

        if (!SupabaseConfig.isConfigured(context)) {
            val existing = SimulatedDatabase.findSchoolById(schoolId)
                ?: return@withContext Result.failure(IllegalStateException("School not found."))

            val updated = SimulatedDatabase.updateSchool(schoolId) { current ->
                current.copy(
                    isActive = newActiveStatus,
                    updatedAt = "2026-08-26T12:00:00Z"
                )
            }
            if (updated != null) {
                Result.success(updated)
            } else {
                Result.failure(IllegalStateException("Failed to update school status."))
            }
        } else {
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
    }

    /**
     * Get active staff count for a school (for warning modal)
     */
    fun getActiveStaffCountForSchool(schoolId: String): Int {
        return SimulatedDatabase.getUsersBySchoolId(schoolId).size
    }
}
