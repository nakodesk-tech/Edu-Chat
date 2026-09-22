package com.example.data.repository

import android.content.Context
import com.example.data.local.SessionManager
import com.example.data.model.AssessmentAttempt
import com.example.data.model.AssessmentQuestion
import com.example.data.model.AssessmentQuestionInput
import com.example.data.model.AssessmentSummary
import com.example.data.model.AssessmentShareId
import com.example.data.model.CreateAssessmentRequest
import com.example.data.model.ShareAssessmentRequest
import com.example.data.model.SubmitAssessmentRequest
import com.example.data.remote.SupabaseAuthApi
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssessmentRepository(
    private val context: Context,
    private val sessionManager: SessionManager = SessionManager(context),
    private val apiOverride: SupabaseAuthApi? = null
) {
    private fun session() = sessionManager.getSession()
        ?: throw SecurityException("Authentication required.")

    private fun api(): SupabaseAuthApi = apiOverride ?: SupabaseClient.getApi(context)

    private fun headers(): Pair<String, String> {
        val s = session()
        return SupabaseConfig.getSupabaseAnonKey(context) to ("Bearer " + s.accessToken)
    }

    suspend fun getAssessments(): Result<List<AssessmentSummary>> = withContext(Dispatchers.IO) {
        try {
            val (key, bearer) = headers()
            val response = api().getAssessmentSummaries(key, bearer)
            if (response.isSuccessful) Result.success(response.body().orEmpty())
            else Result.failure(Exception(SupabaseClient.parseError(response.errorBody()?.string()) ?: "चाचण्या लोड करता आल्या नाहीत."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getQuestions(assessmentId: String, includeAnswers: Boolean): Result<List<AssessmentQuestion>> = withContext(Dispatchers.IO) {
        try {
            val (key, bearer) = headers()
            val response = api().getAssessmentQuestions(
                apiKey = key,
                bearerToken = bearer,
                assessmentIdFilter = "eq.$assessmentId",
                select = if (includeAnswers) "*" else "id,assessment_id,question_no,question_text,options,marks"
            )
            if (response.isSuccessful) Result.success(response.body().orEmpty())
            else Result.failure(Exception(SupabaseClient.parseError(response.errorBody()?.string()) ?: "प्रश्न लोड करता आले नाहीत."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createAssessment(
        title: String,
        subject: String,
        standard: String,
        durationMinutes: Int,
        questions: List<AssessmentQuestionInput>
    ): Result<AssessmentSummary> = withContext(Dispatchers.IO) {
        try {
            val (key, bearer) = headers()
            val response = api().createAssessmentRpc(
                key,
                bearer,
                CreateAssessmentRequest(title, subject, standard, durationMinutes, questions, "published")
            )
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!)
            else Result.failure(Exception(SupabaseClient.parseError(response.errorBody()?.string()) ?: "चाचणी तयार करता आली नाही."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun shareAssessment(assessmentId: String, groupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val (key, bearer) = headers()
            val response = api().shareAssessmentRpc(
                key,
                bearer,
                ShareAssessmentRequest(assessmentId, groupId)
            )
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(SupabaseClient.parseError(response.errorBody()?.string()) ?: "चाचणी गटात शेअर करता आली नाही."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAssessmentsForGroup(groupId: String): Result<List<AssessmentSummary>> = withContext(Dispatchers.IO) {
        try {
            val (key, bearer) = headers()
            val shares = api().getAssessmentSharesForGroup(key, bearer, "eq.$groupId")
            if (!shares.isSuccessful) {
                return@withContext Result.failure(Exception(SupabaseClient.parseError(shares.errorBody()?.string()) ?: "गटातील चाचण्या लोड करता आल्या नाहीत."))
            }
            val ids = shares.body().orEmpty().map(AssessmentShareId::assessmentId).toSet()
            if (ids.isEmpty()) return@withContext Result.success(emptyList())
            val all = getAssessments().getOrElse { return@withContext Result.failure(it) }
            Result.success(all.filter { ids.contains(it.id) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyAttempts(): Result<List<AssessmentAttempt>> = withContext(Dispatchers.IO) {
        try {
            val s = session()
            val (key, bearer) = headers()
            val response = api().getMyAssessmentAttempts(key, bearer, "eq." + s.profile.id)
            if (response.isSuccessful) Result.success(response.body().orEmpty())
            else Result.failure(Exception(SupabaseClient.parseError(response.errorBody()?.string()) ?: "सोडवलेल्या चाचण्या लोड करता आल्या नाहीत."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitAttempt(
        assessmentId: String,
        groupId: String,
        answers: Map<String, String>
    ): Result<AssessmentAttempt> = withContext(Dispatchers.IO) {
        try {
            val (key, bearer) = headers()
            val response = api().submitAssessmentRpc(
                key,
                bearer,
                SubmitAssessmentRequest(assessmentId, groupId, answers)
            )
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!)
            else Result.failure(Exception(SupabaseClient.parseError(response.errorBody()?.string()) ?: "चाचणी जमा करता आली नाही."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
