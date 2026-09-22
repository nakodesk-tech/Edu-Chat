package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AssessmentSummary(
    @Json(name="id") val id: String,
    @Json(name="title") val title: String,
    @Json(name="subject") val subject: String,
    @Json(name="standard") val standard: String,
    @Json(name="duration_minutes") val durationMinutes: Int,
    @Json(name="status") val status: String,
    @Json(name="total_questions") val totalQuestions: Int,
    @Json(name="created_by") val createdBy: String,
    @Json(name="school_id") val schoolId: String? = null,
    @Json(name="created_at") val createdAt: String? = null,
    @Json(name="published_at") val publishedAt: String? = null,
    @Json(name="shared_student_count") val sharedStudentCount: Long = 0,
    @Json(name="completed_student_count") val completedStudentCount: Long = 0
) {
    val isDraft get() = status == "draft"
    val isPublished get() = status == "published"
}

@JsonClass(generateAdapter = true)
data class AssessmentQuestion(
    @Json(name="id") val id: String,
    @Json(name="assessment_id") val assessmentId: String,
    @Json(name="question_no") val questionNo: Int,
    @Json(name="question_text") val questionText: String,
    @Json(name="options") val options: Map<String, String>,
    @Json(name="correct_option") val correctOption: String? = null,
    @Json(name="marks") val marks: Int = 1
)

@JsonClass(generateAdapter = true)
data class AssessmentQuestionInput(
    @Json(name="question_no") val questionNo: Int,
    @Json(name="question_text") val questionText: String,
    @Json(name="options") val options: Map<String, String>,
    @Json(name="correct_option") val correctOption: String,
    @Json(name="marks") val marks: Int = 1
)

@JsonClass(generateAdapter = true)
data class CreateAssessmentRequest(
    @Json(name="p_title") val title: String,
    @Json(name="p_subject") val subject: String,
    @Json(name="p_standard") val standard: String,
    @Json(name="p_duration_minutes") val durationMinutes: Int,
    @Json(name="p_questions") val questions: List<AssessmentQuestionInput>,
    @Json(name="p_status") val status: String = "published"
)

@JsonClass(generateAdapter = true)
data class ShareAssessmentRequest(
    @Json(name="p_assessment_id") val assessmentId: String,
    @Json(name="p_group_id") val groupId: String
)

@JsonClass(generateAdapter = true)
data class SubmitAssessmentRequest(
    @Json(name="p_assessment_id") val assessmentId: String,
    @Json(name="p_group_id") val groupId: String,
    @Json(name="p_answers") val answers: Map<String, String>
)

@JsonClass(generateAdapter = true)
data class AssessmentAttempt(
    @Json(name="id") val id: String,
    @Json(name="assessment_id") val assessmentId: String,
    @Json(name="student_id") val studentId: String,
    @Json(name="group_id") val groupId: String,
    @Json(name="answers") val answers: Map<String, String> = emptyMap(),
    @Json(name="score") val score: Int = 0,
    @Json(name="total_marks") val totalMarks: Int = 0,
    @Json(name="status") val status: String = "completed",
    @Json(name="completed_at") val completedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AssessmentShareId(
    @Json(name="assessment_id") val assessmentId: String
)
