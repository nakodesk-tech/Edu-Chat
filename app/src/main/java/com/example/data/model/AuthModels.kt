package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class UserRole(val dbValue: String, val displayName: String) {
    @Json(name = "officer_admin")
    OFFICER_ADMIN("officer_admin", "Admin"),

    @Json(name = "school_admin")
    SCHOOL_ADMIN("school_admin", "School Admin"),

    @Json(name = "teacher")
    TEACHER("teacher", "Teacher"),

    @Json(name = "student")
    STUDENT("student", "Student");

    companion object {
        fun fromDbValue(value: String?): UserRole? {
            return entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) }
        }
    }
}

@JsonClass(generateAdapter = true)
data class UserProfile(
    @Json(name = "id") val id: String,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "mobile") val mobile: String? = null,
    @Json(name = "standard") val standard: String? = null,
    @Json(name = "role") val role: String = "student",
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "is_primary_admin") val isPrimaryAdmin: Boolean = false,
    @Json(name = "school_id") val schoolId: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
) {
    val userRole: UserRole
        get() = UserRole.fromDbValue(role) ?: UserRole.STUDENT

    val isOfficerAdmin: Boolean
        get() = role.equals("officer_admin", ignoreCase = true)

    val isSchoolAdmin: Boolean
        get() = role.equals("school_admin", ignoreCase = true)
}

@JsonClass(generateAdapter = true)
data class School(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "code") val code: String,
    @Json(name = "mobile") val mobile: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateSchoolRequest(
    @Json(name = "name") val name: String,
    @Json(name = "code") val code: String,
    @Json(name = "mobile") val mobile: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "address") val address: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateSchoolRequest(
    @Json(name = "school_id") val schoolId: String,
    @Json(name = "name") val name: String,
    @Json(name = "code") val code: String,
    @Json(name = "mobile") val mobile: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class OfficerAdminCreateUserRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "mobile") val mobile: String? = null,
    @Json(name = "role") val role: String,
    @Json(name = "school_id") val schoolId: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateOfficerProfileRequest(
    @Json(name = "full_name") val fullName: String,
    @Json(name = "mobile") val mobile: String? = null
)

@JsonClass(generateAdapter = true)
data class SchoolAdminCreateTeacherRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "mobile") val mobile: String? = null
)

@JsonClass(generateAdapter = true)
data class SchoolAdminCreateStudentRequest(
    @Json(name = "p_email") val email: String,
    @Json(name = "p_password") val password: String,
    @Json(name = "p_full_name") val fullName: String,
    @Json(name = "p_mobile") val mobile: String? = null,
    @Json(name = "p_standard") val standard: String? = null,
    @Json(name = "p_section") val section: String? = null,
    @Json(name = "p_academic_year") val academicYear: String? = null
)

@JsonClass(generateAdapter = true)
data class SchoolAdminUpdateStudentRequest(
    @Json(name = "p_student_id") val studentId: String,
    @Json(name = "p_full_name") val fullName: String,
    @Json(name = "p_mobile") val mobile: String? = null,
    @Json(name = "p_standard") val standard: String? = null,
    @Json(name = "p_section") val section: String? = null,
    @Json(name = "p_is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class SchoolAdminUpdateTeacherRequest(
    @Json(name = "teacher_id") val teacherId: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "mobile") val mobile: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class CreateStudentRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "mobile") val mobile: String? = null,
    @Json(name = "standard") val standard: String? = null,
    @Json(name = "school_id") val schoolId: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateStudentRequest(
    @Json(name = "student_id") val studentId: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "mobile") val mobile: String? = null,
    @Json(name = "standard") val standard: String? = null,
    @Json(name = "school_id") val schoolId: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthUser(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseSignupResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "expires_in") val expiresIn: Long? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "user") val user: SupabaseAuthUser? = null
) {
    val effectiveUserId: String?
        get() = user?.id ?: id
}

@JsonClass(generateAdapter = true)
data class SupabaseTokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String = "bearer",
    @Json(name = "expires_in") val expiresIn: Long = 3600,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "user") val user: SupabaseAuthUser? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseLoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class SupabaseRefreshTokenRequest(
    @Json(name = "refresh_token") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class SupabaseSignupRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "data") val data: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class UpdateDisplayNameRequest(
    @Json(name = "new_full_name") val newFullName: String
)

@JsonClass(generateAdapter = true)
data class AdminCreateUserRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "role") val role: String,
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class AdminUpdateUserRequest(
    @Json(name = "user_id") val userId: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "role") val role: String,
    @Json(name = "is_active") val isActive: Boolean
)

@JsonClass(generateAdapter = true)
data class AdminToggleStatusRequest(
    @Json(name = "user_id") val userId: String,
    @Json(name = "is_active") val isActive: Boolean
)

@JsonClass(generateAdapter = true)
data class SupabaseErrorResponse(
    @Json(name = "error") val error: String? = null,
    @Json(name = "error_description") val errorDescription: String? = null,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "code") val code: Any? = null
)

data class AuthSession(
    val accessToken: String,
    val refreshToken: String?,
    val profile: UserProfile
)
