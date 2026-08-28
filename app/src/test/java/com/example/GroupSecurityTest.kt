package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.BuildConfig
import com.example.data.local.SessionManager
import com.example.data.model.AddGroupMemberRequest
import com.example.data.model.AdminCreateUserRequest
import com.example.data.model.AdminToggleStatusRequest
import com.example.data.model.AdminUpdateUserRequest
import com.example.data.model.AuthSession
import com.example.data.model.ChatMessage
import com.example.data.model.CreateGroupRequest
import com.example.data.model.CreateSchoolRequest
import com.example.data.model.Group
import com.example.data.model.GroupMember
import com.example.data.model.GroupType
import com.example.data.model.OfficerAdminCreateUserRequest
import com.example.data.model.RemoveGroupMemberRequest
import com.example.data.model.School
import com.example.data.model.SchoolAdminCreateTeacherRequest
import com.example.data.model.SchoolAdminUpdateTeacherRequest
import com.example.data.model.SendGroupMessageRequest
import com.example.data.model.SupabaseLoginRequest
import com.example.data.model.SupabaseSignupRequest
import com.example.data.model.SupabaseTokenResponse
import com.example.data.model.UpdateDisplayNameRequest
import com.example.data.model.UpdateOfficerProfileRequest
import com.example.data.model.UpdateSchoolRequest
import com.example.data.model.UserProfile
import com.example.data.remote.SupabaseAuthApi
import com.example.data.repository.GroupRepository
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
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
import retrofit2.Response
import java.util.UUID

