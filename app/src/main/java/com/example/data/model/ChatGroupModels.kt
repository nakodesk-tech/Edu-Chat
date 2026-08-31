package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class GroupType(val dbValue: String, val marathiTitle: String) {
    @Json(name = "administrative")
    ADMINISTRATIVE("administrative", "प्रशासकीय गट"),

    @Json(name = "teacher")
    TEACHER("teacher", "शिक्षक गट");

    companion object {
        fun fromDbValue(value: String?): GroupType? {
            return entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) }
        }
    }
}

@JsonClass(generateAdapter = true)
data class Group(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "group_type") val groupType: String,
    @Json(name = "created_by") val createdBy: String,
    @Json(name = "school_id") val schoolId: String? = null,
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "member_count") val memberCount: Int = 0,
    @Json(name = "creator_name") val creatorName: String? = null,
    @Json(name = "school_name") val schoolName: String? = null
) {
    val typedGroupType: GroupType
        get() = GroupType.fromDbValue(groupType) ?: GroupType.ADMINISTRATIVE

    val isAdministrative: Boolean
        get() = groupType.equals("administrative", ignoreCase = true)

    val isTeacherGroup: Boolean
        get() = groupType.equals("teacher", ignoreCase = true)
}

@JsonClass(generateAdapter = true)
data class GroupMember(
    @Json(name = "id") val id: String,
    @Json(name = "group_id") val groupId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "role_in_group") val roleInGroup: String? = "member",
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "joined_at") val joinedAt: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "user_profile") val userProfile: UserProfile? = null
)

@JsonClass(generateAdapter = true)
data class CreateGroupRequest(
    @Json(name = "name") val name: String,
    @Json(name = "group_type") val groupType: String
)

@JsonClass(generateAdapter = true)
data class AddGroupMemberRequest(
    @Json(name = "group_id") val groupId: String,
    @Json(name = "user_id") val userId: String
)

@JsonClass(generateAdapter = true)
data class AddGroupMembersBatchRequest(
    @Json(name = "group_id") val groupId: String,
    @Json(name = "user_ids") val userIds: List<String>
)

@JsonClass(generateAdapter = true)
data class RemoveGroupMemberRequest(
    @Json(name = "group_id") val groupId: String,
    @Json(name = "user_id") val userId: String
)

data class GroupDetails(
    val group: Group,
    val members: List<GroupMember>,
    val creatorProfile: UserProfile? = null
)

@JsonClass(generateAdapter = true)
data class ChatMessage(
    @Json(name = "id") val id: String,
    @Json(name = "group_id") val groupId: String,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "content") val content: String = "",
    @Json(name = "message_type") val messageType: String = "text",
    @Json(name = "media_url") val mediaUrl: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "is_deleted") val isDeleted: Boolean = false,
    @Json(name = "sender_profile") val senderProfile: UserProfile? = null
) {
    val isImageMessage: Boolean
        get() = messageType.equals("image", ignoreCase = true) || !mediaUrl.isNullOrBlank()
}

@JsonClass(generateAdapter = true)
data class SendGroupMessageRequest(
    @Json(name = "group_id") val groupId: String,
    @Json(name = "content") val content: String = "",
    @Json(name = "message_type") val messageType: String = "text",
    @Json(name = "media_url") val mediaUrl: String? = null
)

