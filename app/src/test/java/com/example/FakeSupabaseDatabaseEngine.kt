package com.example

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
import com.example.data.model.OfficerAdminCreateUserRequest
import com.example.data.model.RemoveGroupMemberRequest
import com.example.data.model.School
import com.example.data.model.SchoolAdminCreateStudentRequest
import com.example.data.model.SchoolAdminCreateTeacherRequest
import com.example.data.model.SchoolAdminUpdateStudentRequest
import com.example.data.model.SchoolAdminUpdateTeacherRequest
import com.example.data.model.SendGroupMessageRequest
import com.example.data.model.SupabaseAuthUser
import com.example.data.model.SupabaseLoginRequest
import com.example.data.model.SupabaseRefreshTokenRequest
import com.example.data.model.SupabaseSignupRequest
import com.example.data.model.SupabaseSignupResponse
import com.example.data.model.SupabaseTokenResponse
import com.example.data.model.UpdateDisplayNameRequest
import com.example.data.model.UpdateSchoolRequest
import com.example.data.model.UserProfile
import com.example.data.remote.SupabaseAuthApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.util.UUID

/**
 * Authoritative Fake Supabase Database Engine for Unit/Robolectric Tests.
 * Simulates PostgreSQL tables, Row-Level Security, RPC functions, and GoTrue Auth in-memory.
 */
open class FakeSupabaseDatabaseEngine : SupabaseAuthApi {

    data class FakeUserRecord(
        val email: String,
        val password: String,
        var profile: UserProfile
    )

    private val userRecords = mutableMapOf<String, FakeUserRecord>() // email -> record
    private val profiles = mutableMapOf<String, UserProfile>() // id -> profile
    private val schools = mutableMapOf<String, School>() // id -> school
    private val groups = mutableListOf<Group>()
    private val groupMembers = mutableListOf<GroupMember>()
    private val messages = mutableListOf<ChatMessage>()

    val puneSchoolId = "s0000000-0001-4000-8000-000000000001"
    val thaneSchoolId = "s0000000-0002-4000-8000-000000000002"
    val nagpurSchoolId = "s0000000-0003-4000-8000-000000000003"
    val mumbaiSchoolId = "s0000000-0002-4000-8000-000000000002"

    init {
        reset()
    }