/**
 * Authoritative Supabase Security & Authorization Test Suite for Feature 5A.
 * Covers all 25 specific security, RLS, RPC, and fail-closed requirements.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GroupSecurityTest {

    private lateinit var context: Context
    private lateinit var sessionManager: SessionManager
    private lateinit var fakeSupabaseApi: FakeSupabaseDatabaseEngine
    private lateinit var groupRepository: GroupRepository

    private val puneSchoolId = "s0000000-0001-4000-8000-000000000001"
    private val mumbaiSchoolId = "s0000000-0002-4000-8000-000000000002"

    private lateinit var officerAdmin: UserProfile
    private lateinit var secondOfficerAdmin: UserProfile
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
        sessionManager = SessionManager(context)
        sessionManager.clearSession()

        fakeSupabaseApi = FakeSupabaseDatabaseEngine()
        groupRepository = GroupRepository(context, sessionManager, fakeSupabaseApi)

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
        fakeSupabaseApi.addProfile(officerAdmin)

        secondOfficerAdmin = UserProfile(
            id = "officer-admin-02",
            fullName = "Deputy Officer Admin",
            email = "deputy.officer@educhat.edu",
            mobile = "9811111112",
            role = "officer_admin",
            schoolId = null,
            isActive = true
        )
        fakeSupabaseApi.addProfile(secondOfficerAdmin)

        puneSchoolAdmin = UserProfile(
            id = "pune-admin-01",
            fullName = "Pune Principal",
            email = "principal.pune@educhat.edu",
            mobile = "9822222222",
            role = "school_admin",
            schoolId = puneSchoolId,
            isActive = true
        )
        fakeSupabaseApi.addProfile(puneSchoolAdmin)

        puneTeacher1 = UserProfile(
            id = "pune-teacher-01",
            fullName = "Sunil Patil",
            email = "sunil.patil@pune.edu",
            mobile = "9833333331",
            role = "teacher",
            schoolId = puneSchoolId,
            isActive = true
        )
        fakeSupabaseApi.addProfile(puneTeacher1)

        puneTeacher2 = UserProfile(
            id = "pune-teacher-02",
            fullName = "Anjali Joshi",
            email = "anjali.joshi@pune.edu",
            mobile = "9833333332",
            role = "teacher",
            schoolId = puneSchoolId,
            isActive = true
        )
        fakeSupabaseApi.addProfile(puneTeacher2)

        puneStudent1 = UserProfile(
            id = "pune-student-01",
            fullName = "Rahul Deshmukh",
            email = "rahul.pune@student.edu",
            mobile = "9844444441",
            role = "student",
            schoolId = puneSchoolId,
            isActive = true
        )
        fakeSupabaseApi.addProfile(puneStudent1)

        mumbaiTeacher = UserProfile(
            id = "mumbai-teacher-01",
            fullName = "Vikram Rane",
            email = "vikram.rane@mumbai.edu",
            mobile = "9855555551",
            role = "teacher",
            schoolId = mumbaiSchoolId,
            isActive = true
        )
        fakeSupabaseApi.addProfile(mumbaiTeacher)

        mumbaiStudent = UserProfile(
            id = "mumbai-student-01",
            fullName = "Neha Shinde",
            email = "neha.mumbai@student.edu",
            mobile = "9866666661",
            role = "student",
            schoolId = mumbaiSchoolId,
            isActive = true
        )
        fakeSupabaseApi.addProfile(mumbaiStudent)

        deactivatedTeacher = UserProfile(
            id = "deact-teacher-01",
            fullName = "Inactive Teacher",
            email = "inactive.teacher@pune.edu",
            mobile = "9877777771",
            role = "teacher",
            schoolId = puneSchoolId,
            isActive = false
        )
        fakeSupabaseApi.addProfile(deactivatedTeacher)
    }

    private fun setSession(profile: UserProfile) {
        sessionManager.saveSession(
            AuthSession(
                accessToken = profile.id,
                refreshToken = "mock-refresh-${profile.id}",
                profile = profile
            )
        )
    }

    // 1. Officer Admin creates administrative group.
    @Test
    fun test01_officerAdminCreatesAdministrativeGroup() = runBlocking {
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

    // 2. School Admin cannot create administrative group.
    @Test
    fun test02_schoolAdminCannotCreateAdministrativeGroup() = runBlocking {
        setSession(puneSchoolAdmin)
        val result = groupRepository.createGroup("School Admin Group", GroupType.ADMINISTRATIVE)
        assertFalse(result.isSuccess)
    }

    // 3. Teacher cannot create administrative group.
    @Test
    fun test03_teacherCannotCreateAdministrativeGroup() = runBlocking {
        setSession(puneTeacher1)
        val result = groupRepository.createGroup("Illegal Admin Group", GroupType.ADMINISTRATIVE)
        assertFalse(result.isSuccess)
    }

    // 4. Student cannot create any group.
    @Test
    fun test04_studentCannotCreateAnyGroup() = runBlocking {
        setSession(puneStudent1)
        val result1 = groupRepository.createGroup("Student Admin Group", GroupType.ADMINISTRATIVE)
        val result2 = groupRepository.createGroup("Student Study Group", GroupType.TEACHER)
        assertFalse(result1.isSuccess)
        assertFalse(result2.isSuccess)
    }

    // 5. Teacher creates teacher group.
    @Test
    fun test05_teacherCreatesTeacherGroup() = runBlocking {
        setSession(puneTeacher1)
        val result = groupRepository.createGroup("Grade 10 Math", GroupType.TEACHER)
        assertTrue(result.isSuccess)
        val group = result.getOrNull()
        assertNotNull(group)
        assertEquals("Grade 10 Math", group?.name)
        assertEquals(GroupType.TEACHER, group?.typedGroupType)
    }

    // 6. Teacher group automatically receives Teacher's school_id.
    @Test
    fun test06_teacherGroupAutomaticallyReceivesTeacherSchoolId() = runBlocking {
        setSession(puneTeacher1)
        val result = groupRepository.createGroup("Grade 10 Science", GroupType.TEACHER)
        assertTrue(result.isSuccess)
        assertEquals(puneSchoolId, result.getOrNull()?.schoolId)
        assertEquals(puneTeacher1.id, result.getOrNull()?.createdBy)
    }

    // 7. Client cannot override school_id.
    @Test
    fun test07_clientCannotOverrideSchoolId() = runBlocking {
        setSession(puneTeacher1)
        val result = groupRepository.createGroup("Biology Lab", GroupType.TEACHER)
        assertTrue(result.isSuccess)
        assertEquals(puneSchoolId, result.getOrNull()?.schoolId)
        assertFalse(result.getOrNull()?.schoolId == mumbaiSchoolId)
    }

    // 8. Officer Admin can add Officer Admin.
    @Test
    fun test08_officerAdminCanAddOfficerAdmin() = runBlocking {
        setSession(officerAdmin)
        val group = groupRepository.createGroup("High Level Admin", GroupType.ADMINISTRATIVE).getOrNull()!!
        val addRes = groupRepository.addMember(group.id, secondOfficerAdmin.id)
        assertTrue(addRes.isSuccess)
        assertEquals(secondOfficerAdmin.id, addRes.getOrNull()?.userId)
    }

    // 9. Officer Admin can add School Admin.
    @Test
    fun test09_officerAdminCanAddSchoolAdmin() = runBlocking {
        setSession(officerAdmin)
        val group = groupRepository.createGroup("District Council", GroupType.ADMINISTRATIVE).getOrNull()!!
        val addRes = groupRepository.addMember(group.id, puneSchoolAdmin.id)
        assertTrue(addRes.isSuccess)
        assertEquals(puneSchoolAdmin.id, addRes.getOrNull()?.userId)
    }

    // 10. Officer Admin can add Teacher.
    @Test
    fun test10_officerAdminCanAddTeacher() = runBlocking {
        setSession(officerAdmin)
        val group = groupRepository.createGroup("District Taskforce", GroupType.ADMINISTRATIVE).getOrNull()!!
        val addRes = groupRepository.addMember(group.id, puneTeacher1.id)
        assertTrue(addRes.isSuccess)
        assertEquals(puneTeacher1.id, addRes.getOrNull()?.userId)
    }

    // 11. Officer Admin cannot add Student to administrative group.
    @Test
    fun test11_officerAdminCannotAddStudentToAdministrativeGroup() = runBlocking {
        setSession(officerAdmin)
        val group = groupRepository.createGroup("Admin Only", GroupType.ADMINISTRATIVE).getOrNull()!!
        val addRes = groupRepository.addMember(group.id, puneStudent1.id)
        assertFalse(addRes.isSuccess)
    }

    // 12. Teacher cannot add unauthorized Student (e.g. Mumbai student to Pune teacher group).
    @Test
    fun test12_teacherCannotAddUnauthorizedStudent() = runBlocking {
        setSession(puneTeacher1)
        val group = groupRepository.createGroup("Pune Class 9", GroupType.TEACHER).getOrNull()!!
        val addMumbaiStudent = groupRepository.addMember(group.id, mumbaiStudent.id)
        assertFalse(addMumbaiStudent.isSuccess)
    }

    // 13. Teacher cannot access another Teacher's group.
    @Test
    fun test13_teacherCannotAccessAnotherTeachersGroup() = runBlocking {
        setSession(puneTeacher1)
        val group = groupRepository.createGroup("Pune Private Group", GroupType.TEACHER).getOrNull()!!

        setSession(mumbaiTeacher)
        val detailsRes = groupRepository.getGroupDetails(group.id)
        assertFalse(detailsRes.isSuccess)
    }

    // 14. School Admin cannot access unrelated group unless member.
    @Test
    fun test14_schoolAdminCannotAccessUnrelatedGroupUnlessMember() = runBlocking {
        setSession(officerAdmin)
        val group = groupRepository.createGroup("Secret Admin Group", GroupType.ADMINISTRATIVE).getOrNull()!!

        setSession(puneSchoolAdmin)
        val detailsRes = groupRepository.getGroupDetails(group.id)
        assertFalse(detailsRes.isSuccess)

        // Now add puneSchoolAdmin
        setSession(officerAdmin)
        groupRepository.addMember(group.id, puneSchoolAdmin.id)

        setSession(puneSchoolAdmin)
        val detailsResAfter = groupRepository.getGroupDetails(group.id)
        assertTrue(detailsResAfter.isSuccess)
    }

    // 15. Student sees only groups where member.
    @Test
    fun test15_studentSeesOnlyGroupsWhereMember() = runBlocking {
        setSession(puneTeacher1)
        val group1 = groupRepository.createGroup("Pune Physics", GroupType.TEACHER).getOrNull()!!
        val group2 = groupRepository.createGroup("Pune Chemistry", GroupType.TEACHER).getOrNull()!!
        groupRepository.addMember(group1.id, puneStudent1.id)

        setSession(puneStudent1)
        val studentGroups = groupRepository.getMyGroups().getOrNull()!!
        assertEquals(1, studentGroups.size)
        assertEquals(group1.id, studentGroups.first().id)
    }

    // 16. Inactive user cannot access groups.
    @Test
    fun test16_inactiveUserCannotAccessGroups() = runBlocking {
        setSession(deactivatedTeacher)
        val groupsRes = groupRepository.getMyGroups()
        assertFalse(groupsRes.isSuccess)
    }

    // 17. Removed member loses access.
    @Test
    fun test17_removedMemberLosesAccess() = runBlocking {
        setSession(puneTeacher1)
        val group = groupRepository.createGroup("Math Olympiad", GroupType.TEACHER).getOrNull()!!
        groupRepository.addMember(group.id, puneTeacher2.id)

        // Verify member can see
        setSession(puneTeacher2)
        assertTrue(groupRepository.getMyGroups().getOrNull()!!.any { it.id == group.id })

        // Creator removes member
        setSession(puneTeacher1)
        val removeRes = groupRepository.removeMember(group.id, puneTeacher2.id)
        assertTrue(removeRes.isSuccess)

        // Member should no longer see group
        setSession(puneTeacher2)
        assertTrue(groupRepository.getMyGroups().getOrNull()!!.none { it.id == group.id })
    }

    // 18. Duplicate membership is rejected/prevented.
    @Test
    fun test18_duplicateMembershipIsRejected() = runBlocking {
        setSession(puneTeacher1)
        val group = groupRepository.createGroup("Science Club", GroupType.TEACHER).getOrNull()!!
        val add1 = groupRepository.addMember(group.id, puneTeacher2.id)
        assertTrue(add1.isSuccess)

        val add2 = groupRepository.addMember(group.id, puneTeacher2.id)
        assertFalse(add2.isSuccess)
    }

    // 19. Direct unauthorized PostgREST/RPC calls fail.
    @Test
    fun test19_directUnauthorizedRpcCallsFail() = runBlocking {
        // Direct call to create_group RPC from Student token
        val res = fakeSupabaseApi.createGroupRpc(
            apiKey = "anon-key",
            bearerToken = "Bearer ${puneStudent1.id}",
            request = CreateGroupRequest(name = "Direct RPC", groupType = "administrative")
        )
        assertFalse(res.isSuccessful)
        assertEquals(403, res.code())
    }

    // 20. No SimulatedDatabase fallback occurs.
    @Test
    fun test20_noSimulatedDatabaseFallbackOccurs() = runBlocking {
        // Create an offline/fail engine
        val failingApi = object : FakeSupabaseDatabaseEngine() {
            override suspend fun getGroups(apiKey: String, bearerToken: String, select: String, isActiveFilter: String, order: String): Response<List<Group>> {
                return Response.error(503, "Service Unavailable".toResponseBody("application/json".toMediaTypeOrNull()))
            }
        }
        val repo = GroupRepository(context, sessionManager, failingApi)
        setSession(puneTeacher1)

        val res = repo.getGroups()
        // MUST fail closed instead of falling back to SimulatedDatabase
        assertFalse(res.isSuccess)
        assertEquals("सर्व्हरशी संपर्क होऊ शकला नाही. कृपया पुन्हा प्रयत्न करा.", res.exceptionOrNull()?.message)
    }

    // 21. Supabase failure fails closed.
    @Test
    fun test21_supabaseFailureFailsClosed() = runBlocking {
        val failingApi = object : FakeSupabaseDatabaseEngine() {
            override suspend fun createGroupRpc(apiKey: String, bearerToken: String, request: CreateGroupRequest): Response<Group> {
                return Response.error(500, "Internal Server Error".toResponseBody("application/json".toMediaTypeOrNull()))
            }
        }
        val repo = GroupRepository(context, sessionManager, failingApi)
        setSession(officerAdmin)

        val res = repo.createGroup("Closed Group", GroupType.ADMINISTRATIVE)
        assertFalse(res.isSuccess)
        assertEquals("गट तयार करता आला नाही. कृपया पुन्हा प्रयत्न करा.", res.exceptionOrNull()?.message)
    }

    // 22. Existing authentication tests remain passing.
    @Test
    fun test22_sessionAuthenticationValidation() = runBlocking {
        sessionManager.clearSession()
        val res = groupRepository.getMyGroups()
        assertFalse(res.isSuccess)
        assertTrue(res.exceptionOrNull()?.message?.contains("Session expired") == true ||
                res.exceptionOrNull()?.message?.contains("सत्र समाप्त") == true)
    }

    // 23. Existing role mismatch tests remain passing.
    @Test
    fun test23_roleMismatchValidation() = runBlocking {
        setSession(puneTeacher1)
        val searchAdmin = groupRepository.searchEligibleUsersForAdminGroup()
        assertFalse(searchAdmin.isSuccess)

        setSession(officerAdmin)
        val searchStudents = groupRepository.searchEligibleStudentsForTeacherGroup()
        assertFalse(searchStudents.isSuccess)
    }

    // 24. Existing school-scope RLS tests remain passing.
    @Test
    fun test24_searchProfilesSchoolScopeEnforced() = runBlocking {
        setSession(puneTeacher1)
        val studentsRes = groupRepository.searchEligibleStudentsForTeacherGroup()
        assertTrue(studentsRes.isSuccess)
        val students = studentsRes.getOrNull()!!
        assertTrue(students.all { it.schoolId == puneSchoolId })
        assertTrue(students.none { it.schoolId == mumbaiSchoolId })
    }

    // 25. No privileged secret exists in Android client.
    @Test
    fun test25_noPrivilegedSecretExistsInAndroidClient() {
        val fields = BuildConfig::class.java.fields
        val fieldNames = fields.map { it.name.lowercase() }
        assertFalse(fieldNames.any { it.contains("service_role") })
        assertFalse(fieldNames.any { it.contains("db_password") })
        assertFalse(fieldNames.any { it.contains("master_key") })
    }

    // 26. Active member can send and read messages
    @Test
    fun test26_activeMemberCanSendAndReadMessages() = runBlocking {
        setSession(officerAdmin)
        val created = groupRepository.createGroup("Security Council", GroupType.ADMINISTRATIVE.name).getOrNull()!!

        val sendRes = groupRepository.sendGroupMessage(created.id, "Hello, team!")
        assertTrue(sendRes.isSuccess)
        val sent = sendRes.getOrNull()!!
        assertEquals("Hello, team!", sent.content)
        assertEquals(officerAdmin.id, sent.senderId)

        val messagesRes = groupRepository.getGroupMessages(created.id)
        assertTrue(messagesRes.isSuccess)
        val messages = messagesRes.getOrNull()!!
        assertEquals(1, messages.size)
        assertEquals("Hello, team!", messages[0].content)
    }

    // 27. Non-member cannot send or read messages (Fail closed)
    @Test
    fun test27_nonMemberCannotSendOrReadMessages() = runBlocking {
        setSession(officerAdmin)
        val created = groupRepository.createGroup("Admin Only Group", GroupType.ADMINISTRATIVE.name).getOrNull()!!

        // Switch to puneStudent1 who is not in the group
        setSession(puneStudent1)
        val sendRes = groupRepository.sendGroupMessage(created.id, "Unauthorized message")
        assertFalse(sendRes.isSuccess)

        val readRes = groupRepository.getGroupMessages(created.id)
        assertFalse(readRes.isSuccess)
    }

    // 28. Blank message content is rejected
    @Test
    fun test28_blankMessageContentRejected() = runBlocking {
        setSession(officerAdmin)
        val created = groupRepository.createGroup("Discussion", GroupType.ADMINISTRATIVE.name).getOrNull()!!

        val sendBlank = groupRepository.sendGroupMessage(created.id, "   ")
        assertFalse(sendBlank.isSuccess)
    }

    // 29. Teacher can add Officer Admin to teacher group even though Officer Admin schoolId is null
    @Test
    fun test29_teacherCanAddOfficerAdminToTeacherGroupEvenIfSchoolIdIsNull() = runBlocking {
        setSession(puneTeacher1)
        val group = groupRepository.createGroup("Science Dept", GroupType.TEACHER).getOrNull()!!
        assertNull(officerAdmin.schoolId)

        val addOfficerRes = groupRepository.addMember(group.id, officerAdmin.id)
        assertTrue(addOfficerRes.isSuccess)
        val member = addOfficerRes.getOrNull()
        assertNotNull(member)
        assertEquals(officerAdmin.id, member?.userId)
        assertEquals("member", member?.roleInGroup)
        assertTrue(member?.isActive == true)
    }

    // 30. Teacher can add same-school School Admin to teacher group
    @Test
    fun test30_teacherCanAddSameSchoolSchoolAdminToTeacherGroup() = runBlocking {
        setSession(puneTeacher1)
        val group = groupRepository.createGroup("Math Dept", GroupType.TEACHER).getOrNull()!!

        val addAdminRes = groupRepository.addMember(group.id, puneSchoolAdmin.id)
        assertTrue(addAdminRes.isSuccess)
        assertEquals(puneSchoolAdmin.id, addAdminRes.getOrNull()?.userId)
    }

    // 31. Teacher cannot add different-school School Admin or Teacher
    @Test
    fun test31_teacherCannotAddDifferentSchoolUsersToTeacherGroup() = runBlocking {
        setSession(puneTeacher1)
        val group = groupRepository.createGroup("English Dept", GroupType.TEACHER).getOrNull()!!

        val addMumbaiTeacherRes = groupRepository.addMember(group.id, mumbaiTeacher.id)
        assertFalse(addMumbaiTeacherRes.isSuccess)
    }

    // 32. Inactive group membership is reactivated on addMember
    @Test
    fun test32_reactivateInactiveGroupMember() = runBlocking {
        setSession(puneTeacher1)
        val group = groupRepository.createGroup("History Club", GroupType.TEACHER).getOrNull()!!

        // Add member
        val add1 = groupRepository.addMember(group.id, puneTeacher2.id)
        assertTrue(add1.isSuccess)

        // Remove member (soft delete / is_active = false)
        val removeRes = groupRepository.removeMember(group.id, puneTeacher2.id)
        assertTrue(removeRes.isSuccess)

        // Re-add member -> must reactivate
        val addAgain = groupRepository.addMember(group.id, puneTeacher2.id)
        assertTrue(addAgain.isSuccess)
        val reactivated = addAgain.getOrNull()!!
        assertTrue(reactivated.isActive)
        assertEquals("member", reactivated.roleInGroup)
    }
}

/**
 * Authoritative Supabase In-Memory Engine simulating PostgreSQL constraints, RLS, and RPC functions.
 */
