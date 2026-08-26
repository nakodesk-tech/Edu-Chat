package com.example.data.repository

import android.content.Context
import com.example.data.local.SessionManager
import com.example.data.local.SimulatedDatabase
import com.example.data.model.AddGroupMemberRequest
import com.example.data.model.CreateGroupRequest
import com.example.data.model.Group
import com.example.data.model.GroupDetails
import com.example.data.model.GroupMember
import com.example.data.model.GroupType
import com.example.data.model.RemoveGroupMemberRequest
import com.example.data.model.School
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GroupRepository(
    private val context: Context,
    private val sessionManager: SessionManager = SessionManager(context)
) {

    private fun getActiveSessionProfile(): UserProfile {
        val session = sessionManager.getSession()
            ?: throw SecurityException("सत्र समाप्त झाले आहे. कृपया पुन्हा लॉगिन करा. (Session expired. Please log in again)")
        val profile = session.profile
        if (!profile.isActive) {
            throw SecurityException("आपले खाते निष्क्रिय आहे. (Your account is deactivated)")
        }
        return profile
    }

    suspend fun getGroups(): Result<List<Group>> = withContext(Dispatchers.IO) {
        try {
            val profile = getActiveSessionProfile()
            if (SupabaseConfig.isConfigured(context)) {
                val api = SupabaseClient.getApi(context)
                val token = sessionManager.getAccessToken() ?: ""
                val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
                val response = api.getGroups(
                    apiKey = anonKey,
                    bearerToken = "Bearer $token"
                )
                if (response.isSuccessful && response.body() != null) {
                    return@withContext Result.success(response.body()!!)
                }
            }
            // Offline / Simulated engine
            return@withContext SimulatedDatabase.getGroupsForUser(profile.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyGroups(): Result<List<Group>> = getGroups()

    suspend fun getGroupDetails(groupId: String): Result<GroupDetails> = withContext(Dispatchers.IO) {
        try {
            val profile = getActiveSessionProfile()
            val result = SimulatedDatabase.getGroupDetails(profile.id, groupId)
            if (result.isSuccess) {
                val pair = result.getOrThrow()
                val creator = SimulatedDatabase.findById(pair.first.createdBy)?.profile
                Result.success(GroupDetails(group = pair.first, members = pair.second, creatorProfile = creator))
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to load group details"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createGroup(name: String, groupType: GroupType): Result<Group> =
        createGroup(name, groupType.dbValue)

    suspend fun createGroup(name: String, groupType: String): Result<Group> = withContext(Dispatchers.IO) {
        try {
            val profile = getActiveSessionProfile()
            val parsedType = GroupType.fromDbValue(groupType)
                ?: return@withContext Result.failure(IllegalArgumentException("अवैध गट प्रकार (Invalid group type)"))

            // Role boundary validation
            when (parsedType) {
                GroupType.ADMINISTRATIVE -> {
                    if (profile.userRole != UserRole.OFFICER_ADMIN) {
                        return@withContext Result.failure(
                            SecurityException("केवळ Officer Admin प्रशासकीय गट तयार करू शकतात. (Only Officer Admin can create administrative groups)")
                        )
                    }
                }
                GroupType.TEACHER -> {
                    if (profile.userRole != UserRole.TEACHER) {
                        return@withContext Result.failure(
                            SecurityException("केवळ शिक्षक शिक्षक गट तयार करू शकतात. (Only Teachers can create teacher groups)")
                        )
                    }
                    if (profile.schoolId.isNullOrBlank()) {
                        return@withContext Result.failure(
                            IllegalStateException("शिक्षकाला शाळा जोडलेली नाही. (Teacher has no assigned school)")
                        )
                    }
                }
            }

            if (SupabaseConfig.isConfigured(context)) {
                val api = SupabaseClient.getApi(context)
                val token = sessionManager.getAccessToken() ?: ""
                val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
                val response = api.createGroupRpc(
                    apiKey = anonKey,
                    bearerToken = "Bearer $token",
                    request = CreateGroupRequest(name = name.trim(), groupType = parsedType.dbValue)
                )
                if (response.isSuccessful && response.body() != null) {
                    return@withContext Result.success(response.body()!!)
                }
            }

            return@withContext SimulatedDatabase.createGroup(
                callerId = profile.id,
                name = name,
                groupType = parsedType.dbValue
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMember(groupId: String, targetUserId: String): Result<GroupMember> = withContext(Dispatchers.IO) {
        try {
            val profile = getActiveSessionProfile()

            if (SupabaseConfig.isConfigured(context)) {
                val api = SupabaseClient.getApi(context)
                val token = sessionManager.getAccessToken() ?: ""
                val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
                val response = api.addGroupMemberRpc(
                    apiKey = anonKey,
                    bearerToken = "Bearer $token",
                    request = AddGroupMemberRequest(groupId = groupId, userId = targetUserId)
                )
                if (response.isSuccessful && response.body() != null) {
                    return@withContext Result.success(response.body()!!)
                }
            }

            return@withContext SimulatedDatabase.addMemberToGroup(
                callerId = profile.id,
                groupId = groupId,
                targetUserId = targetUserId
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMembersBatch(groupId: String, targetUserIds: List<String>): Result<List<GroupMember>> = withContext(Dispatchers.IO) {
        try {
            val profile = getActiveSessionProfile()
            val addedMembers = mutableListOf<GroupMember>()
            var lastError: Throwable? = null

            for (targetId in targetUserIds) {
                val res = SimulatedDatabase.addMemberToGroup(
                    callerId = profile.id,
                    groupId = groupId,
                    targetUserId = targetId
                )
                if (res.isSuccess) {
                    addedMembers.add(res.getOrThrow())
                } else {
                    lastError = res.exceptionOrNull()
                }
            }

            if (addedMembers.isEmpty() && lastError != null) {
                return@withContext Result.failure(lastError)
            }

            return@withContext Result.success(addedMembers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeMember(groupId: String, targetUserId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val profile = getActiveSessionProfile()

            if (SupabaseConfig.isConfigured(context)) {
                val api = SupabaseClient.getApi(context)
                val token = sessionManager.getAccessToken() ?: ""
                val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
                val response = api.removeGroupMemberRpc(
                    apiKey = anonKey,
                    bearerToken = "Bearer $token",
                    request = RemoveGroupMemberRequest(groupId = groupId, userId = targetUserId)
                )
                if (response.isSuccessful && response.body() != null) {
                    return@withContext Result.success(response.body()!!)
                }
            }

            return@withContext SimulatedDatabase.removeMemberFromGroup(
                callerId = profile.id,
                groupId = groupId,
                targetUserId = targetUserId
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchEligibleUsersForAdminGroup(
        query: String = "",
        roleFilter: String? = null,
        schoolFilter: String? = null
    ): Result<List<UserProfile>> = withContext(Dispatchers.IO) {
        try {
            val profile = getActiveSessionProfile()
            if (profile.userRole != UserRole.OFFICER_ADMIN) {
                return@withContext Result.failure(
                    SecurityException("केवळ Officer Admin वापरकर्ते शोधू शकतात.")
                )
            }
            return@withContext SimulatedDatabase.searchEligibleUsersForAdminGroup(
                callerId = profile.id,
                query = query,
                roleFilter = roleFilter,
                schoolFilter = schoolFilter
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchEligibleStudentsForTeacherGroup(
        query: String = ""
    ): Result<List<UserProfile>> = withContext(Dispatchers.IO) {
        try {
            val profile = getActiveSessionProfile()
            if (profile.userRole != UserRole.TEACHER) {
                return@withContext Result.failure(
                    SecurityException("केवळ शिक्षक विद्यार्थी शोधू शकतात.")
                )
            }
            return@withContext SimulatedDatabase.searchEligibleStudentsForTeacherGroup(
                callerId = profile.id,
                query = query
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveSchools(): Result<List<School>> = withContext(Dispatchers.IO) {
        try {
            val schools = SimulatedDatabase.getAllSchools().filter { it.isActive }
            Result.success(schools)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
