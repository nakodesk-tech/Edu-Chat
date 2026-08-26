package com.example.ui.schooladmin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.School
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.SchoolAdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SchoolAdminTab(val marathiTitle: String) {
    CHATS("चॅट्स"),
    TEACHERS("शिक्षक"),
    MY_SCHOOL("माझी शाळा"),
    MY_PROFILE("माझे प्रोफाइल")
}

sealed class SchoolAdminDialog {
    object None : SchoolAdminDialog()
    object CreateTeacher : SchoolAdminDialog()
    data class EditTeacher(val teacher: UserProfile) : SchoolAdminDialog()
    data class ConfirmDeactivateTeacher(val teacher: UserProfile) : SchoolAdminDialog()
    object EditProfile : SchoolAdminDialog()
}

data class SchoolAdminUiState(
    val selectedTab: SchoolAdminTab = SchoolAdminTab.TEACHERS,
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val profile: UserProfile? = null,
    val assignedSchool: School? = null,
    val teachers: List<UserProfile> = emptyList(),
    val searchQuery: String = "",
    val currentDialog: SchoolAdminDialog = SchoolAdminDialog.None,
    val snackbarMessage: String? = null,
    val errorMessage: String? = null
) {
    val filteredTeachers: List<UserProfile>
        get() {
            if (searchQuery.isBlank()) return teachers
            val q = searchQuery.trim().lowercase()
            return teachers.filter {
                (it.fullName ?: "").lowercase().contains(q) ||
                (it.email ?: "").lowercase().contains(q) ||
                (it.mobile ?: "").contains(q)
            }
        }
}

class SchoolAdminDashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val schoolAdminRepo = SchoolAdminRepository(application)
    private val authRepo = AuthRepository(application)

    private val _uiState = MutableStateFlow(SchoolAdminUiState())
    val uiState: StateFlow<SchoolAdminUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun selectTab(tab: SchoolAdminTab) {
        _uiState.update { it.copy(selectedTab = tab, errorMessage = null) }
        if (tab == SchoolAdminTab.TEACHERS && _uiState.value.teachers.isEmpty()) {
            loadTeachers()
        } else if (tab == SchoolAdminTab.MY_SCHOOL && _uiState.value.assignedSchool == null) {
            loadSchool()
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val profileResult = schoolAdminRepo.getSchoolAdminProfile()
            if (profileResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = profileResult.exceptionOrNull()?.message ?: "Authorization failed."
                    )
                }
                return@launch
            }

            val schoolResult = schoolAdminRepo.getAssignedSchool()
            val teachersResult = schoolAdminRepo.getTeachers()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    profile = profileResult.getOrNull(),
                    assignedSchool = schoolResult.getOrNull(),
                    teachers = teachersResult.getOrDefault(emptyList())
                )
            }
        }
    }

    fun loadTeachers() {
        viewModelScope.launch {
            val teachersResult = schoolAdminRepo.getTeachers()
            if (teachersResult.isSuccess) {
                _uiState.update { it.copy(teachers = teachersResult.getOrNull() ?: emptyList()) }
            }
        }
    }

    fun loadSchool() {
        viewModelScope.launch {
            val schoolResult = schoolAdminRepo.getAssignedSchool()
            if (schoolResult.isSuccess) {
                _uiState.update { it.copy(assignedSchool = schoolResult.getOrNull()) }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openDialog(dialog: SchoolAdminDialog) {
        _uiState.update { it.copy(currentDialog = dialog, errorMessage = null) }
    }

    fun closeDialog() {
        _uiState.update { it.copy(currentDialog = SchoolAdminDialog.None, errorMessage = null) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    /**
     * Create a new Teacher strictly for the caller's school
     */
    fun createTeacher(
        fullName: String,
        email: String,
        mobile: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        if (_uiState.value.isActionLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            val result = schoolAdminRepo.createTeacher(
                fullNameInput = fullName,
                emailInput = email,
                mobileInput = mobile,
                passwordInput = password
            )
            _uiState.update { it.copy(isActionLoading = false) }

            if (result.isSuccess) {
                closeDialog()
                _uiState.update {
                    it.copy(
                        snackbarMessage = "शिक्षक यशस्वीरित्या नोंदणीकृत झाले!"
                    )
                }
                loadTeachers()
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = result.exceptionOrNull()?.message ?: "शिक्षक नोंदणी पूर्ण होऊ शकली नाही."
                    )
                }
            }
        }
    }

    /**
     * Update Teacher details (Name, Mobile, Active status)
     */
    fun updateTeacher(
        teacherId: String,
        fullName: String,
        mobile: String,
        isActive: Boolean,
        onSuccess: () -> Unit
    ) {
        if (_uiState.value.isActionLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            val result = schoolAdminRepo.updateTeacher(
                teacherId = teacherId,
                fullNameInput = fullName,
                mobileInput = mobile,
                isActive = isActive
            )
            _uiState.update { it.copy(isActionLoading = false) }

            if (result.isSuccess) {
                closeDialog()
                _uiState.update {
                    it.copy(
                        snackbarMessage = "शिक्षकांची माहिती यशस्वीरित्या अद्यतनित झाली!"
                    )
                }
                loadTeachers()
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = result.exceptionOrNull()?.message ?: "माहिती अद्यतनित करण्यात त्रुटी आली."
                    )
                }
            }
        }
    }

    /**
     * Soft toggle Teacher Active / Inactive status
     */
    fun toggleTeacherStatus(teacher: UserProfile, isActive: Boolean) {
        if (_uiState.value.isActionLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            val result = schoolAdminRepo.toggleTeacherStatus(teacher.id, isActive)
            _uiState.update { it.copy(isActionLoading = false) }

            if (result.isSuccess) {
                closeDialog()
                val msg = if (isActive) "शिक्षक पुन्हा सक्रिय करण्यात आले." else "शिक्षक निष्क्रिय करण्यात आले."
                _uiState.update { it.copy(snackbarMessage = msg) }
                loadTeachers()
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = result.exceptionOrNull()?.message ?: "स्थिती बदलण्यात त्रुटी आली."
                    )
                }
            }
        }
    }

    /**
     * Update School Admin's own profile (Name, Mobile)
     */
    fun updateProfile(
        fullName: String,
        mobile: String,
        onSuccess: () -> Unit
    ) {
        if (_uiState.value.isActionLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            val result = schoolAdminRepo.updateSchoolAdminProfile(
                fullNameInput = fullName,
                mobileInput = mobile
            )
            _uiState.update { it.copy(isActionLoading = false) }

            if (result.isSuccess) {
                closeDialog()
                _uiState.update {
                    it.copy(
                        profile = result.getOrNull(),
                        snackbarMessage = "आपले प्रोफाइल यशस्वीरित्या अद्यतनित झाले!"
                    )
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = result.exceptionOrNull()?.message ?: "प्रोफाइल अद्यतनित करण्यात त्रुटी आली."
                    )
                }
            }
        }
    }
}