    fun reset() {
        userRecords.clear()
        profiles.clear()
        schools.clear()
        groups.clear()
        groupMembers.clear()
        messages.clear()

        // Seed default schools
        val puneSchool = School(
            id = puneSchoolId,
            name = "जिल्हा परिषद प्राथमिक शाळा, पुणे (ZP Primary School)",
            code = "SCH-PUN-001",
            mobile = "9822012345",
            email = "zp.pune.01@educhat.edu",
            address = "शिवाजीनगर, पुणे, महाराष्ट्र ४११०१६",
            isActive = true,
            createdAt = "2026-01-01T08:00:00Z",
            updatedAt = "2026-01-01T08:00:00Z"
        )
        val thaneSchool = School(
            id = thaneSchoolId,
            name = "शासकीय माध्यमिक विद्यालय, ठाणे (Govt High School)",
            code = "SCH-THA-002",
            mobile = "9822067890",
            email = "govt.thane.02@educhat.edu",
            address = "नौपाडा, ठाणे (पश्चिम), महाराष्ट्र ४००६०२",
            isActive = true,
            createdAt = "2026-01-05T09:00:00Z",
            updatedAt = "2026-01-05T09:00:00Z"
        )
        val nagpurSchool = School(
            id = nagpurSchoolId,
            name = "महात्मा फुले विद्यालय, नागपूर (Phule Vidya Mandir)",
            code = "SCH-NAG-003",
            mobile = "9822099887",
            email = "phule.nagpur.03@educhat.edu",
            address = "धरमपेठ, नागपूर, महाराष्ट्र ४४००१०",
            isActive = false,
            createdAt = "2026-01-10T10:00:00Z",
            updatedAt = "2026-01-10T10:00:00Z"
        )
        schools[puneSchool.id] = puneSchool
        schools[thaneSchool.id] = thaneSchool
        schools[nagpurSchool.id] = nagpurSchool

        // Seed default users
        val teacher = UserProfile(
            id = "d0a1b2c3-0001-4000-8000-000000000001",
            email = "teacher@educhat.edu",
            fullName = "Prof. Sarah Jenkins",
            mobile = "9822012345",
            role = "teacher",
            isActive = true,
            isPrimaryAdmin = false,
            schoolId = puneSchoolId,
            createdAt = "2026-01-15T08:00:00Z",
            updatedAt = "2026-01-15T08:00:00Z"
        )
        addUser("teacher@educhat.edu", "password123", teacher)

        val student = UserProfile(
            id = "d0a1b2c3-0002-4000-8000-000000000002",
            email = "student@educhat.edu",
            fullName = "Alex Rivera (प्रतीक मोरे)",
            mobile = "9822054321",
            standard = "इयत्ता १० वी (Class 10-A)",
            role = "student",
            isActive = true,
            isPrimaryAdmin = false,
            schoolId = puneSchoolId,
            createdAt = "2026-02-01T09:30:00Z",
            updatedAt = "2026-02-01T09:30:00Z"
        )
        addUser("student@educhat.edu", "password123", student)

        val admin = UserProfile(
            id = "d0a1b2c3-0003-4000-8000-000000000003",
            email = "admin@educhat.edu",
            fullName = "Primary Officer Admin",
            mobile = "9800112233",
            role = "officer_admin",
            isActive = true,
            isPrimaryAdmin = true,
            schoolId = null,
            createdAt = "2025-12-01T12:00:00Z",
            updatedAt = "2025-12-01T12:00:00Z"
        )
        addUser("admin@educhat.edu", "password123", admin)

        val schoolAdmin = UserProfile(
            id = "d0a1b2c3-0008-4000-8000-000000000008",
            email = "schooladmin@educhat.edu",
            fullName = "Default School Admin",
            mobile = "9800112244",
            role = "school_admin",
            isActive = true,
            isPrimaryAdmin = false,
            schoolId = puneSchoolId,
            createdAt = "2025-12-01T12:00:00Z",
            updatedAt = "2025-12-01T12:00:00Z"
        )
        addUser("schooladmin@educhat.edu", "password123", schoolAdmin)

        val inactive = UserProfile(
            id = "d0a1b2c3-0004-4000-8000-000000000004",
            email = "inactive@educhat.edu",
            fullName = "John Suspended (अमित पवार)",
            mobile = "9899001122",
            standard = "इयत्ता १० वी (Class 10)",
            role = "student",
            isActive = false,
            schoolId = thaneSchoolId,
            createdAt = "2026-01-10T10:00:00Z",
            updatedAt = "2026-01-10T10:00:00Z"
        )
        addUser("inactive@educhat.edu", "password123", inactive)

        val snehal = UserProfile(
            id = "d0a1b2c3-0005-4000-8000-000000000005",
            email = "snehal.patil@educhat.edu",
            fullName = "Snehal Patil (स्नेहल पाटील)",
            mobile = "9822077889",
            standard = "इयत्ता १० वी (Class 10-A)",
            role = "student",
            isActive = true,
            schoolId = puneSchoolId,
            createdAt = "2026-02-05T11:00:00Z",
            updatedAt = "2026-02-05T11:00:00Z"
        )
        addUser("snehal.patil@educhat.edu", "password123", snehal)

        val rohan = UserProfile(
            id = "d0a1b2c3-0006-4000-8000-000000000006",
            email = "rohan.deshmukh@educhat.edu",
            fullName = "Rohan Deshmukh (रोहन देशमुख)",
            mobile = "9822033445",
            standard = "इयत्ता ९ वी (Class 9-B)",
            role = "student",
            isActive = true,
            schoolId = puneSchoolId,
            createdAt = "2026-02-10T14:20:00Z",
            updatedAt = "2026-02-10T14:20:00Z"
        )
        addUser("rohan.deshmukh@educhat.edu", "password123", rohan)

        val pooja = UserProfile(
            id = "d0a1b2c3-0007-4000-8000-000000000007",
            email = "pooja.shinde@educhat.edu",
            fullName = "Pooja Shinde (पूजा शिंदे)",
            mobile = "9822066778",
            standard = "इयत्ता ८ वी (Class 8-C)",
            role = "student",
            isActive = true,
            schoolId = puneSchoolId,
            createdAt = "2026-02-12T16:00:00Z",
            updatedAt = "2026-02-12T16:00:00Z"
        )
        addUser("pooja.shinde@educhat.edu", "password123", pooja)
    }

    fun addUser(email: String, pass: String, profile: UserProfile) {
        val normalizedEmail = email.trim().lowercase()
        val record = FakeUserRecord(normalizedEmail, pass, profile)
        userRecords[normalizedEmail] = record
        profiles[profile.id] = profile
    }

    fun addProfile(p: UserProfile) {
        profiles[p.id] = p
        val email = p.email?.trim()?.lowercase()
        if (email != null && userRecords.containsKey(email)) {
            val existing = userRecords[email]!!
            userRecords[email] = existing.copy(profile = p)
        } else if (email != null) {
            userRecords[email] = FakeUserRecord(email, "password123", p)
        }
    }

