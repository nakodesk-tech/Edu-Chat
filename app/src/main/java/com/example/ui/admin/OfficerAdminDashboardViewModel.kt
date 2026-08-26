package com.example.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.School
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.OfficerAdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OfficerAdminTab(val marathiTitle: String) {
    CHATS("चॅट्स"),
    USERS("वापरकर्ते"),
    SCHOOLS("शाळा"),
    MY_PROFILE("माझे प्रोफाइल")
}

sealed class OfficerAdminDialog {
    object None : OfficerAdminDialog()
    object CreateOfficerAdmin : OfficerAdminDialog()
    object CreateSchoolAdmin : OfficerAdminDialog()
    object CreateSchool : OfficerAdminDialog()
    object ManageSchools : OfficerAdminDialog()
    object MyProfile : OfficerAdminDialog()
    data class EditSchool(val school: School) : OfficerAdminDialog()
    data class ConfirmDeactivateSchool(val school: School, val staffCount: Int) : OfficerAdminDialog()
}

data class OfficerAdminDashboardUiState(
    val selectedTab: OfficerAdminTab = OfficerAdminTab.CHATS,
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val profile: UserProfile? = null,
    val schools: List<School> = emptyList(),
    val activeSchools: List<School> = emptyList(),
    val currentDialog: OfficerAdminDialog = OfficerAdminDialog.None,
    val snackbarMessage: String? = null,
    val errorMessage: String? = null,
    val isLoggedOut: Boolean = false
)

class OfficerAdminDashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val officerRepo = OfficerAdminRepository(application)
    private val authRepo = AuthRepository(application)

    private val _uiState = MutableStateFlow(OfficerAdminDashboardUiState())
    val uiState: StateFlow<OfficerAdminDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val profileResult = officerRepo.getOfficerProfile()
            if (profileResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = profileResult.exceptionOrNull()?.message ?: "Authorization failed."
                    )
                }
                return@launch
            }

            val schoolsResult = officerRepo.getSchools()
            val schoolsList = schoolsResult.getOrDefault(emptyList())

            _uiState.update {
                it.copy(
                    isLoading = false,
                    profile = profileResult.getOrNull(),
                    schools = schoolsList,
                    activeSchools = schoolsList.filter { s -> s.isActive }
                )
            }
        }
    }

    fun selectTab(tab: OfficerAdminTab) {
        _uiState.update { it.copy(selectedTab = tab, errorMessage = null) }
    }

    fun openDialog(dialog: OfficerAdminDialog) {
        _uiState.update { it.copy(currentDialog = dialog, errorMessage = null) }
    }

    fun closeDialog() {
        _uiState.update { it.copy(currentDialog = OfficerAdminDialog.None, errorMessage = null) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    /**
     * Tile 1: Create Officer Admin (role: officer_admin, is_primary_admin: false, school_id: NULL)
     */
    fun createOfficerAdmin(
        fullName: String,
        email: String,
        mobile: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            val result = officerRepo.createOfficerAdmin(
                fullNameInput = fullName,
                emailInput = email,
                mobileInput = mobile,
                passwordInput = password
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        snackbarMessage = "नवीन अधिकारी प्रशासक यशस्वीरित्या नोंदणीकृत झाला!"
                    )
                }
                closeDialog()
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "अधिकारी नोंदणी अयशस्वी झाली."
                    )
                }
            }
        }
    }

    /**
     * Tile 2: Create School Admin (role: school_admin, school_id: selected)
     */
    fun createSchoolAdmin(
        fullName: String,
        email: String,
        mobile: String,
        password: String,
        schoolId: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            val result = officerRepo.createSchoolAdmin(
                fullNameInput = fullName,
                emailInput = email,
                mobileInput = mobile,
                passwordInput = password,
                schoolIdInput = schoolId
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        snackbarMessage = "शाळा प्रशासक यशस्वीरित्या नोंदणीकृत झाला!"
                    )
                }
                closeDialog()
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "शाळा प्रशासक नोंदणी अयशस्वी झाली."
                    )
                }
            }
        }
    }

    /**
     * Tile 3: Register New School
     */
    fun createSchool(
        name: String,
        code: String,
        address: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            val result = officerRepo.createSchool(
                nameInput = name,
                codeInput = code,
                addressInput = address
            )

            if (result.isSuccess) {
                refreshSchools()
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        snackbarMessage = "शाळा यशस्वीरित्या नोंदणीकृत झाली!"
                    )
                }
                closeDialog()
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "शाळा नोंदणी अयशस्वी झाली."
                    )
                }
            }
        }
    }

    /**
     * Tile 4: Update School
     */
    fun updateSchool(
        schoolId: String,
        name: String,
        code: String,
        address: String,
        isActive: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            val result = officerRepo.updateSchool(
                schoolId = schoolId,
                nameInput = name,
                codeInput = code,
                addressInput = address,
                isActive = isActive
            )

            if (result.isSuccess) {
                refreshSchools()
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        snackbarMessage = "शाळेची माहिती यशस्वीरित्या अद्यतनित झाली!"
                    )
                }
                openDialog(OfficerAdminDialog.ManageSchools)
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "शाळा अद्यतन अयशस्वी झाले."
                    )
                }
            }
        }
    }

    /**
     * Tile 4: Request Deactivation (Shows confirmation with staff count)
     */
    fun requestToggleSchoolStatus(school: School) {
        if (school.isActive) {
            val staffCount = officerRepo.getActiveStaffCountForSchool(school.id)
            openDialog(OfficerAdminDialog.ConfirmDeactivateSchool(school, staffCount))
        } else {
            executeToggleSchoolStatus(school.id, true)
        }
    }

    fun executeToggleSchoolStatus(schoolId: String, newActiveStatus: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            val result = officerRepo.toggleSchoolStatus(schoolId, newActiveStatus)

            if (result.isSuccess) {
                refreshSchools()
                val msg = if (newActiveStatus) "शाळा यशस्वीरित्या सक्रिय केली गेली." else "शाळा निष्क्रिय केली गेली."
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        snackbarMessage = msg
                    )
                }
                openDialog(OfficerAdminDialog.ManageSchools)
            } else {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "स्थिती बदल अयशस्वी झाला."
                    )
                }
            }
        }
    }

    private suspend fun refreshSchools() {
        val schoolsResult = officerRepo.getSchools()
        val schoolsList = schoolsResult.getOrDefault(emptyList())
        _uiState.update {
            it.copy(
                schools = schoolsList,
                activeSchools = schoolsList.filter { s -> s.isActive }
            )
        }
    }

    /**
     * Tile 5: Update Officer Profile
     */
    fun updateProfile(
        fullName: String,
        mobile: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            val result = officerRepo.updateOfficerProfile(
                fullNameInput = fullName,
                mobileInput = mobile
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        profile = result.getOrNull(),
                        snackbarMessage = "प्रोफाइल यशस्वीरित्या अद्यतनित केले!"
                    )
                }
                closeDialog()
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "प्रोफाइल अद्यतन अयशस्वी झाले."
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepo.logout()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }
}
