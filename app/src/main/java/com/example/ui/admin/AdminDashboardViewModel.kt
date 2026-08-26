package com.example.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SessionManager
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.repository.AdminUserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UserRoleFilter(val label: String) {
    ALL("All Users"),
    TEACHERS("Teachers"),
    STUDENTS("Students")
}

data class AdminDashboardUiState(
    val adminProfile: UserProfile? = null,
    val users: List<UserProfile> = emptyList(),
    val filter: UserRoleFilter = UserRoleFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showAddDialog: Boolean = false,
    val userToEdit: UserProfile? = null,
    val userToDeactivate: UserProfile? = null
) {
    val filteredUsers: List<UserProfile>
        get() {
            return users.filter { user ->
                val matchesFilter = when (filter) {
                    UserRoleFilter.ALL -> true
                    UserRoleFilter.TEACHERS -> user.role.equals("teacher", ignoreCase = true)
                    UserRoleFilter.STUDENTS -> user.role.equals("student", ignoreCase = true)
                }
                val matchesSearch = if (searchQuery.isBlank()) {
                    true
                } else {
                    val q = searchQuery.trim().lowercase()
                    (user.fullName?.lowercase()?.contains(q) == true) ||
                            (user.email?.lowercase()?.contains(q) == true) ||
                            (user.role.lowercase().contains(q))
                }
                matchesFilter && matchesSearch
            }
        }
}

class AdminDashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val adminRepository = AdminUserRepository(application)
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val currentSession = sessionManager.getSession()
        _uiState.update { it.copy(adminProfile = currentSession?.profile, isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = adminRepository.getUsers()
            result.onSuccess { userList ->
                _uiState.update {
                    it.copy(
                        users = userList,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to load users"
                    )
                }
            }
        }
    }

    fun setFilter(filter: UserRoleFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, errorMessage = null, successMessage = null) }
    }

    fun closeAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun openEditDialog(user: UserProfile) {
        _uiState.update { it.copy(userToEdit = user, errorMessage = null, successMessage = null) }
    }

    fun closeEditDialog() {
        _uiState.update { it.copy(userToEdit = null) }
    }

    fun promptDeactivate(user: UserProfile) {
        _uiState.update { it.copy(userToDeactivate = user, errorMessage = null, successMessage = null) }
    }

    fun dismissDeactivatePrompt() {
        _uiState.update { it.copy(userToDeactivate = null) }
    }

    fun createUser(
        fullName: String,
        email: String,
        password: String,
        role: UserRole,
        isActive: Boolean
    ) {
        _uiState.update { it.copy(isActionLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = adminRepository.createUser(
                fullNameInput = fullName,
                emailInput = email,
                passwordInput = password,
                role = role,
                isActive = isActive
            )
            result.onSuccess { createdUser ->
                val updatedList = listOf(createdUser) + _uiState.value.users.filter { it.id != createdUser.id }
                _uiState.update {
                    it.copy(
                        users = updatedList,
                        isActionLoading = false,
                        showAddDialog = false,
                        successMessage = "${createdUser.userRole.displayName} '${createdUser.fullName}' created successfully."
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to create user."
                    )
                }
            }
        }
    }

    fun updateUser(
        userId: String,
        fullName: String,
        role: UserRole,
        isActive: Boolean
    ) {
        _uiState.update { it.copy(isActionLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = adminRepository.updateUser(
                userId = userId,
                fullNameInput = fullName,
                role = role,
                isActive = isActive
            )
            result.onSuccess { updatedUser ->
                val updatedList = _uiState.value.users.map { if (it.id == updatedUser.id) updatedUser else it }
                _uiState.update {
                    it.copy(
                        users = updatedList,
                        isActionLoading = false,
                        userToEdit = null,
                        successMessage = "User '${updatedUser.fullName}' updated successfully."
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to update user."
                    )
                }
            }
        }
    }

    fun confirmDeactivate() {
        val target = _uiState.value.userToDeactivate ?: return
        _uiState.update { it.copy(isActionLoading = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val result = adminRepository.deactivateUser(target.id)
            result.onSuccess { deactivatedUser ->
                val updatedList = _uiState.value.users.map { if (it.id == deactivatedUser.id) deactivatedUser else it }
                _uiState.update {
                    it.copy(
                        users = updatedList,
                        isActionLoading = false,
                        userToDeactivate = null,
                        successMessage = "User '${deactivatedUser.fullName}' has been deactivated."
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        userToDeactivate = null,
                        errorMessage = error.localizedMessage ?: "Failed to deactivate user."
                    )
                }
            }
        }
    }

    fun reactivateUser(user: UserProfile) {
        _uiState.update { it.copy(isActionLoading = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val result = adminRepository.reactivateUser(user.id)
            result.onSuccess { reactivatedUser ->
                val updatedList = _uiState.value.users.map { if (it.id == reactivatedUser.id) reactivatedUser else it }
                _uiState.update {
                    it.copy(
                        users = updatedList,
                        isActionLoading = false,
                        successMessage = "User '${reactivatedUser.fullName}' has been reactivated."
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to reactivate user."
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
