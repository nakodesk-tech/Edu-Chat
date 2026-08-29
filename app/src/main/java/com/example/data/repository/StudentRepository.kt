package com.example.data.repository

import android.content.Context
import android.util.Patterns
import com.example.data.local.SessionManager
import com.example.data.local.SimulatedDatabase
import com.example.data.model.AuthSession
import com.example.data.model.OfficerAdminCreateUserRequest
import com.example.data.model.School
import com.example.data.model.SchoolAdminCreateStudentRequest
import com.example.data.model.UserProfile
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Authoritative Repository for Student Registration and Student Management (Phase C).
 * Supports Teachers, School Admins, and Officer Admins.
 */
class StudentRepository(private val context: Context) {
    private val sessionManager = SessionManager(context)

    /**
     * Verifies that the caller has an active session and is authorized to manage students.
     * Allowed roles: TEACHER, SCHOOL_ADMIN, OFFICER_ADMIN.
     */
    fun checkAuthorization(): Result<AuthSession> {
        val session = sessionManager.getSession()
            ?: return Result.failure(SecurityException("प्रमाणीकरण आवश्यक आहे. (Authentication required)"))

        val profile = session.profile
        if (!profile.isActive) {
            return Result.failure(SecurityException("आपले खाते निष्क्रिय आहे. (Account is deactivated)"))
        }

        val role = profile.userRole
        if (role != com.example.data.model.UserRole.OFFICER_ADMIN &&
            role != com.example.data.model.UserRole.SCHOOL_ADMIN &&
            role != com.example.data.model.UserRole.TEACHER
        ) {
            return Result.failure(SecurityException("विद्यार्थी व्यवस्थापनासाठी परवानगी नाही. (Unauthorized role)"))
        }

        return Result.success(session)
    }

