package com.example.data.remote

import com.example.data.model.AdminCreateUserRequest
import com.example.data.model.AdminToggleStatusRequest
import com.example.data.model.AdminUpdateUserRequest
import com.example.data.model.CreateGroupRequest
import com.example.data.model.CreateSchoolRequest
import com.example.data.model.Group
import com.example.data.model.GroupMember
import com.example.data.model.AddGroupMemberRequest
import com.example.data.model.RemoveGroupMemberRequest
import com.example.data.model.OfficerAdminCreateUserRequest
import com.example.data.model.School
import com.example.data.model.SchoolAdminCreateTeacherRequest
import com.example.data.model.SchoolAdminUpdateTeacherRequest
import com.example.data.model.SupabaseLoginRequest
import com.example.data.model.SupabaseSignupRequest
import com.example.data.model.SupabaseTokenResponse
import com.example.data.model.UpdateDisplayNameRequest
import com.example.data.model.UpdateOfficerProfileRequest
import com.example.data.model.UpdateSchoolRequest
import com.example.data.model.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseAuthApi {
    @POST("auth/v1/token")
    suspend fun login(
        @Query("grant_type") grantType: String = "password",
        @Header("apikey") apiKey: String,
        @Body request: SupabaseLoginRequest
    ): Response<SupabaseTokenResponse>

    @POST("auth/v1/signup")
    suspend fun signup(
        @Header("apikey") apiKey: String,
        @Body request: SupabaseSignupRequest
    ): Response<SupabaseTokenResponse>

    @POST("auth/v1/logout")
    suspend fun logout(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String
    ): Response<Unit>

    @GET("rest/v1/profiles")
    suspend fun getProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String, // e.g. "eq.USER_ID"
        @Query("select") select: String = "*"
    ): Response<List<UserProfile>>

    @GET("rest/v1/profiles")
    suspend fun getAllProfiles(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<UserProfile>>

    @POST("rest/v1/rpc/update_profile_display_name")
    suspend fun updateDisplayNameRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body request: UpdateDisplayNameRequest
    ): Response<UserProfile>

    @POST("rest/v1/rpc/admin_create_user")
    suspend fun adminCreateUserRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body request: AdminCreateUserRequest
    ): Response<UserProfile>

    @POST("rest/v1/rpc/admin_update_user")
    suspend fun adminUpdateUserRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body request: AdminUpdateUserRequest
    ): Response<UserProfile>

    @POST("rest/v1/rpc/admin_toggle_user_status")
    suspend fun adminToggleStatusRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body request: AdminToggleStatusRequest
    ): Response<UserProfile>

    @POST("rest/v1/rpc/officer_admin_create_user")
    suspend fun officerAdminCreateUserRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body request: OfficerAdminCreateUserRequest
    ): Response<UserProfile>

    @GET("rest/v1/schools")
    suspend fun getSchools(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<School>>

    @GET("rest/v1/schools")
    suspend fun getSchoolById(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String, // e.g. "eq.SCHOOL_ID"
        @Query("select") select: String = "*"
    ): Response<List<School>>

    @GET("rest/v1/profiles")
    suspend fun getTeachersBySchool(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("school_id") schoolIdFilter: String,
        @Query("role") roleFilter: String = "eq.teacher",
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<UserProfile>>

    @POST("rest/v1/rpc/school_admin_create_teacher")
    suspend fun schoolAdminCreateTeacherRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body request: SchoolAdminCreateTeacherRequest
    ): Response<UserProfile>

    @POST("rest/v1/rpc/school_admin_update_teacher")
    suspend fun schoolAdminUpdateTeacherRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body request: SchoolAdminUpdateTeacherRequest
    ): Response<UserProfile>

    @POST("rest/v1/rpc/officer_admin_create_school")
    suspend fun officerAdminCreateSchoolRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body request: CreateSchoolRequest
    ): Response<School>

    @POST("rest/v1/rpc/officer_admin_update_school")
    suspend fun officerAdminUpdateSchoolRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body request: UpdateSchoolRequest
    ): Response<School>

    @PATCH("rest/v1/profiles")
    suspend fun patchProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String,
        @Body updates: Map<String, Any?>
    ): Response<List<UserProfile>>

    @GET("rest/v1/groups")
    suspend fun getGroups(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("select") select: String = "*,group_members(*)",
        @Query("is_active") isActiveFilter: String = "eq.true",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<Group>>

    @GET("rest/v1/groups")
    suspend fun getGroupById(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<Group>>

    @GET("rest/v1/group_members")
    suspend fun getGroupMembers(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("group_id") groupIdFilter: String,
        @Query("is_active") isActiveFilter: String = "eq.true",
        @Query("select") select: String = "*,user_profile:profiles(*)"
    ): Response<List<GroupMember>>

    @GET("rest/v1/profiles")
    suspend fun searchProfiles(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("role") roleFilter: String? = null,
        @Query("school_id") schoolIdFilter: String? = null,
        @Query("is_active") isActiveFilter: String = "eq.true",
        @Query("select") select: String = "*",
        @Query("order") order: String = "full_name.asc"
    ): Response<List<UserProfile>>

    @POST("rest/v1/rpc/create_group")
    suspend fun createGroupRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body request: CreateGroupRequest
    ): Response<Group>

    @POST("rest/v1/rpc/add_group_member")
    suspend fun addGroupMemberRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body request: AddGroupMemberRequest
    ): Response<GroupMember>

    @POST("rest/v1/rpc/remove_group_member")
    suspend fun removeGroupMemberRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body request: RemoveGroupMemberRequest
    ): Response<Boolean>
}