    fun addSchool(s: School) {
        schools[s.id] = s
    }

    fun findSchoolById(id: String): School? = schools[id]

    fun deleteSchool(id: String): Boolean = schools.remove(id) != null

    fun deleteSchoolWithRestrictCheck(schoolId: String): Result<Boolean> {
        val hasUsers = profiles.values.any { it.schoolId == schoolId }
        if (hasUsers) {
            return Result.failure(
                IllegalStateException("FOREIGN KEY CONSTRAINT VIOLATION: Cannot delete school $schoolId because active or inactive user records reference it. Postgres constraint 'profiles_school_id_fkey' is ON DELETE RESTRICT.")
            )
        }
        val removed = schools.remove(schoolId)
        return if (removed != null) Result.success(true) else Result.failure(NoSuchElementException("School not found"))
    }

    private fun getCallerId(bearerToken: String): String {
        return bearerToken.removePrefix("Bearer ").trim()
    }

    private fun findCaller(bearerToken: String): UserProfile? {
        val callerId = getCallerId(bearerToken)
        return profiles[callerId] ?: userRecords.values.firstOrNull { it.profile.id == callerId }?.profile
    }

    // ==========================================
    // AUTHENTICATION
    // ==========================================

    override suspend fun login(
        grantType: String,
        apiKey: String,
        request: SupabaseLoginRequest
    ): Response<SupabaseTokenResponse> {
        val email = request.email.trim().lowercase()
        val record = userRecords[email]
        if (record == null || record.password != request.password) {
            val errorJson = "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid login credentials\"}"
            return Response.error(400, errorJson.toResponseBody("application/json".toMediaTypeOrNull()))
        }

        val tokenResp = SupabaseTokenResponse(
            accessToken = record.profile.id, // For testing, accessToken == userId
            tokenType = "bearer",
            expiresIn = 3600,
            refreshToken = "refresh_${record.profile.id}",
            user = SupabaseAuthUser(
                id = record.profile.id,
                email = record.email,
                createdAt = record.profile.createdAt,
                updatedAt = record.profile.updatedAt
            )
        )
        return Response.success(tokenResp)
    }

    override suspend fun refreshToken(
        grantType: String,
        apiKey: String,
        request: SupabaseRefreshTokenRequest
    ): Response<SupabaseTokenResponse> {
        val token = request.refreshToken
        val userId = token.removePrefix("refresh_")
        val profile = profiles[userId]
        if (profile != null) {
            return Response.success(
                SupabaseTokenResponse(
                    accessToken = profile.id,
                    tokenType = "bearer",
                    expiresIn = 3600,
                    refreshToken = "refresh_${profile.id}",
                    user = SupabaseAuthUser(id = profile.id, email = profile.email)
                )
            )
        }
        return Response.success(
            SupabaseTokenResponse(
                accessToken = "refreshed_jwt_token",
                tokenType = "bearer",
                expiresIn = 3600,
                refreshToken = "refreshed_refresh_token"
            )
        )
    }

