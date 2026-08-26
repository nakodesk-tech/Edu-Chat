package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.data.remote.SupabaseConfig
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentAmberContainer
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoContainer
import com.example.ui.theme.SecondaryGreen
import com.example.ui.theme.SecondaryGreenContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: AuthViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    var showConfigDialog by remember { mutableStateOf(false) }

    val isLoading = uiState is AuthUiState.Loading

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 520.dp)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Configuration Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { showConfigDialog = true },
                        modifier = Modifier.testTag("settings_config_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Supabase Connection Settings",
                            tint = TextTertiary
                        )
                    }
                }

                // Application Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(22.dp),
                                spotColor = PrimaryIndigo.copy(alpha = 0.35f),
                                ambientColor = PrimaryIndigo.copy(alpha = 0.2f)
                            )
                            .background(PrimaryIndigo, RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Edu Chat Logo",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Edu Chat",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "CLASSROOM EXCELLENCE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryIndigo,
                        letterSpacing = 1.2.sp
                    )
                }

                // Role Selection Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. Select Role to Access",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val displayRoles = listOf(UserRole.OFFICER_ADMIN, UserRole.TEACHER, UserRole.STUDENT)
                        displayRoles.forEach { role ->
                            RoleChipButton(
                                role = role,
                                isSelected = formState.selectedRole == role,
                                onSelect = { viewModel.onRoleSelected(role) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Credentials Form Section
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "2. Enter Credentials",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        // Email Field
                        Column {
                            OutlinedTextField(
                                value = formState.email,
                                onValueChange = { viewModel.onEmailChanged(it) },
                                label = { Text("Email Address") },
                                placeholder = { Text("e.g., teacher@educhat.edu") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = if (formState.emailError != null) MaterialTheme.colorScheme.error else TextSecondary
                                    )
                                },
                                singleLine = true,
                                isError = formState.emailError != null,
                                enabled = !isLoading,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryIndigo,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("email_input")
                            )

                            if (formState.emailError != null) {
                                Text(
                                    text = formState.emailError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                )
                            }
                        }

                        // Password Field
                        Column {
                            OutlinedTextField(
                                value = formState.password,
                                onValueChange = { viewModel.onPasswordChanged(it) },
                                label = { Text("Password") },
                                placeholder = { Text("Enter your password") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (formState.passwordError != null) MaterialTheme.colorScheme.error else TextSecondary
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { viewModel.togglePasswordVisibility() },
                                        modifier = Modifier.testTag("password_toggle_button")
                                    ) {
                                        Icon(
                                            imageVector = if (formState.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (formState.isPasswordVisible) "Hide password" else "Show password",
                                            tint = TextSecondary
                                        )
                                    }
                                },
                                singleLine = true,
                                isError = formState.passwordError != null,
                                enabled = !isLoading,
                                visualTransformation = if (formState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        viewModel.login()
                                    }
                                ),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryIndigo,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("password_input")
                            )

                            if (formState.passwordError != null) {
                                Text(
                                    text = formState.passwordError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                )
                            }
                        }

                        // Error Banner (Role mismatch, Inactive account, or Auth failure)
                        AnimatedVisibility(
                            visible = uiState is AuthUiState.Error,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            if (uiState is AuthUiState.Error) {
                                val errorMessage = (uiState as AuthUiState.Error).message
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFFEF2F2),
                                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = errorMessage,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFB91C1C),
                                            lineHeight = 18.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // Login Button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.login()
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryIndigo,
                                contentColor = Color.White,
                                disabledContainerColor = PrimaryIndigo.copy(alpha = 0.5f),
                                disabledContentColor = Color.White.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(
                                    elevation = if (!isLoading) 4.dp else 0.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    spotColor = PrimaryIndigo.copy(alpha = 0.3f)
                                )
                                .testTag("login_submit_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "AUTHENTICATING...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                            } else {
                                Text(
                                    text = "LOG IN AS ${formState.selectedRole.displayName.uppercase()}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                            }
                        }
                    }
                }

                // Demo Accounts Fast-Fill Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Test Accounts (Tap to populate)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DemoAccountChip(
                            title = "Teacher",
                            color = PrimaryIndigo,
                            containerColor = PrimaryIndigoContainer,
                            onClick = {
                                viewModel.fillDemoCredentials(
                                    email = "teacher@educhat.edu",
                                    pass = "password123",
                                    role = UserRole.TEACHER
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        DemoAccountChip(
                            title = "Student",
                            color = SecondaryGreen,
                            containerColor = SecondaryGreenContainer,
                            onClick = {
                                viewModel.fillDemoCredentials(
                                    email = "student@educhat.edu",
                                    pass = "password123",
                                    role = UserRole.STUDENT
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        DemoAccountChip(
                            title = "Admin",
                            color = AccentAmber,
                            containerColor = AccentAmberContainer,
                            onClick = {
                                viewModel.fillDemoCredentials(
                                    email = "admin@educhat.edu",
                                    pass = "password123",
                                    role = UserRole.OFFICER_ADMIN
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DemoAccountChip(
                            title = "Role Mismatch Test",
                            color = Color(0xFF6B7280),
                            containerColor = Color(0xFFF3F4F6),
                            onClick = {
                                viewModel.fillDemoCredentials(
                                    email = "teacher@educhat.edu",
                                    pass = "password123",
                                    role = UserRole.STUDENT // Mismatched on purpose!
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        DemoAccountChip(
                            title = "Inactive Account Test",
                            color = Color(0xFFDC2626),
                            containerColor = Color(0xFFFEF2F2),
                            onClick = {
                                viewModel.fillDemoCredentials(
                                    email = "inactive@educhat.edu",
                                    pass = "password123",
                                    role = UserRole.STUDENT
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    HorizontalDivider(
                        modifier = Modifier.width(32.dp),
                        thickness = 1.dp,
                        color = BorderSubtle
                    )
                    Text(
                        text = "V 1.0.0 (BETA)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextTertiary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.width(32.dp),
                        thickness = 1.dp,
                        color = BorderSubtle
                    )
                }
            }
        }
    }

    // Supabase Connection Settings Dialog
    if (showConfigDialog) {
        SupabaseConfigDialog(
            onDismiss = { showConfigDialog = false },
            onSave = { url, key ->
                SupabaseConfig.saveCustomConfig(context, url, key)
                showConfigDialog = false
            }
        )
    }
}

@Composable
private fun RoleChipButton(
    role: UserRole,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val roleColor = when (role) {
        UserRole.TEACHER -> PrimaryIndigo
        UserRole.STUDENT -> SecondaryGreen
        UserRole.OFFICER_ADMIN, UserRole.SCHOOL_ADMIN -> AccentAmber
    }

    val roleBg = when (role) {
        UserRole.TEACHER -> PrimaryIndigoContainer
        UserRole.STUDENT -> SecondaryGreenContainer
        UserRole.OFFICER_ADMIN, UserRole.SCHOOL_ADMIN -> AccentAmberContainer
    }

    val roleIcon = when (role) {
        UserRole.TEACHER -> Icons.Default.MenuBook
        UserRole.STUDENT -> Icons.Default.People
        UserRole.OFFICER_ADMIN, UserRole.SCHOOL_ADMIN -> Icons.Default.AdminPanelSettings
    }

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) roleColor else BorderSubtle,
        animationSpec = tween(150),
        label = "chipBorder"
    )

    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) roleBg else Color.White,
        animationSpec = tween(150),
        label = "chipBg"
    )

    val tag = if (role == UserRole.OFFICER_ADMIN) "role_chip_officer_admin" else "role_chip_${role.dbValue}"

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(18.dp),
        color = animatedBg,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, animatedBorderColor),
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier
            .height(72.dp)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = roleIcon,
                contentDescription = role.displayName,
                tint = if (isSelected) roleColor else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = role.displayName,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) roleColor else TextPrimary
            )
        }
    }
}

@Composable
private fun DemoAccountChip(
    title: String,
    color: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier.height(38.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SupabaseConfigDialog(
    onDismiss: () -> Unit,
    onSave: (url: String, key: String) -> Unit
) {
    val context = LocalContext.current
    var url by remember { mutableStateOf(SupabaseConfig.getSupabaseUrl(context)) }
    var anonKey by remember { mutableStateOf(SupabaseConfig.getSupabaseAnonKey(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Supabase Backend Configuration",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Configure your Supabase project URL and anon public key below or via the AI Studio Secrets panel.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Supabase URL") },
                    placeholder = { Text("https://your-ref.supabase.co") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = anonKey,
                    onValueChange = { anonKey = it },
                    label = { Text("Supabase Anon Key") },
                    placeholder = { Text("eyJhbGciOi...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(url, anonKey) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
