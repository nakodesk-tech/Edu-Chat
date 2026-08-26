package com.example.data.repository

import android.content.Context
import android.util.Patterns
import com.example.data.local.SessionManager
import com.example.data.local.SimulatedDatabase
import com.example.data.model.AuthSession
import com.example.data.model.School
import com.example.data.model.SchoolAdminCreateTeacherRequest
import com.example.data.model.SchoolAdminUpdateTeacherRequest
import com.example.data.model.UserProfile
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Authoritative Repository for School Admin operations.
 *
 * Enforces School Admin authorization:
 * - role == "school_admin"
 * - is_active == true
 * - school_id != null
 *
 * Scope:
 * - Can manage Teachers belonging ONLY to the School Admin's assigned school.
 * - Cannot view, edit, deactivate, or create users for other schools.
 * - Cannot create Officer Admin, School Admin, or Student.
 */
class SchoolAdminRepository(private val context: Context) {
    private val sessionManager = SessionManager(context)

    /**
     * Authoritative Security Guard
     */
    fun checkSchoolAdminAuthorization(): Result<AuthSession> {
        val session = sessionManager.getSession()
            ?: return Result.failure(SecurityException("Authentication required. No active session."))

        val profile = session.profile
        if (!profile.isActive) {
            return Result.failure(SecurityException("Your School Admin account has been deactivated."))
        }

        if (!profile.role.equals("school_admin", ignoreCase = true)) {
            return Result.failure(SecurityException("Access denied. School Admin privileges are required."))
        }

        if (profile.schoolId.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Access denied. No assigned school found on profile."))
        }

        return Result.success(session)
    }

