package com.example

import com.example.data.model.AuthSession
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.common.RoleDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleDestinationTest {

    private fun createSession(
        role: String,
        isPrimaryAdmin: Boolean = false,
        isActive: Boolean = true,
        schoolId: String? = null
    ): AuthSession {
        return AuthSession(
            accessToken = "token_abc",
            refreshToken = "refresh_abc",
            profile = UserProfile(
                id = "user_1",
                email = "test@educhat.edu",
                fullName = "Test User",
                role = role,
                isActive = isActive,
                isPrimaryAdmin = isPrimaryAdmin,
                schoolId = schoolId
            )
        )
    }

    @Test
    fun resolves_teacher_destination() {
        val session = createSession(role = "teacher", schoolId = "school_1")
        val destination = RoleDestination.resolve(session)
        assertTrue(destination is RoleDestination.Teacher)
        assertEquals(session, (destination as RoleDestination.Teacher).session)
    }

    @Test
    fun resolves_student_destination() {
        val session = createSession(role = "student", schoolId = "school_1")
        val destination = RoleDestination.resolve(session)
        assertTrue(destination is RoleDestination.Student)
        assertEquals(session, (destination as RoleDestination.Student).session)
    }

    @Test
    fun resolves_school_admin_with_valid_school() {
        val session = createSession(role = "school_admin", schoolId = "school_123")
        val destination = RoleDestination.resolve(session)
        assertTrue(destination is RoleDestination.SchoolAdmin)
        assertEquals("school_123", (destination as RoleDestination.SchoolAdmin).schoolId)
    }

    @Test
    fun resolves_school_admin_without_school_to_restricted() {
        val session = createSession(role = "school_admin", schoolId = null)
        val destination = RoleDestination.resolve(session)
        assertTrue(destination is RoleDestination.Restricted)
    }

    @Test
    fun resolves_officer_admin_destination() {
        val session = createSession(role = "officer_admin", isPrimaryAdmin = false)
        val destination = RoleDestination.resolve(session)
        assertTrue(destination is RoleDestination.OfficerAdmin)
    }

    @Test
    fun resolves_primary_officer_admin_destination() {
        val session = createSession(role = "officer_admin", isPrimaryAdmin = true)
        val destination = RoleDestination.resolve(session)
        assertTrue(destination is RoleDestination.PrimaryOfficerAdmin)
    }

    @Test
    fun resolves_inactive_profile_to_restricted() {
        val session = createSession(role = "teacher", isActive = false)
        val destination = RoleDestination.resolve(session)
        assertTrue(destination is RoleDestination.Restricted)
    }
}
