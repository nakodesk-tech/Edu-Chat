package com.example.data.repository

import android.content.Context
import com.example.data.local.SessionManager
import com.example.data.model.AddGroupMemberRequest
import com.example.data.model.ChatMessage
import com.example.data.model.CreateGroupRequest
import com.example.data.model.Group
import com.example.data.model.GroupDetails
import com.example.data.model.GroupMember
import com.example.data.model.GroupType
import com.example.data.model.RemoveGroupMemberRequest
import com.example.data.model.School
import com.example.data.model.SendGroupMessageRequest
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.remote.SupabaseAuthApi
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Authoritative Supabase Group Repository.
 * All operations execute strictly against Supabase PostgreSQL/PostgREST/RPC backend.
 * All production fallback paths to local/simulated storage have been removed.
 * Network/Authorization failures FAIL CLOSED.
 */
class GroupRepository(
    private val context: Context,
    private val sessionManager: SessionManager = SessionManager(context),
    private val apiOverride: SupabaseAuthApi? = null
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

    private fun getApiAndToken(): Pair<SupabaseAuthApi, String> {
        val token = sessionManager.getAccessToken()
        if (token.isNullOrBlank()) {
            throw SecurityException("सत्र समाप्त झाले आहे. कृपया पुन्हा लॉगिन करा. (Session expired. Please log in again)")
        }
        val api = apiOverride ?: if (SupabaseConfig.isConfigured(context)) {
            SupabaseClient.getApi(context)
        } else {
            throw IllegalStateException("सर्व्हरशी संपर्क होऊ शकला नाही. कृपया पुन्हा प्रयत्न करा.")
        }
        return Pair(api, token)
    }

    suspend fun getGroups(): Result<List<Group>> = withContext(Dispatchers.IO) {
        try {
            val profile = getActiveSessionProfile()
            val (api, token) = getApiAndToken()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.getGroups(
                apiKey = anonKey,
                bearerToken = "Bearer $token"
            )
            if (response.isSuccessful) {
                return@withContext Result.success(response.body() ?: emptyList())
            } else if (response.code() == 401 || response.code() == 403) {
                return@withContext Result.failure(SecurityException("आपल्याला या गटासाठी परवानगी नाही."))
            } else {
                return@withContext Result.failure(Exception("सर्व्हरशी संपर्क होऊ शकला नाही. कृपया पुन्हा प्रयत्न करा."))
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("सर्व्हरशी संपर्क होऊ शकला नाही. कृपया पुन्हा प्रयत्न करा.", e))
        }
    }

    suspend fun getMyGroups(): Result<List<Group>> = getGroups()

    suspend fun getGroupDetails(groupId: String): Result<GroupDetails> = withContext(Dispatchers.IO) {
        try {
            val profile = getActiveSessionProfile()
            val (api, token) = getApiAndToken()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)

            val groupRes = api.getGroupById(
                apiKey = anonKey,
                bearerToken = "Bearer $token",
                idFilter = "eq.$groupId"
            )
            if (!groupRes.isSuccessful) {
                if (groupRes.code() == 401 || groupRes.code() == 403) {
                    return@withContext Result.failure(SecurityException("आपल्याला या गटासाठी परवानगी नाही."))
                }
                return@withContext Result.failure(Exception("सर्व्हरशी संपर्क होऊ शकला नाही. कृपया पुन्हा प्रयत्न करा."))
            }
            val groupList = groupRes.body()
            if (groupList.isNullOrEmpty()) {
                return@withContext Result.failure(SecurityException("आपल्याला या गटासाठी परवानगी नाही."))
            }
            val group = groupList.first()

            val membersRes = api.getGroupMembers(
                apiKey = anonKey,
                bearerToken = "Bearer $token",
                groupIdFilter = "eq.$groupId"
            )
            val members = if (membersRes.isSuccessful) membersRes.body() ?: emptyList() else emptyList()

            // Verify membership if not returned by RLS
            val isMember = members.any { it.userId == profile.id && it.isActive }
            if (!isMember && group.createdBy != profile.id) {
                return@withContext Result.failure(SecurityException("आपल्याला या गटासाठी परवानगी नाही."))
            }

            Result.success(GroupDetails(group = group, members = members))
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("सर्व्हरशी संपर्क होऊ शकला नाही. कृपया पुन्हा प्रयत्न करा.", e))
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
                            SecurityException("आपल्याला या गटासाठी परवानगी नाही.")
                        )
                    }
                }
                GroupType.TEACHER -> {
                    if (profile.userRole != UserRole.TEACHER) {
                        return@withContext Result.failure(
                            SecurityException("आपल्याला या गटासाठी परवानगी नाही.")
                        )
                    }
                    if (profile.schoolId.isNullOrBlank()) {
                        return@withContext Result.failure(
                            IllegalStateException("शिक्षकाला शाळा जोडलेली नाही. (Teacher has no assigned school)")
                        )
                    }
                }
            }

            val (api, token) = getApiAndToken()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.createGroupRpc(
                apiKey = anonKey,
                bearerToken = "Bearer $token",
                request = CreateGroupRequest(name = name.trim(), groupType = parsedType.dbValue)
            )

            if (response.isSuccessful && response.body() != null) {
                return@withContext Result.success(response.body()!!)
            } else if (response.code() == 401 || response.code() == 403) {
                return@withContext Result.failure(SecurityException("आपल्याला या गटासाठी परवानगी नाही."))
            } else {
                return@withContext Result.failure(Exception("गट तयार करता आला नाही. कृपया पुन्हा प्रयत्न करा."))
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("गट तयार करता आला नाही. कृपया पुन्हा प्रयत्न करा.", e))
        }
    }

    suspend fun addMember(groupId: String, targetUserId: String): Result<GroupMember> = withContext(Dispatchers.IO) {
        try {
            getActiveSessionProfile()
            val (api, token) = getApiAndToken()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.addGroupMemberRpc(
                apiKey = anonKey,
                bearerToken = "Bearer $token",
                request = AddGroupMemberRequest(groupId = groupId, userId = targetUserId)
            )

            if (response.isSuccessful && response.body() != null) {
                return@withContext Result.success(response.body()!!)
            } else if (response.code() == 401 || response.code() == 403) {
                return@withContext Result.failure(SecurityException("आपल्याला या गटासाठी परवानगी नाही."))
            } else {
                val errorMsg = SupabaseClient.parseError(response.errorBody()?.string())
                    ?: "सदस्य जोडण्यात त्रुटी आली. कृपया पुन्हा प्रयत्न करा."
                return@withContext Result.failure(Exception(errorMsg))
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("सर्व्हरशी संपर्क होऊ शकला नाही. कृपया पुन्हा प्रयत्न करा.", e))
        }
    }

    suspend fun addMembersBatch(groupId: String, targetUserIds: List<String>): Result<List<GroupMember>> = withContext(Dispatchers.IO) {
        try {
            val addedMembers = mutableListOf<GroupMember>()
            var lastError: Throwable? = null

            for (targetId in targetUserIds) {
                val res = addMember(groupId = groupId, targetUserId = targetId)
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
            getActiveSessionProfile()
            val (api, token) = getApiAndToken()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.removeGroupMemberRpc(
                apiKey = anonKey,
                bearerToken = "Bearer $token",
                request = RemoveGroupMemberRequest(groupId = groupId, userId = targetUserId)
            )

            if (response.isSuccessful) {
                return@withContext Result.success(response.body() ?: true)
            } else if (response.code() == 401 || response.code() == 403) {
                return@withContext Result.failure(SecurityException("आपल्याला या गटासाठी परवानगी नाही."))
            } else {
                return@withContext Result.failure(Exception("सर्व्हरशी संपर्क होऊ शकला नाही. कृपया पुन्हा प्रयत्न करा."))
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("सर्व्हरशी संपर्क होऊ शकला नाही. कृपया पुन्हा प्रयत्न करा.", e))
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
                    SecurityException("आपल्याला या गटासाठी परवानगी नाही.")
                )
            }

            val roleQuery = if (!roleFilter.isNullOrBlank()) {
                if (roleFilter.equals("student", ignoreCase = true)) {
                    // Students are NEVER eligible for administrative groups
                    return@withContext Result.success(emptyList())
                }
                "eq.$roleFilter"
            } else {
                "in.(officer_admin,school_admin,teacher)"
            }

            val schoolQuery = if (!schoolFilter.isNullOrBlank()) "eq.$schoolFilter" else null

            val (api, token) = getApiAndToken()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.searchProfiles(
                apiKey = anonKey,
                bearerToken = "Bearer $token",
                roleFilter = roleQuery,
                schoolIdFilter = schoolQuery,
                isActiveFilter = "eq.true"
            )

            if (response.isSuccessful && response.body() != null) {
                val users = response.body()!!.filter { user ->
                    !user.role.equals("student", ignoreCase = true) &&
                    user.isActive &&
                    (query.isBlank() ||
                     (user.fullName?.contains(query, ignoreCase = true) == true) ||
                     (user.email?.contains(query, ignoreCase = true) == true) ||
                     (user.mobile?.contains(query) == true))
                }
                return@withContext Result.success(users)
            } else if (response.code() == 401 || response.code() == 403) {
                return@withContext Result.failure(SecurityException("आपल्याला या गटासाठी परवानगी नाही."))
            } else {
                return@withContext Result.failure(Exception("सर्व्हरशी संपर्क होऊ शकला नाही. कृपया पुन्हा प्रयत्न करा."))
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("सर्व्हरशी संपर्क होऊ शकला नाही. कृपया पुन्हा प्रयत्न करा.", e))
        }
    }

    suspend fun searchEligibleStudentsForTeacherGroup(
        query: String = ""
    ): Result<List<UserProfile>> = withContext(Dispatchers.IO) {
        try {
            val profile = getActiveSessionProfile()
            if (profile.userRole != UserRole.TEACHER) {
                return@withContext Result.failure(
                    SecurityException("आपल्याला या गटासाठी परवानगी नाही.")
                )
            }
            val schoolId = profile.schoolId
            if (schoolId.isNullOrBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("शिक्षकाला शाळा जोडलेली नाही. (Teacher has no assigned school)")
                )
            }

            val (api, token) = getApiAndToken()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.searchProfiles(
                apiKey = anonKey,
                bearerToken = "Bearer $token",
                roleFilter = "eq.student",
                schoolIdFilter = "eq.$schoolId",
                isActiveFilter = "eq.true"
            )

            if (response.isSuccessful && response.body() != null) {
                val students = response.body()!!.filter { user ->
                    user.role.equals("student", ignoreCase = true) &&
                    user.isActive &&
                    user.schoolId == schoolId &&
                    (query.isBlank() ||
                     (user.fullName?.contains(query, ignoreCase = true) == true) ||
                     (user.email?.contains(query, ignoreCase = true) == true) ||
                     (user.mobile?.contains(query) == true))
                }
                return@withContext Result.success(students)
            } else if (response.code() == 401 || response.code() == 403) {
                return@withContext Result.failure(SecurityException("आपल्याला या गटासाठी परवानगी नाही."))
            } else {
                return@withContext Result.failure(Exception("सर्व्हरशी संपर्क होऊ शकला नाही. कृपया पुन्हा प्रयत्न करा."))
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("सर्व्हरशी संपर्क होऊ शकला नाही. कृपया पुन्हा प्रयत्न करा.", e))
        }
    }

    suspend fun getActiveSchools(): Result<List<School>> = withContext(Dispatchers.IO) {
        try {
            val (api, token) = getApiAndToken()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.getSchools(
                apiKey = anonKey,
                bearerToken = "Bearer $token",
                select = "*"
            )
            if (response.isSuccessful && response.body() != null) {
                val schools = response.body()!!.filter { it.isActive }
                return@withContext Result.success(schools)
            } else {
                return@withContext Result.failure(Exception("सर्व्हरशी संपर्क होऊ शकला नाही. कृपया पुन्हा प्रयत्न करा."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("सर्व्हरशी संपर्क होऊ शकला नाही. कृपया पुन्हा प्रयत्न करा.", e))
        }
    }

    suspend fun getGroupMessages(
        groupId: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        try {
            if (groupId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Group ID cannot be blank"))
            }
            val profile = getActiveSessionProfile()
            val (api, token) = getApiAndToken()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)

            val response = api.getGroupMessages(
                apiKey = anonKey,
                bearerToken = "Bearer $token",
                groupIdFilter = "eq.$groupId",
                isDeletedFilter = "eq.false",
                select = "*,sender_profile:profiles(*)",
                order = "created_at.asc"
            )

            if (response.isSuccessful && response.body() != null) {
                val messages = response.body()!!
                val paginated = if (offset > 0 || messages.size > limit) {
                    messages.drop(offset).take(limit)
                } else {
                    messages
                }
                return@withContext Result.success(paginated)
            } else if (response.code() == 401 || response.code() == 403) {
                return@withContext Result.failure(SecurityException("आपल्याला या गटातील संदेश पाहण्याची परवानगी नाही."))
            } else {
                return@withContext Result.failure(Exception("सर्व्हरशी संपर्क होऊ शकला नाही. कृपया पुन्हा प्रयत्न करा."))
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("सर्व्हरशी संपर्क होऊ शकला नाही. कृपया पुन्हा प्रयत्न करा.", e))
        }
    }

    suspend fun sendGroupMessage(
        groupId: String,
        content: String
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val trimmedContent = content.trim()
            if (groupId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Group ID cannot be blank"))
            }
            if (trimmedContent.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("संदेश रिकामा असू शकत नाही. (Message content cannot be blank)"))
            }
            val profile = getActiveSessionProfile()
            val (api, token) = getApiAndToken()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)

            val response = api.sendGroupMessageRpc(
                apiKey = anonKey,
                bearerToken = "Bearer $token",
                request = SendGroupMessageRequest(groupId = groupId, content = trimmedContent)
            )

            if (response.isSuccessful && response.body() != null) {
                return@withContext Result.success(response.body()!!)
            } else if (response.code() == 401 || response.code() == 403) {
                return@withContext Result.failure(SecurityException("आपल्याला या गटात संदेश पाठवण्याची परवानगी नाही."))
            } else {
                return@withContext Result.failure(Exception("संदेश पाठवण्यात त्रुटी आली. कृपया पुन्हा प्रयत्न करा."))
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("संदेश पाठवण्यात त्रुटी आली. कृपया पुन्हा प्रयत्न करा.", e))
        }
    }
}
