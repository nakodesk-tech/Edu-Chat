package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AuthSession
import com.example.data.model.UserProfile
import com.example.data.remote.SupabaseClient

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "edu_chat_auth_session"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_PROFILE = "user_profile_json"
    }

    fun saveSession(session: AuthSession) {
        val profileJson = try {
            SupabaseClient.moshi.adapter(UserProfile::class.java).toJson(session.profile)
        } catch (_: Exception) {
            null
        }

        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putString(KEY_USER_PROFILE, profileJson)
            .apply()
    }

    fun updateTokens(newAccessToken: String, newRefreshToken: String?) {
        val editor = prefs.edit().putString(KEY_ACCESS_TOKEN, newAccessToken)
        if (!newRefreshToken.isNullOrBlank()) {
            editor.putString(KEY_REFRESH_TOKEN, newRefreshToken)
        }
        editor.apply()
    }

    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    fun getUserProfile(): UserProfile? {
        val json = prefs.getString(KEY_USER_PROFILE, null) ?: return null
        return try {
            SupabaseClient.moshi.adapter(UserProfile::class.java).fromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    fun getSession(): AuthSession? {
        val token = getAccessToken() ?: return null
        val profile = getUserProfile() ?: return null
        return AuthSession(
            accessToken = token,
            refreshToken = getRefreshToken(),
            profile = profile
        )
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_PROFILE)
            .commit()
    }

    fun hasActiveSession(): Boolean {
        return !getAccessToken().isNullOrBlank() && getUserProfile() != null
    }
}
