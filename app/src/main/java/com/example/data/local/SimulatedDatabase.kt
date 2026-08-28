package com.example.data.local

import com.example.data.model.Group
import com.example.data.model.GroupMember
import com.example.data.model.GroupType
import com.example.data.model.School
import com.example.data.model.UserProfile
import java.util.UUID

data class SimulatedDbUser(
    val email: String,
    var password: String,
    var profile: UserProfile
)

object SimulatedDatabase {
    private val users = mutableListOf<SimulatedDbUser>()
    private val schools = mutableListOf<School>()
    private val groups = mutableListOf<Group>()
    private val groupMembers = mutableListOf<GroupMember>()

    init {
        reset()
    }

    @Synchronized
    fun reset() {
        users.clear()
        schools.clear()
        groups.clear()
        groupMembers.clear()

        schools.addAll(
            listOf(
                School(
                    id = "s0000000-0001-4000-8000-000000000001",
                    name = "जिल्हा परिषद प्राथमिक शाळा, पुणे (ZP Primary School)",
                    code = "SCH-PUN-001",
                    mobile = "9822012345",
                    email = "zp.pune.01@educhat.edu",
                    address = "शिवाजीनगर, पुणे, महाराष्ट्र ४११०१६",
                    isActive = true,
                    createdAt = "2026-01-01T08:00:00Z",
                    updatedAt = "2026-01-01T08:00:00Z"
                ),
                School(
                    id = "s0000000-0002-4000-8000-000000000002",
                    name = "शासकीय माध्यमिक विद्यालय, ठाणे (Govt High School)",
                    code = "SCH-THA-002",
                    mobile = "9822067890",
                    email = "govt.thane.02@educhat.edu",
                    address = "नौपाडा, ठाणे (पश्चिम), महाराष्ट्र ४००६०२",
                    isActive = true,
                    createdAt = "2026-01-05T09:00:00Z",
                    updatedAt = "2026-01-05T09:00:00Z"
                ),
                School(
                    id = "s0000000-0003-4000-8000-000000000003",
                    name = "महात्मा फुले विद्यालय, नागपूर (Phule Vidya Mandir)",
                    code = "SCH-NAG-003",
                    mobile = "9822099887",
                    email = "phule.nagpur.03@educhat.edu",
                    address = "धरमपेठ, नागपूर, महाराष्ट्र ४४००१०",
                    isActive = false,
                    createdAt = "2026-01-10T10:00:00Z",
                    updatedAt = "2026-01-10T10:00:00Z"
                )
            )
        )

        users.addAll(
            listOf(
                SimulatedDbUser(
                    email = "teacher@educhat.edu",
                    password = "password123",
                    profile = UserProfile(
                        id = "d0a1b2c3-0001-4000-8000-000000000001",
                        fullName = "Prof. Sarah Jenkins",
                        email = "teacher@educhat.edu",
                        mobile = "9822012345",
                        role = "teacher",
                        isActive = true,
                        schoolId = "s0000000-0001-4000-8000-000000000001",
                        createdAt = "2026-01-15T08:00:00Z",
                        updatedAt = "2026-01-15T08:00:00Z"
                    )
                ),
                SimulatedDbUser(
                    email = "student@educhat.edu",
                    password = "password123",
                    profile = UserProfile(
                        id = "d0a1b2c3-0002-4000-8000-000000000002",
                        fullName = "Alex Rivera",
                        email = "student@educhat.edu",
                        mobile = "9822054321",
                        role = "student",
                        isActive = true,
                        schoolId = "s0000000-0001-4000-8000-000000000001",
                        createdAt = "2026-02-01T09:30:00Z",
                        updatedAt = "2026-02-01T09:30:00Z"
                    )
                ),
                SimulatedDbUser(
                    email = "admin@educhat.edu",
                    password = "password123",
                    profile = UserProfile(
                        id = "d0a1b2c3-0003-4000-8000-000000000003",
                        fullName = "Primary Officer Admin",
                        email = "admin@educhat.edu",
                        mobile = "9800112233",
                        role = "officer_admin",
                        isActive = true,
                        isPrimaryAdmin = true,
                        schoolId = null,
                        createdAt = "2025-12-01T12:00:00Z",
                        updatedAt = "2025-12-01T12:00:00Z"
                    )
                ),
                SimulatedDbUser(
                    email = "inactive@educhat.edu",
                    password = "password123",
                    profile = UserProfile(
                        id = "d0a1b2c3-0004-4000-8000-000000000004",
                        fullName = "John Suspended",
                        email = "inactive@educhat.edu",
                        mobile = "9899001122",
                        role = "student",
                        isActive = false,
                        schoolId = "s0000000-0002-4000-8000-000000000002",
                        createdAt = "2026-01-10T10:00:00Z",
                        updatedAt = "2026-01-10T10:00:00Z"
                    )
                )
            )
        )

        // Initial Administrative Group for Primary Officer Admin
        val adminGroupId = "g0000000-0001-4000-8000-000000000001"
        val primaryAdminId = "d0a1b2c3-0003-4000-8000-000000000003"
        groups.add(
            Group(
                id = adminGroupId,
                name = "प्रशासकीय गट (Administrative Group)",
                groupType = "administrative",
                createdBy = primaryAdminId,
                schoolId = null,
                isActive = true,
                createdAt = "2026-02-01T10:00:00Z",
                updatedAt = "2026-02-01T10:00:00Z"
            )
        )

        groupMembers.add(
            GroupMember(
                id = "m0000000-0001-4000-8000-000000000001",
                groupId = adminGroupId,
                userId = primaryAdminId,
                roleInGroup = "owner",
                isActive = true,
                joinedAt = "2026-02-01T10:00:00Z",
                createdAt = "2026-02-01T10:00:00Z"
            )
        )
    }

