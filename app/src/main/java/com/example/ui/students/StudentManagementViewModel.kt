package com.example.ui.students

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SessionManager
import com.example.data.model.School
import com.example.data.model.UserProfile
import com.example.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface StudentDialogState {
    object None : StudentDialogState
    object Register : StudentDialogState
    data class Edit(val user: UserProfile) : StudentDialogState
    data class ConfirmToggleStatus(val user: UserProfile) : StudentDialogState
    data class ConfirmDelete(val user: UserProfile) : StudentDialogState
    data class Detail(val user: UserProfile) : StudentDialogState
}

data class StudentManagementUiState(
    val students: List<UserProfile> = emptyList(),
    val filteredStudents: List<UserProfile> = emptyList(),
    val schools: List<School> = emptyList(),
    val schoolsMap: Map<String, String> = emptyMap(),
    val currentSchoolId: String? = null,
    val isOfficerAdmin: Boolean = false,
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedStandardFilter: String = "सर्व",
    val selectedStatusFilter: String = "सर्व",
    val dialogState: StudentDialogState = StudentDialogState.None,
    val snackbarMessage: String? = null,
    val errorMessage: String? = null,
    val actionErrorMessage: String? = null
)

class StudentManagementViewModel @JvmOverloads constructor(
    application: Application,
    private val studentRepository: StudentRepository = StudentRepository(application)
) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val _uiState = MutableStateFlow(StudentManagementUiState())
    val uiState: StateFlow<StudentManagementUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        val session = sessionManager.getSession()
        val callerProfile = session?.profile
        val isOfficer = callerProfile?.isOfficerAdmin == true
        val schoolId = callerProfile?.schoolId

        _uiState.update {
            it.copy(
                isOfficerAdmin = isOfficer,
                currentSchoolId = schoolId
            )
        }

        loadStudents()
    }

    fun loadStudents() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                // Load schools first for lookups
                val schoolsRes = studentRepository.getSchools()
                val schoolsList = schoolsRes.getOrDefault(emptyList())
                val schoolsMap = schoolsList.associate { it.id to it.name }

                val studentsRes = studentRepository.getStudents()
                if (studentsRes.isSuccess) {
                    val list = studentsRes.getOrDefault(emptyList())
                    _uiState.update { state ->
                        val filtered = filterStudentsList(list, state.searchQuery, state.selectedStandardFilter, state.selectedStatusFilter)
                        state.copy(
                            students = list,
                            filteredStudents = filtered,
                            schools = schoolsList,
                            schoolsMap = schoolsMap,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = studentsRes.exceptionOrNull()?.message ?: "विद्यार्थी लोड करण्यात अयशस्वी."
                        )
                    }
                }
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "विद्यार्थी लोड करण्यात अयशस्वी."
                    )
                }
            }
        }
    }

    fun registerStudent(
        fullName: String,
        email: String,
        password: String,
        mobile: String?,
        standard: String?,
        schoolId: String,
        academicYear: String = StudentStandardUtils.DEFAULT_ACADEMIC_YEAR
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, actionErrorMessage = null) }
            val res = studentRepository.registerStudent(
                fullName = fullName,
                email = email,
                password = password,
                mobile = mobile,
                standard = standard,
                schoolId = schoolId,
                academicYear = academicYear
            )

            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        dialogState = StudentDialogState.None,
                        snackbarMessage = "विद्यार्थी '${fullName.trim()}' ची नोंदणी यशस्वीरीत्या झाली!"
                    )
                }
                loadStudents()
            } else {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        actionErrorMessage = res.exceptionOrNull()?.message ?: "नोंदणी अयशस्वी झाली."
                    )
                }
            }
        }
    }

    fun updateStudent(
        studentId: String,
        fullName: String,
        mobile: String?,
        standard: String?,
        schoolId: String?,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, actionErrorMessage = null) }
            val res = studentRepository.updateStudent(
                studentId = studentId,
                fullName = fullName,
                mobile = mobile,
                standard = standard,
                schoolId = schoolId,
                isActive = isActive
            )

            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        dialogState = StudentDialogState.None,
                        snackbarMessage = "विद्यार्थी माहिती यशस्वीरीत्या अद्यतनित केली!"
                    )
                }
                loadStudents()
            } else {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        actionErrorMessage = res.exceptionOrNull()?.message ?: "माहिती अद्यतनित करण्यात त्रुटी."
                    )
                }
            }
        }
    }

    fun toggleStatus(user: UserProfile, targetStatus: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, actionErrorMessage = null) }
            val res = studentRepository.toggleStudentStatus(user.id, targetStatus)

            if (res.isSuccess) {
                val statusText = if (targetStatus) "सक्रिय" else "निष्क्रीय"
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        dialogState = StudentDialogState.None,
                        snackbarMessage = "विद्यार्थी खाते $statusText केले!"
                    )
                }
                loadStudents()
            } else {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        actionErrorMessage = res.exceptionOrNull()?.message ?: "स्थिती बदलण्यात अयशस्वी."
                    )
                }
            }
        }
    }

    fun deleteStudent(user: UserProfile) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, actionErrorMessage = null) }
            val res = studentRepository.deleteStudent(user.id)

            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        dialogState = StudentDialogState.None,
                        snackbarMessage = "विद्यार्थी '${user.fullName ?: user.email}' यादीतून काढून टाकला."
                    )
                }
                loadStudents()
            } else {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        actionErrorMessage = res.exceptionOrNull()?.message ?: "विद्यार्थी काढून टाकण्यात अयशस्वी."
                    )
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = filterStudentsList(state.students, query, state.selectedStandardFilter, state.selectedStatusFilter)
            state.copy(searchQuery = query, filteredStudents = filtered)
        }
    }

    fun setStandardFilter(standard: String) {
        _uiState.update { state ->
            val filtered = filterStudentsList(state.students, state.searchQuery, standard, state.selectedStatusFilter)
            state.copy(selectedStandardFilter = standard, filteredStudents = filtered)
        }
    }

    fun setStatusFilter(status: String) {
        _uiState.update { state ->
            val filtered = filterStudentsList(state.students, state.searchQuery, state.selectedStandardFilter, status)
            state.copy(selectedStatusFilter = status, filteredStudents = filtered)
        }
    }

    fun openDialog(dialog: StudentDialogState) {
        _uiState.update { it.copy(dialogState = dialog, actionErrorMessage = null) }
    }

    fun dismissDialog() {
        if (!_uiState.value.isActionLoading) {
            _uiState.update { it.copy(dialogState = StudentDialogState.None, actionErrorMessage = null) }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun filterStudentsList(
        allStudents: List<UserProfile>,
        query: String,
        standardFilter: String,
        statusFilter: String
    ): List<UserProfile> {
        val q = query.trim().lowercase()
        return allStudents.filter { student ->
            // Search filter
            val matchesQuery = if (q.isBlank()) true else {
                (student.fullName?.lowercase()?.contains(q) == true) ||
                (student.email?.lowercase()?.contains(q) == true) ||
                (student.mobile?.contains(q) == true) ||
                (student.standard?.lowercase()?.contains(q) == true)
            }

            // Standard filter
            val matchesStandard = if (standardFilter == "सर्व" || standardFilter.equals("All", ignoreCase = true)) {
                true
            } else {
                val parsedStd = StudentStandardUtils.parseStandard(student.standard)
                parsedStd.equals(standardFilter, ignoreCase = true) ||
                    student.standard?.contains(standardFilter, ignoreCase = true) == true
            }

            // Status filter
            val matchesStatus = when (statusFilter) {
                "सक्रिय" -> student.isActive
                "निष्क्रीय" -> !student.isActive
                else -> true
            }

            matchesQuery && matchesStandard && matchesStatus
        }
    }
}