    /**
     * Retrieves students list scoped by school or system-wide for Officer Admin.
     */
    suspend fun getStudents(targetSchoolId: String? = null): Result<List<UserProfile>> = withContext(Dispatchers.IO) {
        val authResult = checkAuthorization()
        if (authResult.isFailure) {
            return@withContext Result.failure(authResult.exceptionOrNull()!!)
        }
        val currentSession = authResult.getOrNull()!!
        val callerProfile = currentSession.profile

        val effectiveSchoolId = when {
            callerProfile.isOfficerAdmin -> targetSchoolId
            !callerProfile.schoolId.isNullOrBlank() -> callerProfile.schoolId
            else -> targetSchoolId
        }

        if (SupabaseConfig.isConfigured(context)) {
            try {
                val api = SupabaseClient.getApi(context)
                val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
                val response = api.getStudents(
                    apiKey = anonKey,
                    bearerToken = "Bearer ${currentSession.accessToken}",
                    roleFilter = "eq.student",
                    schoolIdFilter = if (!effectiveSchoolId.isNullOrBlank()) "eq.$effectiveSchoolId" else null
                )

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val rawError = response.errorBody()?.string()
                    val msg = SupabaseClient.parseError(rawError) ?: "विद्यार्थ्यांची यादी लोड करण्यात अयशस्वी."
                    Result.failure(Exception(msg))
                }
            } catch (e: Exception) {
                Result.failure(Exception("नेटवर्क त्रुटी: ${e.localizedMessage ?: "Failed to fetch students"}"))
            }
        } else {
            SimulatedDatabase.getStudents(effectiveSchoolId)
        }
    }

    /**
     * Registers a new student.
     * Enforces required information validation and school association.
     */
    suspend fun registerStudent(
        fullName: String,
        email: String,
        password: String,
        mobile: String?,
        standard: String?,
        schoolId: String,
        academicYear: String = com.example.ui.students.StudentStandardUtils.DEFAULT_ACADEMIC_YEAR
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val authResult = checkAuthorization()
        if (authResult.isFailure) {
            return@withContext Result.failure(authResult.exceptionOrNull()!!)
        }
        val currentSession = authResult.getOrNull()!!
        val callerProfile = currentSession.profile

        val trimmedName = fullName.trim()
        val trimmedEmail = email.trim().lowercase()
        val trimmedMobile = mobile?.trim()?.ifBlank { null }
        val trimmedStandard = standard?.trim()?.ifBlank { null }
        val trimmedAcademicYear = academicYear.trim()
        val effectiveSchoolId = if (callerProfile.isOfficerAdmin) {
            schoolId.trim()
        } else {
            callerProfile.schoolId ?: schoolId.trim()
        }

        // Validation
        if (trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("विद्यार्थ्याचे पूर्ण नाव प्रविष्ट करा. (Full name is required)"))
        }

        if (trimmedAcademicYear.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("शैक्षणिक वर्ष आवश्यक आहे. (Academic year is required)"))
        }

        if (trimmedEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("कृपया वैध ईमेल पत्ता प्रविष्ट करा. (Valid email required)"))
        }

        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("पासवर्ड किमान ६ अक्षरांचा असावा. (Password must be at least 6 characters)"))
        }

        if (effectiveSchoolId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("कृपया विद्यार्थ्यासाठी शाळा निवडा. (School association required)"))
        }

        if (trimmedMobile != null && trimmedMobile.length < 10) {
            return@withContext Result.failure(IllegalArgumentException("कृपया वैध मोबाईल क्रमांक प्रविष्ट करा. (Invalid mobile number)"))
        }

        if (SupabaseConfig.isConfigured(context)) {
            try {
                val api = SupabaseClient.getApi(context)
                val anonKey = SupabaseConfig.getSupabaseAnonKey(context)

                if (callerProfile.isOfficerAdmin) {
                    val response = api.officerAdminCreateUserRpc(
                        apiKey = anonKey,
                        bearerToken = "Bearer ${currentSession.accessToken}",
                        request = OfficerAdminCreateUserRequest(
                            email = trimmedEmail,
                            password = password,
                            fullName = trimmedName,
                            mobile = trimmedMobile,
                            role = "student",
                            schoolId = effectiveSchoolId
                        )
                    )

                    if (response.isSuccessful && response.body() != null) {
                        val created = response.body()!!
                        // Patch standard/class on profile if provided
                        if (!trimmedStandard.isNullOrBlank()) {
                            api.patchProfile(
                                apiKey = anonKey,
                                bearerToken = "Bearer ${currentSession.accessToken}",
                                idFilter = "eq.${created.id}",
                                updates = mapOf("standard" to trimmedStandard)
                            )
                        }
                        Result.success(created.copy(standard = trimmedStandard))
                    } else {
                        val rawError = response.errorBody()?.string()
                        val msg = SupabaseClient.parseError(rawError) ?: "विद्यार्थी नोंदणी अयशस्वी झाली."
                        Result.failure(Exception(msg))
                    }
                } else {
                    // Active TEACHER or SCHOOL_ADMIN creating student within their authorized school via school_admin_create_student RPC
                    val parsedStd = com.example.ui.students.StudentStandardUtils.parseStandard(trimmedStandard) ?: trimmedStandard
                    val parsedSec = com.example.ui.students.StudentStandardUtils.parseSection(trimmedStandard)

                    val response = api.schoolAdminCreateStudentRpc(
                        apiKey = anonKey,
                        bearerToken = "Bearer ${currentSession.accessToken}",
                        request = SchoolAdminCreateStudentRequest(
                            email = trimmedEmail,
                            password = password,
                            fullName = trimmedName,
                            mobile = trimmedMobile,
                            standard = parsedStd,
                            section = parsedSec,
                            academicYear = trimmedAcademicYear
                        )
                    )

                    if (response.isSuccessful && response.body() != null) {
                        val created = response.body()!!
                        Result.success(created)
                    } else {
                        val rawError = response.errorBody()?.string()
                        val msg = SupabaseClient.parseError(rawError) ?: "विद्यार्थी नोंदणी अयशस्वी झाली."
                        Result.failure(Exception(msg))
                    }
                }
            } catch (e: Exception) {
                Result.failure(Exception("नोंदणी त्रुटी: ${e.localizedMessage ?: "Registration error"}"))
            }
        } else {
            SimulatedDatabase.createStudent(
                fullName = trimmedName,
                email = trimmedEmail,
                password = password,
                mobile = trimmedMobile,
                standard = trimmedStandard,
                schoolId = effectiveSchoolId,
                academicYear = trimmedAcademicYear
            )
        }
    }

    /**
     * Updates an existing student's information (Full Name, Mobile, Standard, School, Active Status).
     */
    suspend fun updateStudent(
        studentId: String,
        fullName: String,
        mobile: String?,
        standard: String?,
        schoolId: String?,
        isActive: Boolean
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val authResult = checkAuthorization()
        if (authResult.isFailure) {
            return@withContext Result.failure(authResult.exceptionOrNull()!!)
        }
        val currentSession = authResult.getOrNull()!!

        val trimmedName = fullName.trim()
        if (trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("विद्यार्थ्याचे पूर्ण नाव आवश्यक आहे."))
        }

        val trimmedMobile = mobile?.trim()?.ifBlank { null }
        val trimmedStandard = standard?.trim()?.ifBlank { null }

        if (trimmedMobile != null && trimmedMobile.length < 10) {
            return@withContext Result.failure(IllegalArgumentException("कृपया वैध मोबाईल क्रमांक प्रविष्ट करा."))
        }

        if (SupabaseConfig.isConfigured(context)) {
            try {
                val api = SupabaseClient.getApi(context)
                val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
                val updates = mutableMapOf<String, Any?>(
                    "full_name" to trimmedName,
                    "mobile" to trimmedMobile,
                    "standard" to trimmedStandard,
                    "is_active" to isActive
                )
                if (!schoolId.isNullOrBlank()) {
                    updates["school_id"] = schoolId
                }

                val response = api.patchProfile(
                    apiKey = anonKey,
                    bearerToken = "Bearer ${currentSession.accessToken}",
                    idFilter = "eq.$studentId",
                    updates = updates
                )

                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    Result.success(response.body()!!.first())
                } else {
                    val rawError = response.errorBody()?.string()
                    val msg = SupabaseClient.parseError(rawError) ?: "विद्यार्थी माहिती अद्यतनित करण्यात अयशस्वी."
                    Result.failure(Exception(msg))
                }
            } catch (e: Exception) {
                Result.failure(Exception("अद्यतन त्रुटी: ${e.localizedMessage ?: "Update error"}"))
            }
        } else {
            SimulatedDatabase.updateStudent(
                studentId = studentId,
                fullName = trimmedName,
                mobile = trimmedMobile,
                standard = trimmedStandard,
                schoolId = schoolId,
                isActive = isActive
            )
        }
    }

    /**
     * Toggles a student's active/inactive status.
     */
    suspend fun toggleStudentStatus(studentId: String, isActive: Boolean): Result<UserProfile> = withContext(Dispatchers.IO) {
        val authResult = checkAuthorization()
        if (authResult.isFailure) {
            return@withContext Result.failure(authResult.exceptionOrNull()!!)
        }
        val currentSession = authResult.getOrNull()!!

        if (SupabaseConfig.isConfigured(context)) {
            try {
                val api = SupabaseClient.getApi(context)
                val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
                val response = api.patchProfile(
                    apiKey = anonKey,
                    bearerToken = "Bearer ${currentSession.accessToken}",
                    idFilter = "eq.$studentId",
                    updates = mapOf("is_active" to isActive)
                )

                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    Result.success(response.body()!!.first())
                } else {
                    val rawError = response.errorBody()?.string()
                    val msg = SupabaseClient.parseError(rawError) ?: "स्थिती बदलण्यात अयशस्वी."
                    Result.failure(Exception(msg))
                }
            } catch (e: Exception) {
                Result.failure(Exception("त्रुटी: ${e.localizedMessage ?: "Status toggle error"}"))
            }
        } else {
            SimulatedDatabase.toggleStudentStatus(studentId, isActive)
        }
    }

    /**
     * Safely deletes a student account.
     */
    suspend fun deleteStudent(studentId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val authResult = checkAuthorization()
        if (authResult.isFailure) {
            return@withContext Result.failure(authResult.exceptionOrNull()!!)
        }
        val currentSession = authResult.getOrNull()!!

        if (SupabaseConfig.isConfigured(context)) {
            try {
                val api = SupabaseClient.getApi(context)
                val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
                // In Supabase, deactivates profile or removes record safely
                val response = api.patchProfile(
                    apiKey = anonKey,
                    bearerToken = "Bearer ${currentSession.accessToken}",
                    idFilter = "eq.$studentId",
                    updates = mapOf("is_active" to false)
                )

                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    val rawError = response.errorBody()?.string()
                    val msg = SupabaseClient.parseError(rawError) ?: "विद्यार्थी काढून टाकण्यात अयशस्वी."
                    Result.failure(Exception(msg))
                }
            } catch (e: Exception) {
                Result.failure(Exception("त्रुटी: ${e.localizedMessage ?: "Delete error"}"))
            }
        } else {
            SimulatedDatabase.deleteStudent(studentId)
        }
    }

    /**
     * Fetches all schools for association dropdowns/lookups.
     */
    suspend fun getSchools(): Result<List<School>> = withContext(Dispatchers.IO) {
        val authResult = checkAuthorization()
        if (authResult.isFailure) {
            return@withContext Result.failure(authResult.exceptionOrNull()!!)
        }
        val currentSession = authResult.getOrNull()!!

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
                    Result.success(emptyList())
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.success(SimulatedDatabase.getAllSchools())
        }
    }
}