    /**
     * Get the authenticated School Admin profile
     */
    suspend fun getSchoolAdminProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        val authCheck = checkSchoolAdminAuthorization()
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
     * Get the School Admin's assigned School details
     */
    suspend fun getAssignedSchool(): Result<School> = withContext(Dispatchers.IO) {
        val authCheck = checkSchoolAdminAuthorization()
        if (authCheck.isFailure) {
            return@withContext Result.failure(authCheck.exceptionOrNull()!!)
        }
        val currentSession = authCheck.getOrNull()!!
        val schoolId = currentSession.profile.schoolId!!

        if (SupabaseConfig.isConfigured(context)) {
            try {
                val api = SupabaseClient.getApi(context)
                val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
                val response = api.getSchoolById(
                    apiKey = anonKey,
                    bearerToken = "Bearer ${currentSession.accessToken}",
                    idFilter = "eq.$schoolId"
                )
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    Result.success(response.body()!!.first())
                } else {
                    Result.failure(NoSuchElementException("शाळा आढळली नाही. (Assigned school not found)"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            val school = SimulatedDatabase.findSchoolById(schoolId)
            if (school != null) {
                Result.success(school)
            } else {
                Result.failure(NoSuchElementException("शाळा आढळली नाही. (Assigned school not found)"))
            }
        }
    }

    /**
     * Get all Teachers belonging to the School Admin's assigned school
     */
    suspend fun getTeachers(): Result<List<UserProfile>> = withContext(Dispatchers.IO) {
        val authCheck = checkSchoolAdminAuthorization()
        if (authCheck.isFailure) {
            return@withContext Result.failure(authCheck.exceptionOrNull()!!)
        }
        val currentSession = authCheck.getOrNull()!!
        val schoolId = currentSession.profile.schoolId!!

        if (SupabaseConfig.isConfigured(context)) {
            try {
                val api = SupabaseClient.getApi(context)
                val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
                val response = api.getTeachersBySchool(
                    apiKey = anonKey,
                    bearerToken = "Bearer ${currentSession.accessToken}",
                    schoolIdFilter = "eq.$schoolId"
                )
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("शिक्षकांची यादी लोड करता आली नाही. (Failed to load teachers)"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.success(SimulatedDatabase.getTeachersBySchool(schoolId))
        }
    }

    /**
     * Create a new Teacher for the School Admin's assigned school
     */
    suspend fun createTeacher(
        fullNameInput: String,
        emailInput: String,
        mobileInput: String?,
        passwordInput: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val authCheck = checkSchoolAdminAuthorization()
        if (authCheck.isFailure) {
            return@withContext Result.failure(authCheck.exceptionOrNull()!!)
        }
        val currentSession = authCheck.getOrNull()!!
        val callerSchoolId = currentSession.profile.schoolId!!

        // Validate Input
        val trimmedName = fullNameInput.trim()
        val trimmedEmail = emailInput.trim().lowercase()
        val trimmedMobile = mobileInput?.trim()?.ifBlank { null }
        val password = passwordInput

        if (trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("कृपया शिक्षकांचे पूर्ण नाव प्रविष्ट करा. (Full name is required)"))
        }

        if (trimmedEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("कृपया वैध ईमेल पत्ता प्रविष्ट करा. (Valid email is required)"))
        }

        if (trimmedMobile != null && (trimmedMobile.length < 10 || !trimmedMobile.all { it.isDigit() })) {
            return@withContext Result.failure(IllegalArgumentException("कृपया वैध १०-अंकी मोबाईल नंबर प्रविष्ट करा. (Valid 10-digit mobile required)"))
        }

        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("पासवर्ड किमान ६ अक्षरांचा असावा. (Password must be at least 6 characters)"))
        }

        if (SupabaseConfig.isConfigured(context)) {
            try {
                val api = SupabaseClient.getApi(context)
                val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
                val request = SchoolAdminCreateTeacherRequest(
                    email = trimmedEmail,
                    password = password,
                    fullName = trimmedName,
                    mobile = trimmedMobile
                )
                val response = api.schoolAdminCreateTeacherRpc(
                    apiKey = anonKey,
                    bearerToken = "Bearer ${currentSession.accessToken}",
                    request = request
                )
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = if (response.code() == 409) {
                        "हा Email आधीपासून नोंदणीकृत आहे. (Email already registered)"
                    } else {
                        "शिक्षक नोंदणी पूर्ण होऊ शकली नाही. कृपया माहिती तपासा. (${response.message()})"
                    }
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            SimulatedDatabase.schoolAdminCreateTeacher(
                callerSchoolId = callerSchoolId,
                fullName = trimmedName,
                email = trimmedEmail,
                mobile = trimmedMobile,
                password = password
            )
        }
    }

    /**
     * Edit Teacher information (Name, Mobile, Active/Inactive status)
     */
    suspend fun updateTeacher(
        teacherId: String,
        fullNameInput: String,
        mobileInput: String?,
        isActive: Boolean
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val authCheck = checkSchoolAdminAuthorization()
        if (authCheck.isFailure) {
            return@withContext Result.failure(authCheck.exceptionOrNull()!!)
        }
        val currentSession = authCheck.getOrNull()!!
        val callerSchoolId = currentSession.profile.schoolId!!

        val trimmedName = fullNameInput.trim()
        val trimmedMobile = mobileInput?.trim()?.ifBlank { null }

        if (trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("कृपया शिक्षकांचे पूर्ण नाव प्रविष्ट करा. (Full name is required)"))
        }

        if (trimmedMobile != null && (trimmedMobile.length < 10 || !trimmedMobile.all { it.isDigit() })) {
            return@withContext Result.failure(IllegalArgumentException("कृपया वैध १०-अंकी मोबाईल नंबर प्रविष्ट करा. (Valid 10-digit mobile required)"))
        }

        if (SupabaseConfig.isConfigured(context)) {
            try {
                val api = SupabaseClient.getApi(context)
                val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
                val request = SchoolAdminUpdateTeacherRequest(
                    teacherId = teacherId,
                    fullName = trimmedName,
                    mobile = trimmedMobile,
                    isActive = isActive
                )
                val response = api.schoolAdminUpdateTeacherRpc(
                    apiKey = anonKey,
                    bearerToken = "Bearer ${currentSession.accessToken}",
                    request = request
                )
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("शिक्षकांची माहिती अद्यतनित करण्यात त्रुटी आली. (Failed to update teacher)"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            SimulatedDatabase.schoolAdminUpdateTeacher(
                callerSchoolId = callerSchoolId,
                teacherId = teacherId,
                fullName = trimmedName,
                mobile = trimmedMobile,
                isActive = isActive
            )
        }
    }

    /**
     * Soft toggle Teacher Active / Inactive status
     */
    suspend fun toggleTeacherStatus(
        teacherId: String,
        isActive: Boolean
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val authCheck = checkSchoolAdminAuthorization()
        if (authCheck.isFailure) {
            return@withContext Result.failure(authCheck.exceptionOrNull()!!)
        }
        val currentSession = authCheck.getOrNull()!!
        val callerSchoolId = currentSession.profile.schoolId!!

        if (SupabaseConfig.isConfigured(context)) {
            try {
                val api = SupabaseClient.getApi(context)
                val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
                // Reuse RPC with existing fields
                val teacherMatch = SimulatedDatabase.findById(teacherId)?.profile
                val name = teacherMatch?.fullName ?: "Teacher"
                val mobile = teacherMatch?.mobile
                val request = SchoolAdminUpdateTeacherRequest(
                    teacherId = teacherId,
                    fullName = name,
                    mobile = mobile,
                    isActive = isActive
                )
                val response = api.schoolAdminUpdateTeacherRpc(
                    apiKey = anonKey,
                    bearerToken = "Bearer ${currentSession.accessToken}",
                    request = request
                )
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("स्थिती बदलण्यात त्रुटी आली. (Failed to toggle status)"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            SimulatedDatabase.schoolAdminToggleTeacherStatus(
                callerSchoolId = callerSchoolId,
                teacherId = teacherId,
                isActive = isActive
            )
        }
    }

    /**
     * Update permitted fields for School Admin's own profile (Name, Mobile)
     */
    suspend fun updateSchoolAdminProfile(
        fullNameInput: String,
        mobileInput: String?
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val authCheck = checkSchoolAdminAuthorization()
        if (authCheck.isFailure) {
            return@withContext Result.failure(authCheck.exceptionOrNull()!!)
        }
        val currentSession = authCheck.getOrNull()!!
        val trimmedName = fullNameInput.trim()
        val trimmedMobile = mobileInput?.trim()?.ifBlank { null }

        if (trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("कृपया पूर्ण नाव प्रविष्ट करा. (Full name is required)"))
        }

        if (trimmedMobile != null && (trimmedMobile.length < 10 || !trimmedMobile.all { it.isDigit() })) {
            return@withContext Result.failure(IllegalArgumentException("कृपया वैध १०-अंकी मोबाईल नंबर प्रविष्ट करा. (Valid 10-digit mobile required)"))
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
                    Result.failure(Exception("प्रोफाइल अद्यतनित करण्यात त्रुटी आली."))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            val updated = SimulatedDatabase.updateProfile(currentSession.profile.id) {
                it.copy(fullName = trimmedName, mobile = trimmedMobile)
            }
            if (updated != null) {
                sessionManager.saveSession(currentSession.copy(profile = updated))
                Result.success(updated)
            } else {
                Result.failure(NoSuchElementException("वापरकर्ता प्रोफाइल आढळले नाही."))
            }
        }
    }
}
