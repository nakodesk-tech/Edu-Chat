package com.example.ui.schooladmin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.chat.ChatGroupViewModel
import com.example.ui.chat.ChatsTabContent
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.School
import com.example.data.model.UserProfile
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentAmberContainer
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.OnAccentAmberContainer
import com.example.ui.theme.OnPrimaryIndigoContainer
import com.example.ui.theme.OnSecondaryGreenContainer
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoContainer
import com.example.ui.theme.SecondaryGreen
import com.example.ui.theme.SecondaryGreenContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolAdminDashboardScreen(
    viewModel: SchoolAdminDashboardViewModel,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val chatViewModel: ChatGroupViewModel = viewModel()

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryIndigoContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Edu Chat",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = uiState.assignedSchool?.name ?: "शाळा प्रशासक डॅशबोर्ड",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("school_admin_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "बाहेर पडा (Log Out)",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                // Tab 1: चॅट्स
                NavigationBarItem(
                    selected = uiState.selectedTab == SchoolAdminTab.CHATS,
                    onClick = { viewModel.selectTab(SchoolAdminTab.CHATS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "चॅट्स",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = SchoolAdminTab.CHATS.marathiTitle,
                            fontWeight = if (uiState.selectedTab == SchoolAdminTab.CHATS) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryIndigo,
                        selectedTextColor = PrimaryIndigo,
                        indicatorColor = PrimaryIndigoContainer,
                        unselectedIconColor = TextTertiary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("tab_chats")
                )

                // Tab 2: शिक्षक
                NavigationBarItem(
                    selected = uiState.selectedTab == SchoolAdminTab.TEACHERS,
                    onClick = { viewModel.selectTab(SchoolAdminTab.TEACHERS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = "शिक्षक",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = SchoolAdminTab.TEACHERS.marathiTitle,
                            fontWeight = if (uiState.selectedTab == SchoolAdminTab.TEACHERS) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryIndigo,
                        selectedTextColor = PrimaryIndigo,
                        indicatorColor = PrimaryIndigoContainer,
                        unselectedIconColor = TextTertiary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("tab_teachers")
                )

                // Tab 3: माझी शाळा
                NavigationBarItem(
                    selected = uiState.selectedTab == SchoolAdminTab.MY_SCHOOL,
                    onClick = { viewModel.selectTab(SchoolAdminTab.MY_SCHOOL) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Apartment,
                            contentDescription = "माझी शाळा",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = SchoolAdminTab.MY_SCHOOL.marathiTitle,
                            fontWeight = if (uiState.selectedTab == SchoolAdminTab.MY_SCHOOL) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryIndigo,
                        selectedTextColor = PrimaryIndigo,
                        indicatorColor = PrimaryIndigoContainer,
                        unselectedIconColor = TextTertiary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("tab_my_school")
                )

                // Tab 4: माझे प्रोफाइल
                NavigationBarItem(
                    selected = uiState.selectedTab == SchoolAdminTab.MY_PROFILE,
                    onClick = { viewModel.selectTab(SchoolAdminTab.MY_PROFILE) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "माझे प्रोफाइल",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = SchoolAdminTab.MY_PROFILE.marathiTitle,
                            fontWeight = if (uiState.selectedTab == SchoolAdminTab.MY_PROFILE) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryIndigo,
                        selectedTextColor = PrimaryIndigo,
                        indicatorColor = PrimaryIndigoContainer,
                        unselectedIconColor = TextTertiary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("tab_my_profile")
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                SchoolAdminTab.CHATS -> {
                    ChatsTabContent(viewModel = chatViewModel)
                }
                SchoolAdminTab.TEACHERS -> {
                    TeachersTabContent(
                        uiState = uiState,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onAddTeacherClick = { viewModel.openDialog(SchoolAdminDialog.CreateTeacher) },
                        onEditTeacher = { viewModel.openDialog(SchoolAdminDialog.EditTeacher(it)) },
                        onDeactivateTeacher = { viewModel.openDialog(SchoolAdminDialog.ConfirmDeactivateTeacher(it)) },
                        onReactivateTeacher = { viewModel.toggleTeacherStatus(it, true) },
                        onRefresh = { viewModel.loadTeachers() }
                    )
                }
                SchoolAdminTab.MY_SCHOOL -> {
                    MySchoolTabContent(
                        school = uiState.assignedSchool,
                        isLoading = uiState.isLoading
                    )
                }
                SchoolAdminTab.MY_PROFILE -> {
                    MyProfileTabContent(
                        profile = uiState.profile,
                        school = uiState.assignedSchool,
                        onEditProfile = { viewModel.openDialog(SchoolAdminDialog.EditProfile) },
                        onLogout = onLogout
                    )
                }
            }

            // Dialogs
            when (val dialog = uiState.currentDialog) {
                is SchoolAdminDialog.CreateTeacher -> {
                    CreateTeacherDialog(
                        schoolName = uiState.assignedSchool?.name ?: "शाळा",
                        isActionLoading = uiState.isActionLoading,
                        errorMessage = uiState.errorMessage,
                        onDismiss = { viewModel.closeDialog() },
                        onSubmit = { name, email, mobile, password ->
                            viewModel.createTeacher(name, email, mobile, password) {}
                        }
                    )
                }
                is SchoolAdminDialog.EditTeacher -> {
                    EditTeacherDialog(
                        teacher = dialog.teacher,
                        isActionLoading = uiState.isActionLoading,
                        errorMessage = uiState.errorMessage,
                        onDismiss = { viewModel.closeDialog() },
                        onSubmit = { name, mobile, isActive ->
                            viewModel.updateTeacher(dialog.teacher.id, name, mobile, isActive) {}
                        }
                    )
                }
                is SchoolAdminDialog.ConfirmDeactivateTeacher -> {
                    ConfirmDeactivateTeacherDialog(
                        teacher = dialog.teacher,
                        isActionLoading = uiState.isActionLoading,
                        onDismiss = { viewModel.closeDialog() },
                        onConfirm = {
                            viewModel.toggleTeacherStatus(dialog.teacher, false)
                        }
                    )
                }
                is SchoolAdminDialog.EditProfile -> {
                    EditProfileDialog(
                        profile = uiState.profile,
                        isActionLoading = uiState.isActionLoading,
                        errorMessage = uiState.errorMessage,
                        onDismiss = { viewModel.closeDialog() },
                        onSubmit = { name, mobile ->
                            viewModel.updateProfile(name, mobile) {}
                        }
                    )
                }
                is SchoolAdminDialog.None -> {}
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 1: शिक्षक (Teacher Management)
// -------------------------------------------------------------------------------------------------

@Composable
private fun TeachersTabContent(
    uiState: SchoolAdminUiState,
    onSearchChange: (String) -> Unit,
    onAddTeacherClick: () -> Unit,
    onEditTeacher: (UserProfile) -> Unit,
    onDeactivateTeacher: (UserProfile) -> Unit,
    onReactivateTeacher: (UserProfile) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header with Title & Action Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "शिक्षक",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "एकूण शिक्षक: ${uiState.teachers.size}",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Button(
                onClick = onAddTeacherClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryIndigo,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("btn_add_teacher")
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "+ शिक्षक नोंदणी",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("शिक्षक शोधा (नाव, ईमेल, मोबाईल)...", fontSize = 13.sp, color = TextTertiary) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = PrimaryIndigo,
                unfocusedBorderColor = BorderSubtle
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_search_teacher")
        )

        // Teacher List
        val teachers = uiState.filteredTeachers
        if (teachers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PrimaryIndigoContainer,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Text(
                        text = if (uiState.searchQuery.isNotEmpty()) "शोध परिणामात शिक्षक आढळले नाहीत." else "शाळेमध्ये अद्याप कोणतेही शिक्षक नोंदणीकृत नाहीत.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    if (uiState.searchQuery.isEmpty()) {
                        OutlinedButton(
                            onClick = onAddTeacherClick,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, PrimaryIndigo)
                        ) {
                            Text("+ पहिले शिक्षक नोंदवा", color = PrimaryIndigo, fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("teachers_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(teachers, key = { it.id }) { teacher ->
                    TeacherItemCard(
                        teacher = teacher,
                        onEdit = { onEditTeacher(teacher) },
                        onDeactivate = { onDeactivateTeacher(teacher) },
                        onReactivate = { onReactivateTeacher(teacher) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TeacherItemCard(
    teacher: UserProfile,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onReactivate: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("teacher_card_${teacher.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Avatar + Name + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (teacher.isActive) PrimaryIndigoContainer else Color(0xFFF3F4F6),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = if (teacher.isActive) PrimaryIndigo else TextTertiary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = teacher.fullName ?: "शिक्षक",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "भूमिका: शिक्षक (Teacher)",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                // Active / Inactive Status Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (teacher.isActive) SecondaryGreenContainer else Color(0xFFFEE2E2),
                    border = BorderStroke(
                        1.dp,
                        if (teacher.isActive) SecondaryGreen.copy(alpha = 0.3f) else Color(0xFFEF4444).copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (teacher.isActive) "सक्रिय (Active)" else "निष्क्रिय (Inactive)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (teacher.isActive) OnSecondaryGreenContainer else Color(0xFF991B1B),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.6f))

            // Contact Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Email
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = teacher.email ?: "-",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Mobile
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = teacher.mobile ?: "नाही",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit Button
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("btn_edit_teacher_${teacher.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "संपादित करा", fontSize = 12.sp, color = PrimaryIndigo)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Toggle Status Button
                if (teacher.isActive) {
                    OutlinedButton(
                        onClick = onDeactivate,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_deactivate_teacher_${teacher.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Deactivate",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "निष्क्रिय करा", fontSize = 12.sp, color = Color(0xFFDC2626))
                    }
                } else {
                    Button(
                        onClick = onReactivate,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_reactivate_teacher_${teacher.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Reactivate",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "पुन्हा सक्रिय करा", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 2: विद्यार्थी (Reserved Placeholder)
// -------------------------------------------------------------------------------------------------

@Composable
private fun StudentsPlaceholderTabContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = PrimaryIndigoContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "विद्यार्थी व्यवस्थापन",
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Text(
                    text = "विद्यार्थी व्यवस्थापन",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentAmberContainer,
                    border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "हे फीचर पुढील टप्प्यात उपलब्ध होईल.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnAccentAmberContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }

                Text(
                    text = "पुढील टप्प्यामध्ये शिक्षक (Teacher) स्वतःच्या वर्गातील विद्यार्थ्यांना नोंदणीकृत आणि व्यवस्थापित करू शकतील. शाळा प्रशासकास विद्यार्थ्यांची उपस्थिती आणि अहवाल पाहण्याची सुविधा दिली जाईल.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 3: माझी शाळा (Assigned School Information)
// -------------------------------------------------------------------------------------------------

@Composable
private fun MySchoolTabContent(
    school: School?,
    isLoading: Boolean
) {
    if (isLoading && school == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryIndigo)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "माझी शाळा",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        if (school != null) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // School Name & Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryIndigoContainer,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Apartment,
                                    contentDescription = null,
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = school.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (school.isActive) SecondaryGreenContainer else Color(0xFFFEE2E2),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = if (school.isActive) "सक्रिय शाळा (Active)" else "निष्क्रिय शाळा (Inactive)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (school.isActive) OnSecondaryGreenContainer else Color(0xFF991B1B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = BorderSubtle)

                    // School Code
                    SchoolDetailRow(
                        icon = Icons.Default.QrCode,
                        label = "शाळा कोड (School Code)",
                        value = school.code
                    )

                    // Address
                    SchoolDetailRow(
                        icon = Icons.Default.LocationOn,
                        label = "पत्ता (Address)",
                        value = school.address ?: "पत्ता उपलब्ध नाही"
                    )

                    // Read-only Officer Admin notice
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimaryIndigoContainer.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "शाळेची माहिती फक्त शिक्षण अधिकारी (Officer Admin) द्वारे अद्यतनित केली जाऊ शकते.",
                                fontSize = 12.sp,
                                color = PrimaryIndigo
                            )
                        }
                    }
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "शाळेची माहिती उपलब्ध नाही किंवा असाइन केलेली शाळा आढळली नाही.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SchoolDetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Column {
            Text(text = label, fontSize = 12.sp, color = TextSecondary)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 4: माझे प्रोफाइल (School Admin Profile & Settings)
// -------------------------------------------------------------------------------------------------

@Composable
private fun MyProfileTabContent(
    profile: UserProfile?,
    school: School?,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "माझे प्रोफाइल",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar + Name + Role Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PrimaryIndigoContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile?.fullName ?: "शाळा प्रशासक",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AccentAmberContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "SCHOOL ADMIN (शाळा प्रशासक)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnAccentAmberContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                // Details List
                ProfileDetailRow(
                    icon = Icons.Default.Email,
                    label = "ईमेल पत्ता",
                    value = profile?.email ?: "-"
                )

                ProfileDetailRow(
                    icon = Icons.Default.Phone,
                    label = "मोबाईल नंबर",
                    value = profile?.mobile ?: "नोंदणीकृत नाही"
                )

                ProfileDetailRow(
                    icon = Icons.Default.Apartment,
                    label = "असाइन केलेली शाळा",
                    value = school?.name ?: "शाळा लोड होत आहे..."
                )

                ProfileDetailRow(
                    icon = Icons.Default.CheckCircle,
                    label = "खाते स्थिती",
                    value = if (profile?.isActive == true) "सक्रिय (Active)" else "निष्क्रिय (Inactive)"
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Edit Profile Button
                Button(
                    onClick = onEditProfile,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_edit_profile")
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "माहिती अद्यतनित करा", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                // Logout Button
                OutlinedButton(
                    onClick = onLogout,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_profile_logout")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "बाहेर पडा (Log Out)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileDetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text(text = label, fontSize = 11.sp, color = TextSecondary)
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
    }
}

// -------------------------------------------------------------------------------------------------
// DIALOGS
// -------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTeacherDialog(
    schoolName: String,
    isActionLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (name: String, email: String, mobile: String, password: String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isActionLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "शिक्षक नोंदणी",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = schoolName,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !isActionLoading) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                if (!errorMessage.isNullOrBlank()) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Auto-fixed Info Badges (Role = Teacher, School = Current School)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryIndigoContainer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(14.dp))
                            Text("भूमिका: शिक्षक", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OnPrimaryIndigoContainer)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SecondaryGreenContainer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.School, contentDescription = null, tint = SecondaryGreen, modifier = Modifier.size(14.dp))
                            Text("शाळा: स्वतःची शाळा", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OnSecondaryGreenContainer)
                        }
                    }
                }

                // Full Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("शिक्षकांचे पूर्ण नाव *") },
                    placeholder = { Text("उदा. प्रा. रमेश पाटील") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_create_teacher_name")
                )

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("ईमेल पत्ता (Login ID) *") },
                    placeholder = { Text("teacher@school.edu") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_create_teacher_email")
                )

                // Mobile
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("मोबाईल नंबर (पर्यायी)") },
                    placeholder = { Text("९८XXXXXXXX") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_create_teacher_mobile")
                )

                // Initial Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("प्रारंभिक पासवर्ड * (किमान ६ अक्षरे)") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "लपवा" else "दाखवा"
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_create_teacher_password")
                )

                // Submit Button
                Button(
                    onClick = { onSubmit(fullName, email, mobile, password) },
                    enabled = !isActionLoading && fullName.isNotBlank() && email.isNotBlank() && password.length >= 6,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_submit_create_teacher")
                ) {
                    if (isActionLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("नोंदणी पूर्ण करा", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTeacherDialog(
    teacher: UserProfile,
    isActionLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (name: String, mobile: String, isActive: Boolean) -> Unit
) {
    var fullName by remember { mutableStateOf(teacher.fullName ?: "") }
    var mobile by remember { mutableStateOf(teacher.mobile ?: "") }
    var isActive by remember { mutableStateOf(teacher.isActive) }

    Dialog(
        onDismissRequest = { if (!isActionLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "शिक्षक माहिती संपादन",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss, enabled = !isActionLoading) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                if (!errorMessage.isNullOrBlank()) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Email (Read-only)
                OutlinedTextField(
                    value = teacher.email ?: "",
                    onValueChange = {},
                    label = { Text("ईमेल पत्ता (अपरिवर्तनीय)") },
                    enabled = false,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledContainerColor = Color(0xFFF9FAFB),
                        disabledTextColor = TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Full Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("शिक्षकांचे पूर्ण नाव *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_teacher_name")
                )

                // Mobile
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("मोबाईल नंबर") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_teacher_mobile")
                )

                // Status Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9FAFB), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isActive) "खाते स्थिती: सक्रिय" else "खाते स्थिती: निष्क्रिय",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isActive) "शिक्षक लॉगिन करू शकतात" else "शिक्षक लॉगिन करू शकत नाहीत",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SecondaryGreen
                        ),
                        modifier = Modifier.testTag("switch_teacher_active")
                    )
                }

                // Submit Button
                Button(
                    onClick = { onSubmit(fullName, mobile, isActive) },
                    enabled = !isActionLoading && fullName.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_submit_edit_teacher")
                ) {
                    if (isActionLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("बदल जतन करा", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeactivateTeacherDialog(
    teacher: UserProfile,
    isActionLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isActionLoading) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "शिक्षक निष्क्रिय करा",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "आपण हा शिक्षक निष्क्रिय करू इच्छिता? (${teacher.fullName})\n\nनिष्क्रिय केल्यावर हे शिक्षक ॲपमध्ये लॉगिन करू शकणार नाहीत.",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isActionLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_confirm_deactivate_teacher")
            ) {
                if (isActionLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Text("होय, निष्क्रिय करा", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isActionLoading,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("रद्द करा")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    profile: UserProfile?,
    isActionLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (name: String, mobile: String) -> Unit
) {
    var fullName by remember { mutableStateOf(profile?.fullName ?: "") }
    var mobile by remember { mutableStateOf(profile?.mobile ?: "") }

    Dialog(
        onDismissRequest = { if (!isActionLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "प्रोफाइल माहिती संपादन",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss, enabled = !isActionLoading) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                if (!errorMessage.isNullOrBlank()) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Full Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("पूर्ण नाव *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_profile_name")
                )

                // Mobile
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("मोबाईल नंबर") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_profile_mobile")
                )

                // Submit Button
                Button(
                    onClick = { onSubmit(fullName, mobile) },
                    enabled = !isActionLoading && fullName.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_submit_edit_profile")
                ) {
                    if (isActionLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("बदल जतन करा", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