    override suspend fun signup(
        apiKey: String,
        request: SupabaseSignupRequest
    ): Response<SupabaseSignupResponse> {
        val email = request.email.trim().lowercase()
        if (userRecords.containsKey(email)) {
            val errorJson = "{\"error\":\"user_already_exists\",\"msg\":\"User already registered\"}"
            return Response.error(400, errorJson.toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val newId = UUID.randomUUID().toString()
        // Trigger forces role to 'student' regardless of request payload
        val profile = UserProfile(
            id = newId,
            email = email,
            role = "student",
            isActive = true
        )
        addUser(email, request.password, profile)

        return Response.success(
            SupabaseSignupResponse(
                id = newId,
                email = email,
                accessToken = newId,
                refreshToken = "refresh_$newId",
                user = SupabaseAuthUser(id = newId, email = email)
            )
        )
    }

    override suspend fun logout(apiKey: String, bearerToken: String): Response<Unit> {
        return Response.success(Unit)
    }

    // ==========================================
    // PROFILES & DIRECT PATCH
    // ==========================================

    override suspend fun getProfile(
        apiKey: String,
        bearerToken: String,
        idFilter: String,
        select: String
    ): Response<List<UserProfile>> {
        val targetId = idFilter.removePrefix("eq.").trim()
        val profile = profiles[targetId]
        return if (profile != null) {
            Response.success(listOf(profile))
        } else {
            Response.success(emptyList())
        }
    }

    override suspend fun getAllProfiles(
        apiKey: String,
        bearerToken: String,
        select: String,
        order: String
    ): Response<List<UserProfile>> {
        return Response.success(profiles.values.toList())
    }

    override suspend fun patchProfile(
        apiKey: String,
        bearerToken: String,
        idFilter: String,
        updates: Map<String, Any?>
    ): Response<List<UserProfile>> {
        val callerId = getCallerId(bearerToken)
        val caller = profiles[callerId]
            ?: return Response.error(401, "Unauthorized".toResponseBody("application/json".toMediaTypeOrNull()))

        val targetId = idFilter.removePrefix("eq.").trim()

        // RLS Policy Simulation:
        // Regular users can only patch their own profile.
        // Even on own profile: role, is_active, is_primary_admin, school_id cannot be altered via direct patch.
        if (callerId != targetId) {
            return Response.error(
                403,
                "{\"message\":\"RLS VIOLATION: Users cannot modify profiles of other users\"}".toResponseBody("application/json".toMediaTypeOrNull())
            )
        }

        if (updates.containsKey("role") || updates.containsKey("is_active") || updates.containsKey("is_primary_admin") || updates.containsKey("school_id")) {
            return Response.error(
                403,
                "{\"message\":\"forbidden: Protected columns (role, is_active, is_primary_admin, school_id) cannot be modified via direct profile patch\"}".toResponseBody("application/json".toMediaTypeOrNull())
            )
        }

        val targetProfile = profiles[targetId]
            ?: return Response.error(404, "User not found".toResponseBody("application/json".toMediaTypeOrNull()))

        var updated = targetProfile
        if (updates.containsKey("full_name")) {
            updated = updated.copy(fullName = updates["full_name"] as? String)
        }
        if (updates.containsKey("mobile")) {
            updated = updated.copy(mobile = updates["mobile"] as? String)
        }
        if (updates.containsKey("standard")) {
            updated = updated.copy(standard = updates["standard"] as? String)
        }
        if (updates.containsKey("is_active")) {
            val act = updates["is_active"] as? Boolean ?: true
            updated = updated.copy(isActive = act)
        }

        profiles[targetId] = updated
        val email = updated.email?.trim()?.lowercase()
        if (email != null && userRecords.containsKey(email)) {
            userRecords[email] = userRecords[email]!!.copy(profile = updated)
        }

        return Response.success(listOf(updated))
    }

    override suspend fun deleteProfile(
        apiKey: String,
        bearerToken: String,
        idFilter: String
    ): Response<Unit> {
        val targetId = idFilter.removePrefix("eq.")
        val p = profiles.remove(targetId)
        if (p != null) {
            val email = p.email?.trim()?.lowercase()
            if (email != null) {
                userRecords.remove(email)
            }
        }
        return Response.success(Unit)
    }

    override suspend fun updateDisplayNameRpc(
        apiKey: String,
        bearerToken: String,
        request: UpdateDisplayNameRequest
    ): Response<UserProfile> {
        val callerId = getCallerId(bearerToken)
        val caller = profiles[callerId]
            ?: return Response.error(401, "Unauthorized".toResponseBody("application/json".toMediaTypeOrNull()))

        val updated = caller.copy(fullName = request.newFullName.trim())
        profiles[callerId] = updated
        val email = updated.email?.trim()?.lowercase()
        if (email != null && userRecords.containsKey(email)) {
            userRecords[email] = userRecords[email]!!.copy(profile = updated)
        }

        return Response.success(updated)
    }

    override suspend fun searchProfiles(
        apiKey: String,
        bearerToken: String,
        roleFilter: String?,
        schoolIdFilter: String?,
        isActiveFilter: String,
        select: String,
        order: String
    ): Response<List<UserProfile>> {
        var list = profiles.values.filter { it.isActive }
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

    // ==========================================
    // ADMIN RPCS
    // ==========================================

    override suspend fun adminCreateUserRpc(
        apiKey: String,
        bearerToken: String,
        request: AdminCreateUserRequest
    ): Response<UserProfile> {
        val caller = findCaller(bearerToken)
        if (caller == null || !caller.isOfficerAdmin) {
            return Response.error(403, "Forbidden: Officer Admin required".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val email = request.email.trim().lowercase()
        if (userRecords.containsKey(email)) {
            return Response.error(400, "Email already exists".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val newId = UUID.randomUUID().toString()
        val newProfile = UserProfile(
            id = newId,
            fullName = request.fullName.trim(),
            email = email,
            role = request.role,
            isActive = request.isActive
        )
        addUser(email, request.password, newProfile)
        return Response.success(newProfile)
    }

    override suspend fun adminUpdateUserRpc(
        apiKey: String,
        bearerToken: String,
        request: AdminUpdateUserRequest
    ): Response<UserProfile> {
        val caller = findCaller(bearerToken)
        if (caller == null || !caller.isOfficerAdmin) {
            return Response.error(403, "Forbidden: Officer Admin required".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val target = profiles[request.userId]
            ?: return Response.error(404, "User not found".toResponseBody("application/json".toMediaTypeOrNull()))

        val updated = target.copy(
            fullName = request.fullName.trim(),
            role = request.role,
            isActive = request.isActive,
            mobile = request.mobile?.trim() ?: target.mobile
        )
        profiles[request.userId] = updated
        val email = updated.email?.trim()?.lowercase()
        if (email != null && userRecords.containsKey(email)) {
            userRecords[email] = userRecords[email]!!.copy(profile = updated)
        }
        return Response.success(updated)
    }

    override suspend fun adminToggleStatusRpc(
        apiKey: String,
        bearerToken: String,
        request: AdminToggleStatusRequest
    ): Response<UserProfile> {
        val caller = findCaller(bearerToken)
        if (caller == null || !caller.isOfficerAdmin) {
            return Response.error(403, "Forbidden: Officer Admin required".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val target = profiles[request.userId]
            ?: return Response.error(404, "User not found".toResponseBody("application/json".toMediaTypeOrNull()))

        val updated = target.copy(isActive = request.isActive)
        profiles[request.userId] = updated
        val email = updated.email?.trim()?.lowercase()
        if (email != null && userRecords.containsKey(email)) {
            userRecords[email] = userRecords[email]!!.copy(profile = updated)
        }
        return Response.success(updated)
    }

    // ==========================================
    // OFFICER ADMIN RPCS & SCHOOLS
    // ==========================================

    override suspend fun officerAdminCreateUserRpc(
        apiKey: String,
        bearerToken: String,
        request: OfficerAdminCreateUserRequest
    ): Response<UserProfile> {
        val caller = findCaller(bearerToken)
        if (caller == null || !caller.isOfficerAdmin) {
            return Response.error(403, "Access denied: Officer Admin required".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val email = request.email.trim().lowercase()
        if (userRecords.containsKey(email)) {
            return Response.error(400, "Email already registered".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val newId = UUID.randomUUID().toString()
        val newProfile = UserProfile(
            id = newId,
            fullName = request.fullName.trim(),
            email = email,
            mobile = request.mobile?.trim(),
            role = request.role,
            schoolId = request.schoolId,
            isActive = true,
            isPrimaryAdmin = false
        )
        addUser(email, request.password, newProfile)
        return Response.success(newProfile)
    }

    override suspend fun getSchools(
        apiKey: String,
        bearerToken: String,
        select: String,
        order: String
    ): Response<List<School>> {
        val caller = findCaller(bearerToken)
        if (caller == null) {
            return Response.success(schools.values.toList())
        }
        // RLS rules for schools table:
        // Officer Admin sees all schools
        // School Admin, Teacher, Student see only their assigned school
        return when {
            caller.isOfficerAdmin -> Response.success(schools.values.toList())
            caller.schoolId != null -> {
                val assigned = schools[caller.schoolId]
                if (assigned != null) Response.success(listOf(assigned)) else Response.success(emptyList())
            }
            else -> Response.success(emptyList())
        }
    }

    override suspend fun getSchoolById(
        apiKey: String,
        bearerToken: String,
        idFilter: String,
        select: String
    ): Response<List<School>> {
        val caller = findCaller(bearerToken)
        val id = idFilter.removePrefix("eq.").trim()
        val school = schools[id]
        if (school == null) return Response.success(emptyList())

        if (caller != null && !caller.isOfficerAdmin && caller.schoolId != id) {
            return Response.error(403, "Forbidden: RLS restricts school access".toResponseBody("application/json".toMediaTypeOrNull()))
        }

        return Response.success(listOf(school))
    }

    override suspend fun officerAdminCreateSchoolRpc(
        apiKey: String,
        bearerToken: String,
        request: CreateSchoolRequest
    ): Response<School> {
        val caller = findCaller(bearerToken)
        if (caller == null || !caller.isOfficerAdmin) {
            return Response.error(403, "Access denied: Officer Admin required".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val code = request.code.trim().uppercase()
        if (schools.values.any { it.code.equals(code, ignoreCase = true) }) {
            return Response.error(400, "School code already exists".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val newSchool = School(
            id = UUID.randomUUID().toString(),
            name = request.name.trim(),
            code = code,
            mobile = request.mobile?.trim(),
            email = request.email?.trim()?.lowercase(),
            address = request.address?.trim(),
            isActive = true
        )
        schools[newSchool.id] = newSchool
        return Response.success(newSchool)
    }

    override suspend fun officerAdminUpdateSchoolRpc(
        apiKey: String,
        bearerToken: String,
        request: UpdateSchoolRequest
    ): Response<School> {
        val caller = findCaller(bearerToken)
        if (caller == null || !caller.isOfficerAdmin) {
            return Response.error(403, "Access denied: Officer Admin required".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val existing = schools[request.schoolId]
            ?: return Response.error(404, "School not found".toResponseBody("application/json".toMediaTypeOrNull()))

        val code = request.code.trim().uppercase()
        if (schools.values.any { it.id != request.schoolId && it.code.equals(code, ignoreCase = true) }) {
            return Response.error(400, "School code already exists".toResponseBody("application/json".toMediaTypeOrNull()))
        }

        val updated = existing.copy(
            name = request.name.trim(),
            code = code,
            mobile = request.mobile?.trim(),
            email = request.email?.trim()?.lowercase(),
            address = request.address?.trim(),
            isActive = request.isActive
        )
        schools[request.schoolId] = updated
        return Response.success(updated)
    }

    // ==========================================
    // SCHOOL ADMIN RPCS (TEACHERS & STUDENTS)
    // ==========================================

    override suspend fun getTeachersBySchool(
        apiKey: String,
        bearerToken: String,
        schoolIdFilter: String,
        roleFilter: String,
        select: String,
        order: String
    ): Response<List<UserProfile>> {
        val caller = findCaller(bearerToken)
        val schoolId = schoolIdFilter.removePrefix("eq.").trim()

        if (caller != null && !caller.isOfficerAdmin && caller.schoolId != schoolId) {
            return Response.error(403, "Access denied: Cannot query teachers from another school".toResponseBody("application/json".toMediaTypeOrNull()))
        }

        val teachers = profiles.values.filter {
            it.schoolId == schoolId && it.role.equals("teacher", ignoreCase = true)
        }
        return Response.success(teachers)
    }

    override suspend fun schoolAdminCreateTeacherRpc(
        apiKey: String,
        bearerToken: String,
        request: SchoolAdminCreateTeacherRequest
    ): Response<UserProfile> {
        val caller = findCaller(bearerToken)
        if (caller == null || (!caller.isSchoolAdmin && !caller.isOfficerAdmin)) {
            return Response.error(403, "Access denied: School Admin required".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val schoolId = caller.schoolId
            ?: return Response.error(400, "School Admin has no assigned school".toResponseBody("application/json".toMediaTypeOrNull()))

        val email = request.email.trim().lowercase()
        if (userRecords.containsKey(email)) {
            return Response.error(400, "Email already registered".toResponseBody("application/json".toMediaTypeOrNull()))
        }

        val newTeacher = UserProfile(
            id = UUID.randomUUID().toString(),
            fullName = request.fullName.trim(),
            email = email,
            mobile = request.mobile?.trim(),
            role = "teacher",
            schoolId = schoolId,
            isActive = true
        )
        addUser(email, request.password, newTeacher)
        return Response.success(newTeacher)
    }

    override suspend fun schoolAdminUpdateTeacherRpc(
        apiKey: String,
        bearerToken: String,
        request: SchoolAdminUpdateTeacherRequest
    ): Response<UserProfile> {
        val caller = findCaller(bearerToken)
        if (caller == null || (!caller.isSchoolAdmin && !caller.isOfficerAdmin)) {
            return Response.error(403, "Access denied: School Admin required".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val target = profiles[request.teacherId]
            ?: return Response.error(404, "Teacher not found".toResponseBody("application/json".toMediaTypeOrNull()))

        if (caller.isSchoolAdmin && (target.schoolId != caller.schoolId || !target.role.equals("teacher", ignoreCase = true))) {
            return Response.error(403, "Cannot modify user from another school or non-teacher".toResponseBody("application/json".toMediaTypeOrNull()))
        }

        val updated = target.copy(
            fullName = request.fullName.trim(),
            mobile = request.mobile?.trim(),
            isActive = request.isActive
        )
        profiles[request.teacherId] = updated
        val email = updated.email?.trim()?.lowercase()
        if (email != null && userRecords.containsKey(email)) {
            userRecords[email] = userRecords[email]!!.copy(profile = updated)
        }
        return Response.success(updated)
    }

    override suspend fun getStudents(
        apiKey: String,
        bearerToken: String,
        roleFilter: String,
        schoolIdFilter: String?,
        select: String,
        order: String
    ): Response<List<UserProfile>> {
        var list = profiles.values.filter { it.role.equals("student", ignoreCase = true) }
        if (schoolIdFilter != null && schoolIdFilter.startsWith("eq.")) {
            val sId = schoolIdFilter.removePrefix("eq.").trim()
            list = list.filter { it.schoolId == sId }
        }
        return Response.success(list)
    }

    override suspend fun schoolAdminCreateStudentRpc(
        apiKey: String,
        bearerToken: String,
        request: SchoolAdminCreateStudentRequest
    ): Response<UserProfile> {
        val caller = findCaller(bearerToken)
        val schoolId = caller?.schoolId ?: puneSchoolId

        val email = request.email.trim().lowercase()
        if (userRecords.containsKey(email)) {
            return Response.error(400, "Email already registered".toResponseBody("application/json".toMediaTypeOrNull()))
        }

        val standardVal = if (!request.section.isNullOrBlank()) {
            "${request.standard} - ${request.section}"
        } else {
            request.standard
        }

        val newStudent = UserProfile(
            id = UUID.randomUUID().toString(),
            fullName = request.fullName.trim(),
            email = email,
            mobile = request.mobile?.trim(),
            role = "student",
            standard = standardVal,
            schoolId = schoolId,
            isActive = true
        )
        addUser(email, request.password, newStudent)
        return Response.success(newStudent)
    }

    override suspend fun schoolAdminUpdateStudentRpc(
        apiKey: String,
        bearerToken: String,
        request: SchoolAdminUpdateStudentRequest
    ): Response<UserProfile> {
        val target = profiles[request.studentId]
            ?: return Response.error(404, "Student not found".toResponseBody("application/json".toMediaTypeOrNull()))

        val standardVal = if (!request.section.isNullOrBlank()) {
            "${request.standard} - ${request.section}"
        } else {
            request.standard ?: target.standard
        }

        val updated = target.copy(
            fullName = request.fullName.trim(),
            mobile = request.mobile?.trim(),
            standard = standardVal,
            isActive = request.isActive
        )
        profiles[request.studentId] = updated
        val email = updated.email?.trim()?.lowercase()
        if (email != null && userRecords.containsKey(email)) {
            userRecords[email] = userRecords[email]!!.copy(profile = updated)
        }
        return Response.success(updated)
    }

    // ==========================================
    // GROUPS & MESSAGING
    // ==========================================

    override suspend fun getGroups(
        apiKey: String,
        bearerToken: String,
        select: String,
        isActiveFilter: String,
        order: String
    ): Response<List<Group>> {
        val callerId = getCallerId(bearerToken)
        val caller = profiles[callerId]
        if (caller == null || !caller.isActive) {
            return Response.error(401, "Unauthorized".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val activeGroupIds = groupMembers.filter { it.userId == callerId && it.isActive }.map { it.groupId }.toSet()
        val userGroups = groups.filter { it.isActive && activeGroupIds.contains(it.id) }
        return Response.success(userGroups)
    }

    override suspend fun getGroupById(
        apiKey: String,
        bearerToken: String,
        idFilter: String,
        select: String
    ): Response<List<Group>> {
        val callerId = getCallerId(bearerToken)
        val caller = profiles[callerId]
        if (caller == null || !caller.isActive) {
            return Response.error(401, "Unauthorized".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val targetId = idFilter.removePrefix("eq.").trim()
        val group = groups.firstOrNull { it.id == targetId && it.isActive }
            ?: return Response.success(emptyList())

        val isMember = groupMembers.any { it.groupId == targetId && it.userId == callerId && it.isActive }
        if (!isMember && group.createdBy != callerId && caller.role != "officer_admin") {
            return Response.error(403, "Forbidden".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        return Response.success(listOf(group))
    }

    override suspend fun getGroupMembers(
        apiKey: String,
        bearerToken: String,
        groupIdFilter: String,
        isActiveFilter: String,
        select: String
    ): Response<List<GroupMember>> {
        val callerId = getCallerId(bearerToken)
        val caller = profiles[callerId]
        if (caller == null || !caller.isActive) {
            return Response.error(401, "Unauthorized".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val targetGroupId = groupIdFilter.removePrefix("eq.").trim()
        val isMember = groupMembers.any { it.groupId == targetGroupId && it.userId == callerId && it.isActive }
        val group = groups.firstOrNull { it.id == targetGroupId }
        if (!isMember && group?.createdBy != callerId && caller.role != "officer_admin") {
            return Response.error(403, "Forbidden".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val members = groupMembers.filter { it.groupId == targetGroupId && it.isActive }
        return Response.success(members)
    }

    override suspend fun createGroupRpc(
        apiKey: String,
        bearerToken: String,
        request: CreateGroupRequest
    ): Response<Group> {
        val callerId = getCallerId(bearerToken)
        val caller = profiles[callerId]
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
                isActive = true,
                userProfile = caller
            )
        )

        return Response.success(newGroup)
    }

    override suspend fun addGroupMemberRpc(
        apiKey: String,
        bearerToken: String,
        request: AddGroupMemberRequest
    ): Response<GroupMember> {
        val callerId = getCallerId(bearerToken)
        val caller = profiles[callerId]
        if (caller == null || !caller.isActive) {
            return Response.error(401, "Unauthorized".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val group = groups.firstOrNull { it.id == request.groupId && it.isActive }
            ?: return Response.error(404, "Group not found".toResponseBody("application/json".toMediaTypeOrNull()))

        val callerMembership = groupMembers.firstOrNull { it.groupId == group.id && it.userId == caller.id && it.isActive }
        val isCallerOfficerAdmin = caller.role == "officer_admin"
        val isCallerGroupAdmin = group.createdBy == caller.id || callerMembership?.roleInGroup == "admin"

        if (!isCallerOfficerAdmin && !isCallerGroupAdmin) {
            return Response.error(403, "Access denied: Only Officer Admin or active group admin can add members".toResponseBody("application/json".toMediaTypeOrNull()))
        }

        val targetUser = profiles[request.userId]
        if (targetUser == null || !targetUser.isActive) {
            return Response.error(400, "Target user not found or deactivated".toResponseBody("application/json".toMediaTypeOrNull()))
        }

        if (group.groupType == "administrative") {
            if (targetUser.role == "student") {
                return Response.error(400, "Students cannot be added to administrative groups".toResponseBody("application/json".toMediaTypeOrNull()))
            }
        } else if (group.groupType == "teacher") {
            if (targetUser.role != "officer_admin" && targetUser.schoolId != group.schoolId) {
                return Response.error(400, "Target user belongs to a different school".toResponseBody("application/json".toMediaTypeOrNull()))
            }
        }

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

    override suspend fun removeGroupMemberRpc(
        apiKey: String,
        bearerToken: String,
        request: RemoveGroupMemberRequest
    ): Response<Boolean> {
        val callerId = getCallerId(bearerToken)
        val caller = profiles[callerId]
        if (caller == null || !caller.isActive) {
            return Response.error(401, "Unauthorized".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val group = groups.firstOrNull { it.id == request.groupId && it.isActive }
            ?: return Response.error(404, "Group not found".toResponseBody("application/json".toMediaTypeOrNull()))

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

    override suspend fun getGroupMessages(
        apiKey: String,
        bearerToken: String,
        groupIdFilter: String,
        isDeletedFilter: String,
        select: String,
        order: String
    ): Response<List<ChatMessage>> {
        val callerId = getCallerId(bearerToken)
        val caller = profiles[callerId]
        if (caller == null || !caller.isActive) {
            return Response.error(401, "Unauthorized".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val targetGroupId = groupIdFilter.removePrefix("eq.").trim()
        val isMember = groupMembers.any { it.groupId == targetGroupId && it.userId == callerId && it.isActive }
        if (!isMember && caller.role != "officer_admin") {
            return Response.error(403, "Forbidden".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val groupMsgs = messages.filter { it.groupId == targetGroupId && !it.isDeleted }
        return Response.success(groupMsgs)
    }

    override suspend fun sendGroupMessageRpc(
        apiKey: String,
        bearerToken: String,
        request: SendGroupMessageRequest
    ): Response<ChatMessage> {
        val callerId = getCallerId(bearerToken)
        val caller = profiles[callerId]
        if (caller == null || !caller.isActive) {
            return Response.error(401, "Unauthorized".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val group = groups.firstOrNull { it.id == request.groupId && it.isActive }
            ?: return Response.error(404, "Group not found".toResponseBody("application/json".toMediaTypeOrNull()))

        val isMember = groupMembers.any { it.groupId == group.id && it.userId == callerId && it.isActive }
        if (!isMember && caller.role != "officer_admin" && group.createdBy != callerId) {
            return Response.error(403, "Not a member".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val rawType = request.messageType.trim().lowercase()
        val isMedia = !request.mediaUrl.isNullOrBlank() || (rawType.isNotBlank() && rawType != "text")

        if (!isMedia && request.content.trim().isBlank()) {
            return Response.error(400, "Blank content".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        if (isMedia && request.mediaUrl.isNullOrBlank()) {
            return Response.error(400, "Blank media_url".toResponseBody("application/json".toMediaTypeOrNull()))
        }

        val finalType = if (isMedia) {
            if (rawType.isNotBlank() && rawType != "text") rawType else "image"
        } else {
            "text"
        }

        val newMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            groupId = request.groupId,
            senderId = callerId,
            content = request.content.trim(),
            messageType = finalType,
            mediaUrl = request.mediaUrl,
            createdAt = "2026-08-27T09:30:00Z",
            updatedAt = "2026-08-27T09:30:00Z",
            isDeleted = false,
            senderProfile = caller
        )
        messages.add(newMsg)
        return Response.success(newMsg)
    }
}