    @Synchronized
    fun findByEmail(email: String): SimulatedDbUser? {
        return users.firstOrNull { it.email.equals(email, ignoreCase = true) }
    }

    @Synchronized
    fun findById(id: String): SimulatedDbUser? {
        return users.firstOrNull { it.profile.id == id }
    }

    @Synchronized
    fun getAllProfiles(): List<UserProfile> {
        return users.map { it.profile }
    }

    @Synchronized
    fun addUser(user: SimulatedDbUser) {
        users.add(user)
    }

    @Synchronized
    fun updateProfile(id: String, transform: (UserProfile) -> UserProfile): UserProfile? {
        val index = users.indexOfFirst { it.profile.id == id }
        if (index != -1) {
            val current = users[index]
            val updated = transform(current.profile)
            users[index] = current.copy(profile = updated)
            return updated
        }
        return null
    }

    @Synchronized
    fun getAllSchools(): List<School> {
        return schools.toList()
    }

    @Synchronized
    fun findSchoolById(id: String): School? {
        return schools.firstOrNull { it.id == id }
    }

    @Synchronized
    fun findSchoolByCode(code: String): School? {
        val trimmed = code.trim()
        return schools.firstOrNull { it.code.equals(trimmed, ignoreCase = true) }
    }

    @Synchronized
    fun addSchool(school: School) {
        val targetCode = school.code.trim()
        val duplicate = schools.firstOrNull {
            it.code.trim().equals(targetCode, ignoreCase = true)
        }
        if (duplicate != null) {
            throw IllegalArgumentException("School code '$targetCode' is already registered. Duplicate codes are rejected by database unique constraint.")
        }
        schools.add(0, school)
    }

    @Synchronized
    fun updateSchool(id: String, transform: (School) -> School): School? {
        val index = schools.indexOfFirst { it.id == id }
        if (index != -1) {
            val current = schools[index]
            val updated = transform(current)
            val targetCode = updated.code.trim()
            val duplicate = schools.firstOrNull {
                it.id != id && it.code.trim().equals(targetCode, ignoreCase = true)
            }
            if (duplicate != null) {
                throw IllegalArgumentException("School code '$targetCode' is already registered. Duplicate codes are rejected by database unique constraint.")
            }
            schools[index] = updated
            return updated
        }
        return null
    }

    @Synchronized
    fun deleteSchool(id: String): Result<Boolean> {
        // Enforce ON DELETE RESTRICT / NO ACTION constraint:
        val attachedUsers = users.filter { it.profile.schoolId == id }
        if (attachedUsers.isNotEmpty()) {
            return Result.failure(
                IllegalStateException("Cannot delete school: ${attachedUsers.size} user(s) (School Admins, Teachers, Students) are currently associated with this school. Foreign key constraint violated (ON DELETE RESTRICT).")
            )
        }
        val removed = schools.removeIf { it.id == id }
        return if (removed) Result.success(true) else Result.failure(NoSuchElementException("School not found."))
    }

