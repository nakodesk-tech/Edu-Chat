package com.example.ui.common

import com.example.data.model.AuthSession
import com.example.data.model.UserProfile
import com.example.data.model.UserRole

/**
 * Sealed hierarchy defining clean role-based navigation destinations
 * resolved from an authenticated user's profile and state.
 */
sealed interface RoleDestination {
    /**
     * Unauthenticated or logged out state leading to authentication screen.
     */
    data object Auth : RoleDestination

    /**
     * Teacher role destination with class, chats, and student management.
     */
    data class Teacher(val session: AuthSession) : RoleDestination

    /**
     * School Admin role destination scoped to a specific active school.
     */
    data class SchoolAdmin(val session: AuthSession, val schoolId: String) : RoleDestination

    /**
     * Officer Admin destination for regional/block administration.
     */
    data class OfficerAdmin(val session: AuthSession) : RoleDestination

    /**
     * Primary Officer Admin destination with root privilege controls.
     */
    data class PrimaryOfficerAdmin(val session: AuthSession) : RoleDestination

    /**
     * Student role destination with enrolled learning and chats.
     */
    data class Student(val session: AuthSession) : RoleDestination

    /**
     * Inactive, unassigned, or restricted session destination.
     */
    data class Restricted(val session: AuthSession) : RoleDestination

    companion object {
        /**
         * Resolves the appropriate destination boundary based strictly on the authenticated session profile.
         */
        fun resolve(session: AuthSession): RoleDestination {
            val profile = session.profile
            if (!profile.isActive) {
                return Restricted(session)
            }

            return when (profile.userRole) {
                UserRole.OFFICER_ADMIN -> {
                    if (profile.isPrimaryAdmin) {
                        PrimaryOfficerAdmin(session)
                    } else {
                        OfficerAdmin(session)
                    }
                }
                UserRole.SCHOOL_ADMIN -> {
                    val schoolId = profile.schoolId
                    if (!schoolId.isNullOrBlank()) {
                        SchoolAdmin(session, schoolId)
                    } else {
                        Restricted(session)
                    }
                }
                UserRole.TEACHER -> {
                    Teacher(session)
                }
                UserRole.STUDENT -> {
                    Student(session)
                }
            }
        }
    }
}
