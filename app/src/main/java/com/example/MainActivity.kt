package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserRole
import com.example.ui.admin.OfficerAdminDashboardScreen
import com.example.ui.admin.OfficerAdminDashboardViewModel
import com.example.ui.auth.AuthUiState
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.RolePlaceholderScreen
import com.example.ui.schooladmin.SchoolAdminDashboardScreen
import com.example.ui.schooladmin.SchoolAdminDashboardViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel = authViewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: AuthViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Crossfade(targetState = uiState, label = "authNavigation") { state ->
        when (state) {
            is AuthUiState.Authenticated -> {
                val profile = state.session.profile
                if (profile.userRole == UserRole.OFFICER_ADMIN && profile.isActive) {
                    val officerViewModel: OfficerAdminDashboardViewModel = viewModel()
                    OfficerAdminDashboardScreen(
                        viewModel = officerViewModel,
                        onLogout = { viewModel.logout() }
                    )
                } else if (profile.userRole == UserRole.SCHOOL_ADMIN && profile.isActive && !profile.schoolId.isNullOrBlank()) {
                    val schoolAdminViewModel: SchoolAdminDashboardViewModel = viewModel()
                    SchoolAdminDashboardScreen(
                        viewModel = schoolAdminViewModel,
                        onLogout = { viewModel.logout() }
                    )
                } else {
                    RolePlaceholderScreen(
                        session = state.session,
                        onLogout = { viewModel.logout() }
                    )
                }
            }
            else -> {
                LoginScreen(viewModel = viewModel)
            }
        }
    }
}

