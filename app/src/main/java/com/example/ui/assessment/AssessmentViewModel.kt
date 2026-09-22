package com.example.ui.assessment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AssessmentAttempt
import com.example.data.model.AssessmentQuestion
import com.example.data.model.AssessmentQuestionInput
import com.example.data.model.AssessmentSummary
import com.example.data.model.Group
import com.example.data.model.UserRole
import com.example.data.repository.AssessmentRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssessmentUiState(
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val assessments: List<AssessmentSummary> = emptyList(),
    val groups: List<Group> = emptyList(),
    val attempts: List<AssessmentAttempt> = emptyList(),
    val questions: List<AssessmentQuestion> = emptyList(),
    val selectedAssessment: AssessmentSummary? = null,
    val selectedGroupId: String? = null,
    val answers: Map<String, String> = emptyMap(),
    val errorMessage: String? = null,
    val snackbarMessage: String? = null,
    val searchQuery: String = ""
) {
}

class AssessmentViewModel(
    application: Application,
    private val assessmentRepository: AssessmentRepository = AssessmentRepository(application),
    private val groupRepository: GroupRepository = GroupRepository(application),
    private val authRepository: AuthRepository = AuthRepository(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AssessmentUiState())
    val uiState: StateFlow<AssessmentUiState> = _uiState.asStateFlow()

    val currentSession get() = authRepository.getActiveSession()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val assessments = assessmentRepository.getAssessments()
            val groups = groupRepository.getGroups()
            val attempts = assessmentRepository.getMyAttempts()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    assessments = assessments.getOrDefault(emptyList()),
                    groups = groups.getOrDefault(emptyList()),
                    attempts = attempts.getOrDefault(emptyList()),
                    errorMessage = assessments.exceptionOrNull()?.message
                )
            }
        }
    }

    fun setSearchQuery(value: String) {
        _uiState.update { it.copy(searchQuery = value) }
    }

    fun createAssessment(
        title: String,
        subject: String,
        standard: String,
        durationMinutes: Int,
        questions: List<AssessmentQuestionInput>,
        onSuccess: () -> Unit
    ) {
        if (questions.isEmpty() || title.isBlank() || subject.isBlank() || standard.isBlank()) {
            _uiState.update { it.copy(errorMessage = "चाचणीचे नाव, विषय, इयत्ता आणि किमान एक प्रश्न आवश्यक आहे.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            val result = assessmentRepository.createAssessment(title, subject, standard, durationMinutes, questions)
            if (result.isSuccess) {
                _uiState.update { it.copy(isActionLoading = false, snackbarMessage = "चाचणी यशस्वीरित्या तयार झाली.") }
                load()
                onSuccess()
            } else {
                _uiState.update { it.copy(isActionLoading = false, errorMessage = result.exceptionOrNull()?.message ?: "चाचणी तयार करता आली नाही.") }
            }
        }
    }

    fun shareAssessment(assessmentId: String, groupId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            val result = assessmentRepository.shareAssessment(assessmentId, groupId)
            if (result.isSuccess) {
                _uiState.update { it.copy(isActionLoading = false, snackbarMessage = "चाचणी गटात शेअर केली.") }
                load()
                onSuccess()
            } else {
                _uiState.update { it.copy(isActionLoading = false, errorMessage = result.exceptionOrNull()?.message ?: "चाचणी शेअर करता आली नाही.") }
            }
        }
    }

    fun openAssessment(assessment: AssessmentSummary, groupId: String? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isActionLoading = true,
                    selectedAssessment = assessment,
                    selectedGroupId = groupId,
                    questions = emptyList(),
                    answers = emptyMap(),
                    errorMessage = null
                )
            }
            val includeAnswers = currentSession?.profile?.userRole != UserRole.STUDENT
            val result = assessmentRepository.getQuestions(assessment.id, includeAnswers)
            _uiState.update {
                it.copy(
                    isActionLoading = false,
                    questions = result.getOrDefault(emptyList()),
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun openAssessmentForStudent(assessment: AssessmentSummary) {
        viewModelScope.launch {
            val groups = _uiState.value.groups
            for (group in groups) {
                val shared = assessmentRepository.getAssessmentsForGroup(group.id).getOrDefault(emptyList())
                if (shared.any { it.id == assessment.id }) {
                    openAssessment(assessment, group.id)
                    return@launch
                }
            }
            _uiState.update { it.copy(errorMessage = "ही चाचणी सध्या कोणत्याही उपलब्ध गटाशी जोडलेली नाही.") }
        }
    }

    fun setAnswer(questionNo: Int, option: String) {
        _uiState.update { it.copy(answers = it.answers + (questionNo.toString() to option)) }
    }

    fun submitAssessment(onSuccess: (AssessmentAttempt) -> Unit) {
        val state = _uiState.value
        val assessment = state.selectedAssessment ?: return
        val groupId = state.selectedGroupId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            val result = assessmentRepository.submitAttempt(assessment.id, groupId, state.answers)
            if (result.isSuccess) {
                val attempt = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        attempts = it.attempts.filterNot { a -> a.assessmentId == assessment.id && a.groupId == groupId } + attempt,
                        snackbarMessage = "चाचणी यशस्वीरित्या जमा झाली."
                    )
                }
                onSuccess(attempt)
            } else {
                _uiState.update { it.copy(isActionLoading = false, errorMessage = result.exceptionOrNull()?.message ?: "चाचणी जमा करता आली नाही.") }
            }
        }
    }

    fun closeAssessment() {
        _uiState.update { it.copy(selectedAssessment = null, selectedGroupId = null, questions = emptyList(), answers = emptyMap(), errorMessage = null) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