    @Synchronized
    fun getUsersBySchoolId(schoolId: String): List<UserProfile> {
        return users.filter { it.profile.schoolId == schoolId && it.profile.isActive }.map { it.profile }
    }

    @Synchronized
    fun getTeachersBySchool(schoolId: String): List<UserProfile> {
        return users
            .filter { it.profile.schoolId == schoolId && it.profile.role.equals("teacher", ignoreCase = true) }
            .map { it.profile }
    }

    @Synchronized
    fun schoolAdminCreateTeacher(
        callerSchoolId: String,
        fullName: String,
        email: String,
        mobile: String?,
        password: String
    ): Result<UserProfile> {
        val existing = findByEmail(email)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("हा Email आधीपासून नोंदणीकृत आहे. (User with this email already exists)"))
        }

        val targetSchool = findSchoolById(callerSchoolId)
        if (targetSchool == null || !targetSchool.isActive) {
            return Result.failure(IllegalStateException("Assigned school is invalid or inactive."))
        }

        val newId = UUID.randomUUID().toString()
        val profile = UserProfile(
            id = newId,
            fullName = fullName.trim(),
            email = email.trim().lowercase(),
            mobile = mobile?.trim()?.ifBlank { null },
            role = "teacher",
            isActive = true,
            isPrimaryAdmin = false,
            schoolId = callerSchoolId,
            createdAt = "2026-02-26T10:00:00Z",
            updatedAt = "2026-02-26T10:00:00Z"
        )

        users.add(0, SimulatedDbUser(email = profile.email!!, password = password, profile = profile))
        return Result.success(profile)
    }

    @Synchronized
    fun schoolAdminUpdateTeacher(
        callerSchoolId: String,
        teacherId: String,
        fullName: String,
        mobile: String?,
        isActive: Boolean
    ): Result<UserProfile> {
        val index = users.indexOfFirst { it.profile.id == teacherId }
        if (index == -1) {
            return Result.failure(NoSuchElementException("शिक्षक आढळले नाहीत. (Teacher not found)"))
        }

        val existing = users[index]
        // Strictly verify target teacher belongs to caller's school_id
        if (existing.profile.schoolId != callerSchoolId) {
            return Result.failure(
                SecurityException("आपल्याला या शिक्षकाची माहिती बदलण्याची परवानगी नाही. (Unauthorized school access)")
            )
        }

        // Verify target is a teacher
        if (!existing.profile.role.equals("teacher", ignoreCase = true)) {
            return Result.failure(SecurityException("Only teachers can be managed through this operation."))
        }

        val updatedProfile = existing.profile.copy(
            fullName = fullName.trim(),
            mobile = mobile?.trim()?.ifBlank { null },
            isActive = isActive,
            updatedAt = "2026-02-26T10:00:00Z"
        )

        users[index] = existing.copy(profile = updatedProfile)
        return Result.success(updatedProfile)
    }

    @Synchronized
    fun schoolAdminToggleTeacherStatus(
        callerSchoolId: String,
        teacherId: String,
        isActive: Boolean
    ): Result<UserProfile> {
        val index = users.indexOfFirst { it.profile.id == teacherId }
        if (index == -1) {
            return Result.failure(NoSuchElementException("शिक्षक आढळले नाहीत. (Teacher not found)"))
        }

        val existing = users[index]
        if (existing.profile.schoolId != callerSchoolId) {
            return Result.failure(
                SecurityException("आपल्याला या शिक्षकाची माहिती बदलण्याची परवानगी नाही. (Unauthorized school access)")
            )
        }

        if (!existing.profile.role.equals("teacher", ignoreCase = true)) {
            return Result.failure(SecurityException("Only teachers can be managed through this operation."))
        }

        val updatedProfile = existing.profile.copy(
            isActive = isActive,
            updatedAt = "2026-02-26T10:00:00Z"
        )

        users[index] = existing.copy(profile = updatedProfile)
        return Result.success(updatedProfile)
    }

    // ==========================================
    // Feature 4: Chats & Group Foundation Engine
    // ==========================================

    @Synchronized
    fun createGroup(
        callerId: String,
        name: String,
        groupType: String
    ): Result<Group> {
        val caller = findById(callerId)?.profile
            ?: return Result.failure(SecurityException("वापरकर्ता आढळला नाही. (User not found)"))

        if (!caller.isActive) {
            return Result.failure(SecurityException("आपले खाते निष्क्रिय आहे. (Account is inactive)"))
        }

        val parsedType = GroupType.fromDbValue(groupType)
            ?: return Result.failure(IllegalArgumentException("अवैध गट प्रकार. (Invalid group type: $groupType)"))

        val groupSchoolId: String?

        when (parsedType) {
            GroupType.ADMINISTRATIVE -> {
                if (!caller.isOfficerAdmin) {
                    return Result.failure(
                        SecurityException("केवळ Officer Admin प्रशासकीय गट तयार करू शकतात. (Only Officer Admin can create administrative groups)")
                    )
                }
                // Administrative groups are system-wide
                groupSchoolId = null
            }
            GroupType.TEACHER -> {
                if (!caller.role.equals("teacher", ignoreCase = true)) {
                    return Result.failure(
                        SecurityException("केवळ शिक्षक शिक्षक गट तयार करू शकतात. (Only Teachers can create teacher groups)")
                    )
                }
                if (caller.schoolId.isNullOrBlank()) {
                    return Result.failure(
                        IllegalStateException("शिक्षकाला शाळा जोडलेली नाही. (Teacher must be associated with an active school)")
                    )
                }
                // Automatically inherit authenticated teacher's school scope
                groupSchoolId = caller.schoolId
            }
        }

        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("गटाचे नाव आवश्यक आहे. (Group name cannot be blank)"))
        }

        val newGroupId = UUID.randomUUID().toString()
        val now = "2026-02-26T12:00:00Z"
        val group = Group(
            id = newGroupId,
            name = trimmedName,
            groupType = parsedType.dbValue,
            createdBy = caller.id,
            schoolId = groupSchoolId,
            isActive = true,
            createdAt = now,
            updatedAt = now,
            memberCount = 1,
            creatorName = caller.fullName,
            schoolName = groupSchoolId?.let { findSchoolById(it)?.name }
        )

        groups.add(0, group)

        // Automatically add group creator as owner/member
        val newMember = GroupMember(
            id = UUID.randomUUID().toString(),
            groupId = newGroupId,
            userId = caller.id,
            roleInGroup = "owner",
            isActive = true,
            joinedAt = now,
            createdAt = now,
            userProfile = caller
        )
        groupMembers.add(0, newMember)

        return Result.success(group)
    }

    @Synchronized
    fun getGroupsForUser(userId: String): Result<List<Group>> {
        val caller = findById(userId)?.profile
            ?: return Result.failure(SecurityException("वापरकर्ता आढळला नाही. (User not found)"))

        if (!caller.isActive) {
            return Result.failure(SecurityException("आपले खाते निष्क्रिय आहे. (Account is inactive)"))
        }

        // Active memberships of caller
        val activeGroupIds = groupMembers
            .filter { it.userId == userId && it.isActive }
            .map { it.groupId }
            .toSet()

        val resultGroups = groups
            .filter { activeGroupIds.contains(it.id) && it.isActive }
            .map { g ->
                val count = groupMembers.count { it.groupId == g.id && it.isActive }
                val creator = findById(g.createdBy)?.profile
                val school = g.schoolId?.let { findSchoolById(it) }
                g.copy(
                    memberCount = count,
                    creatorName = creator?.fullName ?: "अज्ञात",
                    schoolName = school?.name
                )
            }

        return Result.success(resultGroups)
    }

    @Synchronized
    fun getGroupDetails(callerId: String, groupId: String): Result<Pair<Group, List<GroupMember>>> {
        val caller = findById(callerId)?.profile
            ?: return Result.failure(SecurityException("वापरकर्ता आढळला नाही. (User not found)"))

        if (!caller.isActive) {
            return Result.failure(SecurityException("आपले खाते निष्क्रिय आहे. (Account is inactive)"))
        }

        val group = groups.firstOrNull { it.id == groupId && it.isActive }
            ?: return Result.failure(NoSuchElementException("गट आढळला नाही. (Group not found)"))

        val callerMembership = groupMembers.firstOrNull { it.groupId == groupId && it.userId == callerId && it.isActive }
        if (callerMembership == null) {
            return Result.failure(
                SecurityException("आपण या गटाचे सक्रिय सदस्य नाही. (Access denied: Not an active member)")
            )
        }

        val members = groupMembers
            .filter { it.groupId == groupId && it.isActive }
            .map { m ->
                val profile = findById(m.userId)?.profile
                m.copy(userProfile = profile)
            }

        val creator = findById(group.createdBy)?.profile
        val school = group.schoolId?.let { findSchoolById(it) }
        val enrichedGroup = group.copy(
            memberCount = members.size,
            creatorName = creator?.fullName ?: "अज्ञात",
            schoolName = school?.name
        )

        return Result.success(Pair(enrichedGroup, members))
    }

    @Synchronized
    fun addMemberToGroup(
        callerId: String,
        groupId: String,
        targetUserId: String
    ): Result<GroupMember> {
        val caller = findById(callerId)?.profile
            ?: return Result.failure(SecurityException("वापरकर्ता आढळला नाही. (User not found)"))

        if (!caller.isActive) {
            return Result.failure(SecurityException("आपले खाते निष्क्रिय आहे. (Account is inactive)"))
        }

        val group = groups.firstOrNull { it.id == groupId && it.isActive }
            ?: return Result.failure(NoSuchElementException("गट आढळला नाही. (Group not found)"))

        // Authorization checks
        val callerMembership = groupMembers.firstOrNull { it.groupId == groupId && it.userId == callerId && it.isActive }
        val isCallerOfficerAdmin = caller.isOfficerAdmin
        val isCallerGroupAdmin = group.createdBy == caller.id || callerMembership?.roleInGroup == "admin" || callerMembership?.roleInGroup == "owner"

        if (!isCallerOfficerAdmin && !isCallerGroupAdmin) {
            return Result.failure(
                SecurityException("केवळ Officer Admin किंवा या गटाचे Admin सदस्य जोडू शकतात. (Unauthorized group manager)")
            )
        }

        val targetUser = findById(targetUserId)?.profile
            ?: return Result.failure(NoSuchElementException("लक्ष्य वापरकर्ता आढळला नाही. (Target user not found)"))

        if (!targetUser.isActive) {
            return Result.failure(IllegalStateException("हा वापरकर्ता निष्क्रिय आहे. (Target user is inactive)"))
        }

        // Eligibility validation
        if (group.groupType.equals("administrative", ignoreCase = true)) {
            // Allowed: officer_admin, school_admin, teacher. Student is FORBIDDEN.
            if (targetUser.role.equals("student", ignoreCase = true)) {
                return Result.failure(
                    SecurityException("प्रशासकीय गटात विद्यार्थ्यांना जोडता येत नाही. (Students cannot be added to administrative groups)")
                )
            }
        } else if (group.groupType.equals("teacher", ignoreCase = true)) {
            // Teacher group:
            // Officer Admin: allowed even when target school_id IS NULL.
            // School Admin: allowed only when target school_id = group.school_id.
            // Teacher: allowed only when target school_id = group.school_id.
            // Student: allowed only when target school_id = group.school_id.
            if (targetUser.isOfficerAdmin) {
                // Officer Admin is allowed across all teacher groups regardless of null schoolId
            } else if (targetUser.schoolId != group.schoolId) {
                return Result.failure(
                    SecurityException("शिक्षक फक्त स्वतःच्या शाळेतील सदस्यांना जोडू शकतात. (Target user belongs to a different school)")
                )
            }
        }

        // Check duplicate active membership
        val existingIndex = groupMembers.indexOfFirst { it.groupId == groupId && it.userId == targetUserId }
        val now = "2026-02-26T12:00:00Z"

        if (existingIndex != -1) {
            val existing = groupMembers[existingIndex]
            if (existing.isActive) {
                return Result.failure(
                    IllegalArgumentException("हा वापरकर्ता आधीपासून या गटाचा सदस्य आहे. (User is already an active member)")
                )
            } else {
                // Reactivate soft-removed member
                val reactivated = existing.copy(
                    isActive = true,
                    joinedAt = now,
                    userProfile = targetUser
                )
                groupMembers[existingIndex] = reactivated
                return Result.success(reactivated)
            }
        }

        val newMember = GroupMember(
            id = UUID.randomUUID().toString(),
            groupId = groupId,
            userId = targetUserId,
            roleInGroup = "member",
            isActive = true,
            joinedAt = now,
            createdAt = now,
            userProfile = targetUser
        )
        groupMembers.add(newMember)
        return Result.success(newMember)
    }

    @Synchronized
    fun removeMemberFromGroup(
        callerId: String,
        groupId: String,
        targetUserId: String
    ): Result<Boolean> {
        val caller = findById(callerId)?.profile
            ?: return Result.failure(SecurityException("वापरकर्ता आढळला नाही. (User not found)"))

        if (!caller.isActive) {
            return Result.failure(SecurityException("आपले खाते निष्क्रिय आहे. (Account is inactive)"))
        }

        val group = groups.firstOrNull { it.id == groupId && it.isActive }
            ?: return Result.failure(NoSuchElementException("गट आढळला नाही. (Group not found)"))

        val isSelfLeave = callerId == targetUserId
        if (!isSelfLeave) {
            if (group.groupType.equals("administrative", ignoreCase = true)) {
                if (!caller.isOfficerAdmin) {
                    return Result.failure(
                        SecurityException("केवळ Officer Admin सदस्य काढू शकतात. (Unauthorized group manager)")
                    )
                }
            } else if (group.groupType.equals("teacher", ignoreCase = true)) {
                if (!caller.role.equals("teacher", ignoreCase = true) || group.createdBy != caller.id) {
                    return Result.failure(
                        SecurityException("केवळ या गटाचे शिक्षक सदस्य काढू शकतात. (Unauthorized group manager)")
                    )
                }
            }
        }

        val index = groupMembers.indexOfFirst { it.groupId == groupId && it.userId == targetUserId && it.isActive }
        if (index == -1) {
            return Result.failure(NoSuchElementException("हा वापरकर्ता या गटाचा सक्रिय सदस्य नाही. (Member not found)"))
        }

        // Soft deactivation (is_active = false)
        val existing = groupMembers[index]
        groupMembers[index] = existing.copy(isActive = false)
        return Result.success(true)
    }

    @Synchronized
    fun searchEligibleUsersForAdminGroup(
        callerId: String,
        query: String = "",
        roleFilter: String? = null,
        schoolFilter: String? = null
    ): Result<List<UserProfile>> {
        val caller = findById(callerId)?.profile
            ?: return Result.failure(SecurityException("वापरकर्ता आढळला नाही. (User not found)"))

        if (!caller.isActive || !caller.isOfficerAdmin) {
            return Result.failure(SecurityException("केवळ Officer Admin वापरकर्ते शोधू शकतात. (Unauthorized)"))
        }

        val trimmedQ = query.trim().lowercase()

        val eligible = users
            .asSequence()
            .map { it.profile }
            .filter { it.isActive }
            .filter { it.role != "student" } // Students strictly excluded
            .filter {
                if (roleFilter.isNullOrBlank()) true else it.role.equals(roleFilter, ignoreCase = true)
            }
            .filter {
                if (schoolFilter.isNullOrBlank()) true else it.schoolId == schoolFilter
            }
            .filter {
                if (trimmedQ.isBlank()) true else {
                    (it.fullName?.lowercase()?.contains(trimmedQ) == true) ||
                    (it.email?.lowercase()?.contains(trimmedQ) == true) ||
                    (it.mobile?.contains(trimmedQ) == true)
                }
            }
            .toList()

        return Result.success(eligible)
    }

    @Synchronized
    fun searchEligibleStudentsForTeacherGroup(
        callerId: String,
        query: String = ""
    ): Result<List<UserProfile>> {
        val caller = findById(callerId)?.profile
            ?: return Result.failure(SecurityException("वापरकर्ता आढळला नाही. (User not found)"))

        if (!caller.isActive || !caller.role.equals("teacher", ignoreCase = true)) {
            return Result.failure(SecurityException("केवळ शिक्षक विद्यार्थी शोधू शकतात. (Unauthorized)"))
        }

        val teacherSchoolId = caller.schoolId
            ?: return Result.failure(IllegalStateException("शिक्षकाला शाळा जोडलेली नाही. (No school attached)"))

        val trimmedQ = query.trim().lowercase()

        val eligible = users
            .asSequence()
            .map { it.profile }
            .filter { it.isActive }
            .filter { it.role.equals("student", ignoreCase = true) }
            .filter { it.schoolId == teacherSchoolId }
            .filter {
                if (trimmedQ.isBlank()) true else {
                    (it.fullName?.lowercase()?.contains(trimmedQ) == true) ||
                    (it.email?.lowercase()?.contains(trimmedQ) == true) ||
                    (it.mobile?.contains(trimmedQ) == true)
                }
            }
            .toList()

        return Result.success(eligible)
    }

    @Synchronized
    fun deactivateGroup(groupId: String): Boolean {
        val index = groups.indexOfFirst { it.id == groupId }
        if (index >= 0) {
            groups[index] = groups[index].copy(isActive = false)
            return true
        }
        return false
    }
}


