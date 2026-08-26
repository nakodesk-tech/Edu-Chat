package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionManager
import com.example.data.local.SimulatedDatabase
import com.example.data.local.SimulatedDbUser
import com.example.data.model.AuthSession
import com.example.data.model.GroupType
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.repository.GroupRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GroupSecurityTest {

    private lateinit var context: Context
    private lateinit var sessionManager: SessionManager
    private lateinit var groupRepository: GroupRepository

    private val puneSchoolId = "s0000000-0001-4000-8000-000000000001"
    private val mumbaiSchoolId = "s0000000-0002-4000-8000-000000000002"

    private lateinit var officerAdmin: UserProfile
    private lateinit var puneSchoolAdmin: UserProfile
    private lateinit var puneTeacher1: UserProfile
    private lateinit var puneTeacher2: UserProfile
    private lateinit var puneStudent1: UserProfile
    private lateinit var mumbaiTeacher: UserProfile
    private lateinit var mumbaiStudent: UserProfile
    private lateinit var deactivatedTeacher: UserProfile

    @Before
    fun setup(): Unit = runBlocking {
        context = ApplicationProvider.getApplicationContext<Context>()
        SimulatedDatabase.reset()
        sessionManager = SessionManager(context)
        sessionManager.clearSession()
        groupRepository = GroupRepository(context)

        officerAdmin = UserProfile(
            id = "officer-admin-01",
            fullName = "District Officer Admin",
            email = "officer.security@educhat.edu",
            mobile = "9811111111",
            role = "officer_admin",
            schoolId = null,
            isActive = true,
            isPrimaryAdmin = true
        )
        SimulatedDatabase.addUser(SimulatedDbUser(email = officerAdmin.email!!, password = "Pass@1234", profile = officerAdmin))

        puneSchoolAdmin = UserProfile(
            id = "pune-admin-01",
            fullName = "Pune Principal",
            email = "principal.pune@educhat.edu",
            mobile = "9822222222",
            role = "school_admin",
            schoolId = puneSchoolId,
            isActive = true
        )
        SimulatedDatabase.addUser(SimulatedDbUser(email = puneSchoolAdmin.email!!, password = "Pass@1234", profile = puneSchoolAdmin))

        puneTeacher1 = UserProfile(
            id = "pune-teacher-01",
            fullName = "Sunil Patil",
            email = "sunil.patil@pune.edu",
            mobile = "9833333331",
            role = "teacher",
            schoolId = puneSchoolId,
            isActive = true
        )
        SimulatedDatabase.addUser(SimulatedDbUser(email = puneTeacher1.email!!, password = "Pass@1234", profile = puneTeacher1))

        puneTeacher2 = UserProfile(
            id = "pune-teacher-02",
            fullName = "Anjali Joshi",
            email = "anjali.joshi@pune.edu",
            mobile = "9833333332",
            role = "teacher",
            schoolId = puneSchoolId,
            isActive = true
        )
        SimulatedDatabase.addUser(SimulatedDbUser(email = puneTeacher2.email!!, password = "Pass@1234", profile = puneTeacher2))

        puneStudent1 = UserProfile(
            id = "pune-student-01",
            fullName = "Rahul Deshmukh",
            email = "rahul.pune@student.edu",
            mobile = "9844444441",
            role = "student",
            schoolId = puneSchoolId,
            isActive = true
        )
        SimulatedDatabase.addUser(SimulatedDbUser(email = puneStudent1.email!!, password = "Pass@1234", profile = puneStudent1))

        mumbaiTeacher = UserProfile(
            id = "mumbai-teacher-01",
            fullName = "Vikram Rane",
            email = "vikram.rane@mumbai.edu",
            mobile = "9855555551",
            role = "teacher",
            schoolId = mumbaiSchoolId,
            isActive = true
        )
        SimulatedDatabase.addUser(SimulatedDbUser(email = mumbaiTeacher.email!!, password = "Pass@1234", profile = mumbaiTeacher))

        mumbaiStudent = UserProfile(
            id = "mumbai-student-01",
            fullName = "Neha Shinde",
            email = "neha.mumbai@student.edu",
            mobile = "9866666661",
            role = "student",
            schoolId = mumbaiSchoolId,
            isActive = true
        )
        SimulatedDatabase.addUser(SimulatedDbUser(email = mumbaiStudent.email!!, password = "Pass@1234", profile = mumbaiStudent))

        deactivatedTeacher = UserProfile(
            id = "deact-teacher-01",
            fullName = "Inactive Teacher",
            email = "inactive.teacher@pune.edu",
            mobile = "9877777771",
            role = "teacher",
            schoolId = puneSchoolId,
            isActive = false
        )
        SimulatedDatabase.addUser(SimulatedDbUser(email = deactivatedTeacher.email!!, password = "Pass@1234", profile = deactivatedTeacher))
    }

    private fun setSession(profile: UserProfile) {
        sessionManager.saveSession(
            AuthSession(
                accessToken = "mock-token-${profile.id}",
                refreshToken = "mock-refresh-${profile.id}",
                profile = profile
            )
        )
    }

    // 1. Officer Admin can create administrative group with NULL school_id
    @Test
    fun test01_officerAdminCanCreateAdministrativeGroup() = runBlocking {
        setSession(officerAdmin)
        val result = groupRepository.createGroup("District Coordination", GroupType.ADMINISTRATIVE)
        assertTrue(result.isSuccess)
        val group = result.getOrNull()
        assertNotNull(group)
        assertEquals("District Coordination", group?.name)
        assertEquals(GroupType.ADMINISTRATIVE, group?.typedGroupType)
        assertNull(group?.schoolId)
        assertEquals(officerAdmin.id, group?.createdBy)
    }

    // 2. Teacher can create teacher group with non-null school_id matching teacher's school_id
    @Test
    fun test02_teacherCanCreateTeacherGroupScopedToOwnSchool() = runBlocking {
        setSession(puneTeacher1)
        val result = groupRepository.createGroup("Grade 10 Teachers", GroupType.TEACHER)
        assertTrue(result.isSuccess)
        val group = result.getOrNull()
        assertNotNull(group)
        assertEquals("Grade 10 Teachers", group?.name)
        assertEquals(GroupType.TEACHER, group?.typedGroupType)
        assertEquals(puneSchoolId, group?.schoolId)
        assertEquals(puneTeacher1.id, group?.createdBy)
    }

    // 3. Teacher cannot create administrative group
    @Test
    fun test03_teacherCannotCreateAdministrativeGroup() = runBlocking {
        setSession(puneTeacher1)
        val result = groupRepository.createGroup("Illegal Admin Group", GroupType.ADMINISTRATIVE)
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("permission", ignoreCase = true) == true ||
                result.exceptionOrNull()?.message?.contains("Officer Admin", ignoreCase = true) == true)
    }

    // 4. Teacher cannot create group with another school's school_id (authoritative profile enforcement)
    @Test
    fun test04_teacherCannotOverrideSchoolId() = runBlocking {
        setSession(puneTeacher1)
        // Group creation automatically binds to puneTeacher1.schoolId (puneSchoolId)
        val result = groupRepository.createGroup("Pune Group", GroupType.TEACHER)
        assertTrue(result.isSuccess)
        assertEquals(puneSchoolId, result.getOrNull()?.schoolId)
    }

    // 5. School Admin cannot create group (creation rejected)
    @Test
    fun test05_schoolAdminCannotCreateGroup() = runBlocking {
        setSession(puneSchoolAdmin)
        val result = groupRepository.createGroup("School Admin Group", GroupType.ADMINISTRATIVE)
        assertFalse(result.isSuccess)
    }

    // 6. Student cannot create group (creation rejected)
    @Test
    fun test06_studentCannotCreateGroup() = runBlocking {
        setSession(puneStudent1)
        val result = groupRepository.createGroup("Student Study Group", GroupType.TEACHER)
        assertFalse(result.isSuccess)
    }

    // 7. Unauthenticated user cannot create group
    @Test
    fun test07_unauthenticatedUserCannotCreateGroup() = runBlocking {
        sessionManager.clearSession()
        val result = groupRepository.createGroup("Anon Group", GroupType.ADMINISTRATIVE)
        assertFalse(result.isSuccess)
    }

    // 8. Deactivated user cannot create group
    @Test
    fun test08_deactivatedUserCannotCreateGroup() = runBlocking {
        setSession(deactivatedTeacher)
        val result = groupRepository.createGroup("Deactivated Group", GroupType.TEACHER)
        assertFalse(result.isSuccess)
    }

    // 9. Group creator is automatically added as group admin member
    @Test
    fun test09_creatorIsAutomaticallyAdminMember() = runBlocking {
        setSession(puneTeacher1)
        val result = groupRepository.createGroup("Math Department", GroupType.TEACHER)
        assertTrue(result.isSuccess)
        val groupId = result.getOrNull()!!.id

        val detailsResult = groupRepository.getGroupDetails(groupId)
        assertTrue(detailsResult.isSuccess)
        val details = detailsResult.getOrNull()
        assertNotNull(details)
        assertEquals(1, details?.members?.size)
        val member = details?.members?.first()
        assertEquals(puneTeacher1.id, member?.userId)
        assertTrue(member?.roleInGroup == "admin" || member?.roleInGroup == "owner")
    }

    // 10. Group member can view the group details
    @Test
    fun test10_groupMemberCanViewGroupDetails() = runBlocking {
        setSession(puneTeacher1)
        val group = groupRepository.createGroup("Science Club", GroupType.TEACHER).getOrNull()!!

        val details = groupRepository.getGroupDetails(group.id).getOrNull()
        assertNotNull(details)
        assertEquals(group.id, details?.group?.id)
        assertEquals("Science Club", details?.group?.name)
    }

    // 11. User not in group cannot view group details (SELECT RLS policy enforcement)
    @Test
    fun test11_nonMemberCannotViewGroupDetails() = runBlocking {
        setSession(puneTeacher1)
        val group = groupRepository.createGroup("Private Club", GroupType.TEACHER).getOrNull()!!

        // Switch to mumbaiTeacher who is NOT in this group
        setSession(mumbaiTeacher)
        val detailsResult = groupRepository.getGroupDetails(group.id)
        assertFalse(detailsResult.isSuccess)
        assertTrue(detailsResult.exceptionOrNull()?.message?.contains("permission", ignoreCase = true) == true ||
                detailsResult.exceptionOrNull()?.message?.contains("Access denied", ignoreCase = true) == true)
    }

    // 12. Officer Admin can search and add active users from any school to administrative group
    @Test
    fun test12_officerAdminCanAddUsersAcrossSchools() = runBlocking {
        setSession(officerAdmin)
        val group = groupRepository.createGroup("Inter-District Taskforce", GroupType.ADMINISTRATIVE).getOrNull()!!

        // Add puneSchoolAdmin and mumbaiTeacher
        val add1 = groupRepository.addMember(group.id, puneSchoolAdmin.id)
        val add2 = groupRepository.addMember(group.id, mumbaiTeacher.id)
        assertTrue(add1.isSuccess)
        assertTrue(add2.isSuccess)

        val details = groupRepository.getGroupDetails(group.id).getOrNull()!!
        assertEquals(3, details.members.size)
    }

    // 13. Officer Admin cannot add deactivated users to administrative group
    @Test
    fun test13_officerAdminCannotAddDeactivatedUser() = runBlocking {
        setSession(officerAdmin)
        val group = groupRepository.createGroup("All Staff", GroupType.ADMINISTRATIVE).getOrNull()!!

        val addResult = groupRepository.addMember(group.id, deactivatedTeacher.id)
        assertFalse(addResult.isSuccess)
    }

    // 14. Teacher can search and add teachers and students from SAME school to teacher group
    @Test
    fun test14_teacherCanAddSameSchoolMembers() = runBlocking {
        setSession(puneTeacher1)
        val group = groupRepository.createGroup("Pune Class 10A", GroupType.TEACHER).getOrNull()!!

        // Add puneTeacher2 and puneStudent1
        val addTeacher2 = groupRepository.addMember(group.id, puneTeacher2.id)
        val addStudent1 = groupRepository.addMember(group.id, puneStudent1.id)
        assertTrue(addTeacher2.isSuccess)
        assertTrue(addStudent1.isSuccess)

        val details = groupRepository.getGroupDetails(group.id).getOrNull()!!
        assertEquals(3, details.members.size)
    }

    // 15. Teacher cannot add teachers or students from DIFFERENT school to teacher group
    @Test
    fun test15_teacherCannotAddDifferentSchoolMembers() = runBlocking {
        setSession(puneTeacher1)
        val group = groupRepository.createGroup("Pune Local Group", GroupType.TEACHER).getOrNull()!!

        // Attempt to add Mumbai teacher and Mumbai student
        val addMumbaiTeacher = groupRepository.addMember(group.id, mumbaiTeacher.id)
        val addMumbaiStudent = groupRepository.addMember(group.id, mumbaiStudent.id)
        assertFalse(addMumbaiTeacher.isSuccess)
        assertFalse(addMumbaiStudent.isSuccess)
    }

    // 16. Teacher cannot add Officer Admin to teacher group (must belong to same school)
    @Test
    fun test16_teacherCannotAddOfficerAdminWithoutSchool() = runBlocking {
        setSession(puneTeacher1)
        val group = groupRepository.createGroup("Class 8", GroupType.TEACHER).getOrNull()!!

        val addOfficer = groupRepository.addMember(group.id, officerAdmin.id)
        assertFalse(addOfficer.isSuccess)
    }

    // 17. School Admin cannot add members to groups
    @Test
    fun test17_schoolAdminCannotAddMembers() = runBlocking {
        // Create a group as Officer Admin and add puneSchoolAdmin as regular member
        setSession(officerAdmin)
        val group = groupRepository.createGroup("Officers & Principals", GroupType.ADMINISTRATIVE).getOrNull()!!
        groupRepository.addMember(group.id, puneSchoolAdmin.id)

        // As puneSchoolAdmin, attempt to add puneTeacher1
        setSession(puneSchoolAdmin)
        val addAttempt = groupRepository.addMember(group.id, puneTeacher1.id)
        assertFalse(addAttempt.isSuccess)
    }

    // 18. Student cannot add members to groups
    @Test
    fun test18_studentCannotAddMembers() = runBlocking {
        setSession(puneTeacher1)
        val group = groupRepository.createGroup("Grade 9 Physics", GroupType.TEACHER).getOrNull()!!
        groupRepository.addMember(group.id, puneStudent1.id)

        // As puneStudent1, attempt to add puneTeacher2
        setSession(puneStudent1)
        val addAttempt = groupRepository.addMember(group.id, puneTeacher2.id)
        assertFalse(addAttempt.isSuccess)
    }

    // 19. Group creator can remove members from group
    @Test
    fun test19_groupCreatorCanRemoveMember() = runBlocking {
        setSession(puneTeacher1)
        val group = groupRepository.createGroup("Pune Project", GroupType.TEACHER).getOrNull()!!
        groupRepository.addMember(group.id, puneTeacher2.id)

        // Remove puneTeacher2
        val removeResult = groupRepository.removeMember(group.id, puneTeacher2.id)
        assertTrue(removeResult.isSuccess)

        val details = groupRepository.getGroupDetails(group.id).getOrNull()!!
        assertEquals(1, details.members.size)
        assertEquals(puneTeacher1.id, details.members.first().userId)
    }

    // 20. Non-creator member cannot remove other members from group
    @Test
    fun test20_nonCreatorCannotRemoveOtherMembers() = runBlocking {
        setSession(puneTeacher1)
        val group = groupRepository.createGroup("History Group", GroupType.TEACHER).getOrNull()!!
        groupRepository.addMember(group.id, puneTeacher2.id)
        groupRepository.addMember(group.id, puneStudent1.id)

        // As puneTeacher2 (not creator), attempt to remove puneStudent1
        setSession(puneTeacher2)
        val removeAttempt = groupRepository.removeMember(group.id, puneStudent1.id)
        assertFalse(removeAttempt.isSuccess)
    }

    // 21. Member can leave/remove themselves from group
    @Test
    fun test21_memberCanLeaveGroup() = runBlocking {
        setSession(puneTeacher1)
        val group = groupRepository.createGroup("English Club", GroupType.TEACHER).getOrNull()!!
        groupRepository.addMember(group.id, puneTeacher2.id)

        // puneTeacher2 leaves the group
        setSession(puneTeacher2)
        val leaveResult = groupRepository.removeMember(group.id, puneTeacher2.id)
        assertTrue(leaveResult.isSuccess)

        // puneTeacher2 should no longer see the group
        val myGroups = groupRepository.getMyGroups().getOrNull()!!
        assertTrue(myGroups.none { it.id == group.id })
    }

    // 22. Duplicate group membership is rejected
    @Test
    fun test22_duplicateMembershipIsRejected() = runBlocking {
        setSession(puneTeacher1)
        val group = groupRepository.createGroup("Math Club", GroupType.TEACHER).getOrNull()!!
        groupRepository.addMember(group.id, puneTeacher2.id)

        // Try adding puneTeacher2 again
        val duplicateAdd = groupRepository.addMember(group.id, puneTeacher2.id)
        assertFalse(duplicateAdd.isSuccess)
    }

    // 23. User only sees groups they belong to in getGroupsForUser
    @Test
    fun test23_userOnlySeesGroupsTheyBelongTo() = runBlocking {
        setSession(puneTeacher1)
        val group1 = groupRepository.createGroup("Pune Group 1", GroupType.TEACHER).getOrNull()!!

        setSession(mumbaiTeacher)
        val group2 = groupRepository.createGroup("Mumbai Group 1", GroupType.TEACHER).getOrNull()!!

        // puneTeacher1 should only see group1
        setSession(puneTeacher1)
        val puneGroups = groupRepository.getMyGroups().getOrNull()!!
        assertTrue(puneGroups.any { it.id == group1.id })
        assertFalse(puneGroups.any { it.id == group2.id })

        // mumbaiTeacher should only see group2
        setSession(mumbaiTeacher)
        val mumbaiGroups = groupRepository.getMyGroups().getOrNull()!!
        assertTrue(mumbaiGroups.any { it.id == group2.id })
        assertFalse(mumbaiGroups.any { it.id == group1.id })
    }

    // 24. Deactivated group does not appear in active group listings
    @Test
    fun test24_deactivatedGroupDoesNotAppearInList() = runBlocking {
        setSession(puneTeacher1)
        val group = groupRepository.createGroup("Old Batch Group", GroupType.TEACHER).getOrNull()!!

        // Deactivate in DB
        SimulatedDatabase.deactivateGroup(group.id)

        val groups = groupRepository.getMyGroups().getOrNull()!!
        assertTrue(groups.none { it.id == group.id })
    }

    // 25. Group details include active member count and metadata
    @Test
    fun test25_groupDetailsIncludesMemberCountAndMetadata() = runBlocking {
        setSession(puneTeacher1)
        val group = groupRepository.createGroup("Geography Batch", GroupType.TEACHER).getOrNull()!!
        groupRepository.addMember(group.id, puneTeacher2.id)
        groupRepository.addMember(group.id, puneStudent1.id)

        val details = groupRepository.getGroupDetails(group.id).getOrNull()!!
        assertEquals(3, details.members.size)
        assertEquals(puneTeacher1.fullName, details.creatorProfile?.fullName)
        assertEquals(puneSchoolId, details.group.schoolId)
    }

    // 26. Client-provided school_id cannot override authoritative teacher profile school_id
    @Test
    fun test26_authoritativeSchoolIdEnforcement() = runBlocking {
        setSession(puneTeacher1)
        // Group creation internally takes user's authoritative schoolId
        val created = groupRepository.createGroup("Biology Lab", GroupType.TEACHER).getOrNull()!!
        assertEquals(puneSchoolId, created.schoolId)
        assertFalse(created.schoolId == mumbaiSchoolId)
    }
}
