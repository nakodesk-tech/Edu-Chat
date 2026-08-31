package com.example.data.repository

import android.content.Context
import com.example.data.local.SessionManager
import com.example.data.model.School
import com.example.data.remote.SupabaseAuthApi
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Access-controlled repository for School queries.
 * Enforces PostgreSQL Row-Level Security (RLS) scoping rules:
 * - officer_admin -> can view all schools
 * - school_admin  -> can view only their assigned school
 * - teacher       -> can view only their assigned school
 * - student       -> can view only their assigned school
 *
 * All operations execute strictly against Supabase backend.
 * Production fallback to simulated storage has been completely removed.
 */
class SchoolRepository(
    private val context: Context,
    private val sessionManager: SessionManager = SessionManager(context),
    private val apiOverride: SupabaseAuthApi? = null
) {
    private fun getApi(): SupabaseAuthApi {
        return apiOverride ?: SupabaseClient.getApi(context)
    }

    /**
     * Get schools accessible to the currently authenticated user based on role and assignment.
     */
    suspend fun getAccessibleSchools(): Result<List<School>> = withContext(Dispatchers.IO) {
        val session = sessionManager.getSession()
            ?: return@withContext Result.failure(SecurityException("Authentication required. No active session."))

        val profile = session.profile
        if (!profile.isActive) {
            return@withContext Result.failure(SecurityException("Your account is deactivated."))
        }

        try {
            val api = getApi()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)

            // Supabase PostgREST applies server-side RLS automatically based on auth.jwt()
            val response = if (profile.role.equals("officer_admin", ignoreCase = true)) {
                api.getSchools(
                    apiKey = anonKey,
                    bearerToken = "Bearer ${session.accessToken}"
                )
            } else {
                if (profile.schoolId.isNullOrBlank()) {
                    return@withContext Result.success(emptyList())
                }
                api.getSchoolById(
                    apiKey = anonKey,
                    bearerToken = "Bearer ${session.accessToken}",
                    idFilter = "eq.${profile.schoolId}"
                )
            }

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to query schools."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Query a specific school by ID.
     * Enforces RLS: non-officer-admin roles can ONLY query their assigned school.
     */
    suspend fun getSchoolById(schoolId: String): Result<School> = withContext(Dispatchers.IO) {
        val session = sessionManager.getSession()
            ?: return@withContext Result.failure(SecurityException("Authentication required. No active session."))

        val profile = session.profile
        if (!profile.isActive) {
            return@withContext Result.failure(SecurityException("Your account is deactivated."))
        }

        // Security check matching RLS:
        val isOfficer = profile.role.equals("officer_admin", ignoreCase = true)
        val isAssigned = profile.schoolId.equals(schoolId, ignoreCase = true)

        if (!isOfficer && !isAssigned) {
            return@withContext Result.failure(
                SecurityException("Access denied. You do not have permission to view school details for school ID: $schoolId")
            )
        }

        try {
            val api = getApi()
            val anonKey = SupabaseConfig.getSupabaseAnonKey(context)
            val response = api.getSchoolById(
                apiKey = anonKey,
                bearerToken = "Bearer ${session.accessToken}",
                idFilter = "eq.$schoolId"
            )
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!!.first())
            } else {
                Result.failure(Exception("School not found or access restricted."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