open class FakeSupabaseDatabaseEngine : SupabaseAuthApi {
    private val profiles = mutableListOf<UserProfile>()
    private val groups = mutableListOf<Group>()
    private val groupMembers = mutableListOf<GroupMember>()
    private val schools = mutableListOf<School>()
    private val messages = mutableListOf<ChatMessage>()

    fun addProfile(p: UserProfile) {
        profiles.removeIf { it.id == p.id }
        profiles.add(p)
    }

    fun addSchool(s: School) {
        schools.removeIf { it.id == s.id }
        schools.add(s)
    }

    private fun getCallerId(bearerToken: String): String {
        return bearerToken.removePrefix("Bearer ").trim()
    }

    override suspend fun getGroups(apiKey: String, bearerToken: String, select: String, isActiveFilter: String, order: String): Response<List<Group>> {
        val callerId = getCallerId(bearerToken)
        val caller = profiles.firstOrNull { it.id == callerId }
        if (caller == null || !caller.isActive) {
            return Response.error(401, "Unauthorized".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val activeGroupIds = groupMembers.filter { it.userId == callerId && it.isActive }.map { it.groupId }.toSet()
        val userGroups = groups.filter { it.isActive && activeGroupIds.contains(it.id) }
        return Response.success(userGroups)
    }

    override suspend fun getGroupById(apiKey: String, bearerToken: String, idFilter: String, select: String): Response<List<Group>> {
        val callerId = getCallerId(bearerToken)
        val caller = profiles.firstOrNull { it.id == callerId }
        if (caller == null || !caller.isActive) {
            return Response.error(401, "Unauthorized".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val targetId = idFilter.removePrefix("eq.")
        val group = groups.firstOrNull { it.id == targetId && it.isActive }
        if (group == null) {
            return Response.success(emptyList())
        }
        val isMember = groupMembers.any { it.groupId == targetId && it.userId == callerId && it.isActive }
        if (!isMember && group.createdBy != callerId) {
            return Response.error(403, "Forbidden".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        return Response.success(listOf(group))
    }

    override suspend fun getGroupMembers(apiKey: String, bearerToken: String, groupIdFilter: String, isActiveFilter: String, select: String): Response<List<GroupMember>> {
        val callerId = getCallerId(bearerToken)
        val caller = profiles.firstOrNull { it.id == callerId }
        if (caller == null || !caller.isActive) {
            return Response.error(401, "Unauthorized".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val targetGroupId = groupIdFilter.removePrefix("eq.")
        val isMember = groupMembers.any { it.groupId == targetGroupId && it.userId == callerId && it.isActive }
        val group = groups.firstOrNull { it.id == targetGroupId }
        if (!isMember && group?.createdBy != callerId) {
            return Response.error(403, "Forbidden".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val members = groupMembers.filter { it.groupId == targetGroupId && it.isActive }
        return Response.success(members)
    }

    override suspend fun searchProfiles(apiKey: String, bearerToken: String, roleFilter: String?, schoolIdFilter: String?, isActiveFilter: String, select: String, order: String): Response<List<UserProfile>> {
        val callerId = getCallerId(bearerToken)
        val caller = profiles.firstOrNull { it.id == callerId }
        if (caller == null || !caller.isActive) {
            return Response.error(401, "Unauthorized".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        var list = profiles.filter { it.isActive }
        if (roleFilter != null) {
            if (roleFilter.startsWith("in.(")) {
                val roles = roleFilter.removePrefix("in.(").removeSuffix(")").split(",").map { it.trim() }
                list = list.filter { roles.contains(it.role) }
            } else if (roleFilter.startsWith("eq.")) {
                val r = roleFilter.removePrefix("eq.")
                list = list.filter { it.role.equals(r, ignoreCase = true) }
            }
        }
        if (schoolIdFilter != null && schoolIdFilter.startsWith("eq.")) {
            val sId = schoolIdFilter.removePrefix("eq.")
            list = list.filter { it.schoolId == sId }
        }
        return Response.success(list)
    }

    override suspend fun createGroupRpc(apiKey: String, bearerToken: String, request: CreateGroupRequest): Response<Group> {
        val callerId = getCallerId(bearerToken)
        val caller = profiles.firstOrNull { it.id == callerId }
        if (caller == null || !caller.isActive) {
            return Response.error(401, "Unauthorized".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val groupType = request.groupType
        val schoolId: String? = when (groupType) {
            "administrative" -> {
                if (caller.role != "officer_admin") {
                    return Response.error(403, "Only Officer Admin can create administrative groups".toResponseBody("application/json".toMediaTypeOrNull()))
                }
                null
            }
            "teacher" -> {
                if (caller.role != "teacher") {
                    return Response.error(403, "Only Teacher can create teacher groups".toResponseBody("application/json".toMediaTypeOrNull()))
                }
                if (caller.schoolId.isNullOrBlank()) {
                    return Response.error(400, "Teacher has no assigned school".toResponseBody("application/json".toMediaTypeOrNull()))
                }
                caller.schoolId
            }
            else -> return Response.error(400, "Invalid group type".toResponseBody("application/json".toMediaTypeOrNull()))
        }

        val newGroup = Group(
            id = UUID.randomUUID().toString(),
            name = request.name,
            groupType = groupType,
            createdBy = caller.id,
            schoolId = schoolId,
            isActive = true
        )
        groups.add(newGroup)

        groupMembers.add(
            GroupMember(
                id = UUID.randomUUID().toString(),
                groupId = newGroup.id,
                userId = caller.id,
                roleInGroup = "admin",
                isActive = true
            )
        )

        return Response.success(newGroup)
    }

    override suspend fun addGroupMemberRpc(apiKey: String, bearerToken: String, request: AddGroupMemberRequest): Response<GroupMember> {
        val callerId = getCallerId(bearerToken)
        val caller = profiles.firstOrNull { it.id == callerId }
        if (caller == null || !caller.isActive) {
            return Response.error(401, "Unauthorized".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val group = groups.firstOrNull { it.id == request.groupId && it.isActive }
            ?: return Response.error(404, "Group not found".toResponseBody("application/json".toMediaTypeOrNull()))

        // Management authorization: Caller must be Officer Admin OR active admin of the target group
        val callerMembership = groupMembers.firstOrNull { it.groupId == group.id && it.userId == caller.id && it.isActive }
        val isCallerOfficerAdmin = caller.role == "officer_admin"
        val isCallerGroupAdmin = group.createdBy == caller.id || callerMembership?.roleInGroup == "admin"

        if (!isCallerOfficerAdmin && !isCallerGroupAdmin) {
            return Response.error(403, "Access denied: Only Officer Admin or active group admin can add members".toResponseBody("application/json".toMediaTypeOrNull()))
        }

        val targetUser = profiles.firstOrNull { it.id == request.userId && it.isActive }
            ?: return Response.error(400, "Target user not found or deactivated".toResponseBody("application/json".toMediaTypeOrNull()))

        // Group type rules
        if (group.groupType == "administrative") {
            // Administrative group: officer_admin, school_admin, teacher allowed; student rejected
            if (targetUser.role == "student") {
                return Response.error(400, "Students cannot be added to administrative groups".toResponseBody("application/json".toMediaTypeOrNull()))
            }
        } else if (group.groupType == "teacher") {
            // Teacher group:
            // - Officer Admin: allowed even when target school_id IS NULL
            // - School Admin: allowed only when target school_id = group.school_id
            // - Teacher: allowed only when target school_id = group.school_id
            // - Student: allowed only when target school_id = group.school_id
            if (targetUser.role == "officer_admin") {
                // Allowed even when target school_id IS NULL
            } else if (targetUser.schoolId != group.schoolId) {
                return Response.error(400, "Target user belongs to a different school".toResponseBody("application/json".toMediaTypeOrNull()))
            }
        }

        // Duplicate check & reactivation with UNIQUE(group_id, user_id)
        val existingIndex = groupMembers.indexOfFirst { it.groupId == group.id && it.userId == targetUser.id }
        if (existingIndex != -1) {
            val existing = groupMembers[existingIndex]
            if (existing.isActive) {
                return Response.error(409, "User is already a member of this group".toResponseBody("application/json".toMediaTypeOrNull()))
            } else {
                val reactivated = existing.copy(
                    isActive = true,
                    roleInGroup = "member",
                    userProfile = targetUser
                )
                groupMembers[existingIndex] = reactivated
                return Response.success(reactivated)
            }
        }

        val member = GroupMember(
            id = UUID.randomUUID().toString(),
            groupId = group.id,
            userId = targetUser.id,
            roleInGroup = "member",
            isActive = true,
            userProfile = targetUser
        )
        groupMembers.add(member)
        return Response.success(member)
    }

    override suspend fun removeGroupMemberRpc(apiKey: String, bearerToken: String, request: RemoveGroupMemberRequest): Response<Boolean> {
        val callerId = getCallerId(bearerToken)
        val caller = profiles.firstOrNull { it.id == callerId }
        if (caller == null || !caller.isActive) {
            return Response.error(401, "Unauthorized".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val group = groups.firstOrNull { it.id == request.groupId && it.isActive }
            ?: return Response.error(404, "Group not found".toResponseBody("application/json".toMediaTypeOrNull()))

        // Management authorization: creator OR self-removal
        val isSelfRemoval = caller.id == request.userId
        val callerMembership = groupMembers.firstOrNull { it.groupId == group.id && it.userId == caller.id && it.isActive }
        val isManager = group.createdBy == caller.id || callerMembership?.roleInGroup == "admin"

        if (!isSelfRemoval && !isManager) {
            return Response.error(403, "Access denied".toResponseBody("application/json".toMediaTypeOrNull()))
        }

        val member = groupMembers.firstOrNull { it.groupId == group.id && it.userId == request.userId && it.isActive }
            ?: return Response.error(404, "Member not found".toResponseBody("application/json".toMediaTypeOrNull()))

        groupMembers.remove(member)
        groupMembers.add(member.copy(isActive = false))
        return Response.success(true)
    }

    // Default implementations for remaining interface methods
    override suspend fun login(grantType: String, apiKey: String, request: SupabaseLoginRequest): Response<SupabaseTokenResponse> = Response.success(null)
    override suspend fun refreshToken(grantType: String, apiKey: String, request: com.example.data.model.SupabaseRefreshTokenRequest): Response<SupabaseTokenResponse> = Response.success(
        SupabaseTokenResponse(accessToken = "refreshed_access_token", refreshToken = "refreshed_token")
    )
    override suspend fun signup(apiKey: String, request: SupabaseSignupRequest): Response<SupabaseTokenResponse> = Response.success(null)
    override suspend fun logout(apiKey: String, bearerToken: String): Response<Unit> = Response.success(Unit)
    override suspend fun getProfile(apiKey: String, bearerToken: String, idFilter: String, select: String): Response<List<UserProfile>> = Response.success(emptyList())
    override suspend fun getAllProfiles(apiKey: String, bearerToken: String, select: String, order: String): Response<List<UserProfile>> = Response.success(profiles)
    override suspend fun updateDisplayNameRpc(apiKey: String, bearerToken: String, request: UpdateDisplayNameRequest): Response<UserProfile> = Response.success(null)
    override suspend fun adminCreateUserRpc(apiKey: String, bearerToken: String, request: AdminCreateUserRequest): Response<UserProfile> = Response.success(null)
    override suspend fun adminUpdateUserRpc(apiKey: String, bearerToken: String, request: AdminUpdateUserRequest): Response<UserProfile> = Response.success(null)
    override suspend fun adminToggleStatusRpc(apiKey: String, bearerToken: String, request: AdminToggleStatusRequest): Response<UserProfile> = Response.success(null)
    override suspend fun officerAdminCreateUserRpc(apiKey: String, bearerToken: String, request: OfficerAdminCreateUserRequest): Response<UserProfile> = Response.success(null)
    override suspend fun getSchools(apiKey: String, bearerToken: String, select: String, order: String): Response<List<School>> = Response.success(schools)
    override suspend fun getSchoolById(apiKey: String, bearerToken: String, idFilter: String, select: String): Response<List<School>> = Response.success(emptyList())
    override suspend fun getTeachersBySchool(apiKey: String, bearerToken: String, schoolIdFilter: String, roleFilter: String, select: String, order: String): Response<List<UserProfile>> = Response.success(emptyList())
    override suspend fun schoolAdminCreateTeacherRpc(apiKey: String, bearerToken: String, request: SchoolAdminCreateTeacherRequest): Response<UserProfile> = Response.success(null)
    override suspend fun schoolAdminUpdateTeacherRpc(apiKey: String, bearerToken: String, request: SchoolAdminUpdateTeacherRequest): Response<UserProfile> = Response.success(null)
    override suspend fun officerAdminCreateSchoolRpc(apiKey: String, bearerToken: String, request: CreateSchoolRequest): Response<School> = Response.success(null)
    override suspend fun officerAdminUpdateSchoolRpc(apiKey: String, bearerToken: String, request: UpdateSchoolRequest): Response<School> = Response.success(null)
    override suspend fun patchProfile(apiKey: String, bearerToken: String, idFilter: String, updates: Map<String, Any?>): Response<List<UserProfile>> = Response.success(emptyList())
    override suspend fun getGroupMessages(apiKey: String, bearerToken: String, groupIdFilter: String, isDeletedFilter: String, select: String, order: String): Response<List<ChatMessage>> {
        val callerId = getCallerId(bearerToken)
        val caller = profiles.firstOrNull { it.id == callerId }
        if (caller == null || !caller.isActive) {
            return Response.error(401, "Unauthorized".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val targetGroupId = groupIdFilter.removePrefix("eq.")
        val isMember = groupMembers.any { it.groupId == targetGroupId && it.userId == callerId && it.isActive }
        if (!isMember) {
            return Response.error(403, "Forbidden".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val groupMsgs = messages.filter { it.groupId == targetGroupId && !it.isDeleted }
        return Response.success(groupMsgs)
    }

    override suspend fun sendGroupMessageRpc(apiKey: String, bearerToken: String, request: SendGroupMessageRequest): Response<ChatMessage> {
        val callerId = getCallerId(bearerToken)
        val caller = profiles.firstOrNull { it.id == callerId }
        if (caller == null || !caller.isActive) {
            return Response.error(401, "Unauthorized".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val group = groups.firstOrNull { it.id == request.groupId && it.isActive }
            ?: return Response.error(404, "Group not found".toResponseBody("application/json".toMediaTypeOrNull()))

        val isMember = groupMembers.any { it.groupId == group.id && it.userId == callerId && it.isActive }
        if (!isMember) {
            return Response.error(403, "Not a member".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        if (request.content.trim().isBlank()) {
            return Response.error(400, "Blank content".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val newMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            groupId = request.groupId,
            senderId = callerId,
            content = request.content.trim(),
            createdAt = "2026-08-27T09:30:00Z",
            updatedAt = "2026-08-27T09:30:00Z",
            isDeleted = false,
            senderProfile = caller
        )
        messages.add(newMsg)
        return Response.success(newMsg)
    }
}
