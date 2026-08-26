package com.example.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuthSession
import com.example.data.model.UserRole
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Authenticated(val session: AuthSession) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val selectedRole: UserRole = UserRole.TEACHER,
    val emailError: String? = null,
    val passwordError: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(application.applicationContext)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(LoginFormState())
    val formState: StateFlow<LoginFormState> = _formState.asStateFlow()

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        val existingSession = repository.getActiveSession()
        if (existingSession != null && existingSession.profile.isActive) {
            _uiState.value = AuthUiState.Authenticated(existingSession)
        }
    }

    fun onEmailChanged(newEmail: String) {
        _formState.update {
            it.copy(
                email = newEmail,
                emailError = null
            )
        }
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    fun onPasswordChanged(newPassword: String) {
        _formState.update {
            it.copy(
                password = newPassword,
                passwordError = null
            )
        }
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    fun togglePasswordVisibility() {
        _formState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onRoleSelected(role: UserRole) {
        _formState.update { it.copy(selectedRole = role) }
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    fun fillDemoCredentials(email: String, pass: String, role: UserRole) {
        _formState.update {
            it.copy(
                email = email,
                password = pass,
                selectedRole = role,
                emailError = null,
                passwordError = null
            )
        }
        _uiState.value = AuthUiState.Idle
    }

    fun login() {
        val form = _formState.value
        var hasError = false

        if (form.email.isBlank()) {
            _formState.update { it.copy(emailError = "Email address is required.") }
            hasError = true
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(form.email.trim()).matches() && !form.email.contains("@")) {
            _formState.update { it.copy(emailError = "Please enter a valid email address.") }
            hasError = true
        }

        if (form.password.isBlank()) {
            _formState.update { it.copy(passwordError = "Password is required.") }
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = repository.login(form.email, form.password, form.selectedRole)) {
                is AuthResult.Success -> {
                    _uiState.value = AuthUiState.Authenticated(result.session)
                }
                is AuthResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = AuthUiState.Idle
        }
    }

    fun clearError() {
        _uiState.value = AuthUiState.Idle
    }
}
