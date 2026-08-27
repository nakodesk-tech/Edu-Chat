package com.example.ui.admin

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.chat.ChatGroupViewModel
import com.example.ui.chat.ChatsTabContent
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

@Composable
fun OfficerAdminDashboardScreen(
    viewModel: OfficerAdminDashboardViewModel,
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                // Tab 1: चॅट्स
                NavigationBarItem(
                    selected = uiState.selectedTab == OfficerAdminTab.CHATS,
                    onClick = { viewModel.selectTab(OfficerAdminTab.CHATS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "चॅट्स",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = OfficerAdminTab.CHATS.marathiTitle,
                            fontWeight = if (uiState.selectedTab == OfficerAdminTab.CHATS) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentAmber,
                        selectedTextColor = AccentAmber,
                        indicatorColor = AccentAmberContainer,
                        unselectedIconColor = TextTertiary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("tab_chats")
                )

                // Tab 2: वापरकर्ते
                NavigationBarItem(
                    selected = uiState.selectedTab == OfficerAdminTab.USERS,
                    onClick = { viewModel.selectTab(OfficerAdminTab.USERS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.SupervisorAccount,
                            contentDescription = "वापरकर्ते",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = OfficerAdminTab.USERS.marathiTitle,
                            fontWeight = if (uiState.selectedTab == OfficerAdminTab.USERS) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentAmber,
                        selectedTextColor = AccentAmber,
                        indicatorColor = AccentAmberContainer,
                        unselectedIconColor = TextTertiary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("tab_users")
                )

                // Tab 3: शाळा
                NavigationBarItem(
                    selected = uiState.selectedTab == OfficerAdminTab.SCHOOLS,
                    onClick = { viewModel.selectTab(OfficerAdminTab.SCHOOLS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "शाळा",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = OfficerAdminTab.SCHOOLS.marathiTitle,
                            fontWeight = if (uiState.selectedTab == OfficerAdminTab.SCHOOLS) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentAmber,
                        selectedTextColor = AccentAmber,
                        indicatorColor = AccentAmberContainer,
                        unselectedIconColor = TextTertiary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("tab_schools")
                )

                // Tab 4: माझे प्रोफाइल
                NavigationBarItem(
                    selected = uiState.selectedTab == OfficerAdminTab.MY_PROFILE,
                    onClick = { viewModel.selectTab(OfficerAdminTab.MY_PROFILE) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "माझे प्रोफाइल",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = OfficerAdminTab.MY_PROFILE.marathiTitle,
                            fontWeight = if (uiState.selectedTab == OfficerAdminTab.MY_PROFILE) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentAmber,
                        selectedTextColor = AccentAmber,
                        indicatorColor = AccentAmberContainer,
                        unselectedIconColor = TextTertiary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("tab_my_profile")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryIndigo)
                }
            } else {
                when (uiState.selectedTab) {
                    OfficerAdminTab.CHATS -> {
                        ChatsTabContent(viewModel = chatViewModel)
                    }
                    OfficerAdminTab.USERS -> {
                        OfficerUsersTabContent(
                            profile = uiState.profile,
                            onOpenDialog = { viewModel.openDialog(it) }
                        )
                    }
                    OfficerAdminTab.SCHOOLS -> {
                        OfficerSchoolsTabContent(
                            schools = uiState.schools,
                            onOpenDialog = { viewModel.openDialog(it) }
                        )
                    }
                    OfficerAdminTab.MY_PROFILE -> {
                        OfficerProfileTabContent(
                            profile = uiState.profile,
                            onEditProfile = { viewModel.openDialog(OfficerAdminDialog.MyProfile) },
                            onLogout = onLogout
                        )
                    }
                }
            }
        }

        // Active Dialog Dispatcher
        when (val dialog = uiState.currentDialog) {
            is OfficerAdminDialog.CreateOfficerAdmin -> {
                CreateOfficerAdminDialog(
                    isLoading = uiState.isActionLoading,
                    errorMessage = uiState.errorMessage,
                    onDismiss = { viewModel.closeDialog() },
                    onConfirm = { name, email, mobile, pass ->
                        viewModel.createOfficerAdmin(name, email, mobile, pass) { }
                    }
                )
            }
            is OfficerAdminDialog.CreateSchoolAdmin -> {
                CreateSchoolAdminDialog(
                    activeSchools = uiState.activeSchools,
                    isLoading = uiState.isActionLoading,
                    errorMessage = uiState.errorMessage,
                    onDismiss = { viewModel.closeDialog() },
                    onConfirm = { name, email, mobile, pass, schoolId ->
                        viewModel.createSchoolAdmin(name, email, mobile, pass, schoolId) { }
                    }
                )
            }
            is OfficerAdminDialog.CreateSchool -> {
                CreateSchoolDialog(
                    isLoading = uiState.isActionLoading,
                    errorMessage = uiState.errorMessage,
                    onDismiss = { viewModel.closeDialog() },
                    onConfirm = { name, code, address ->
                        viewModel.createSchool(name, code, address) { }
                    }
                )
            }
            is OfficerAdminDialog.ManageSchools -> {
                ManageSchoolsDialog(
                    schools = uiState.schools,
                    isLoading = uiState.isActionLoading,
                    onDismiss = { viewModel.closeDialog() },
                    onEditSchool = { school -> viewModel.openDialog(OfficerAdminDialog.EditSchool(school)) },
                    onToggleStatus = { school -> viewModel.requestToggleSchoolStatus(school) }
                )
            }
            is OfficerAdminDialog.EditSchool -> {
                EditSchoolDialog(
                    school = dialog.school,
                    isLoading = uiState.isActionLoading,
                    errorMessage = uiState.errorMessage,
                    onDismiss = { viewModel.openDialog(OfficerAdminDialog.ManageSchools) },
                    onConfirm = { name, code, address, isActive ->
                        viewModel.updateSchool(dialog.school.id, name, code, address, isActive) { }
                    }
                )
            }
            is OfficerAdminDialog.ConfirmDeactivateSchool -> {
                ConfirmDeactivateSchoolDialog(
                    school = dialog.school,
                    staffCount = dialog.staffCount,
                    isLoading = uiState.isActionLoading,
                    onDismiss = { viewModel.openDialog(OfficerAdminDialog.ManageSchools) },
                    onConfirm = {
                        viewModel.executeToggleSchoolStatus(dialog.school.id, false)
                    }
                )
            }
            is OfficerAdminDialog.MyProfile -> {
                OfficerProfileDialog(
                    profile = uiState.profile,
                    isLoading = uiState.isActionLoading,
                    errorMessage = uiState.errorMessage,
                    onDismiss = { viewModel.closeDialog() },
                    onSave = { name, mobile ->
                        viewModel.updateProfile(name, mobile) { }
                    }
                )
            }
            OfficerAdminDialog.None -> { /* No dialog active */ }
        }
    }
}

/**
 * Compact Top Area: Officer Admin Full Name, Email, Mobile, Role Badge & Compact Logout
 */
@Composable
private fun OfficerHeaderCard(
    profile: UserProfile?,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("officer_header_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Avatar icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AccentAmberContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Officer Admin",
                        tint = OnAccentAmberContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = profile?.fullName ?: "Officer Admin",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Role badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AccentAmberContainer
                        ) {
                            Text(
                                text = if (profile?.isPrimaryAdmin == true) "PRIMARY ADMIN" else "OFFICER ADMIN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = OnAccentAmberContainer
                                ),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = profile?.email ?: "admin@educhat.edu",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!profile?.mobile.isNullOrBlank()) {
                        Text(
                            text = "मो: ${profile?.mobile}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // Compact Logout Button (Accessible touch area)
            IconButton(
                onClick = onLogout,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("officer_logout_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Logout",
                    tint = TextSecondary
                )
            }
        }
    }
}

/**
 * 5 Compact Dashboard Tiles Grid
 */
@Composable
private fun OfficerDashboardTilesGrid(
    onTileClick: (OfficerAdminDialog) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Row 1: Tile 1 (अधिकारी नोंदणी) & Tile 2 (शाळा प्रशासक नोंदणी)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DashboardTile(
                title = "अधिकारी नोंदणी",
                subtitle = "नवीन अधिकारी जोडा",
                icon = Icons.Default.PersonAdd,
                iconTint = PrimaryIndigo,
                containerColor = PrimaryIndigoContainer,
                testTag = "tile_officer_admin_registration",
                modifier = Modifier.weight(1f),
                onClick = { onTileClick(OfficerAdminDialog.CreateOfficerAdmin) }
            )

            DashboardTile(
                title = "शाळा प्रशासक नोंदणी",
                subtitle = "शाळेचा प्रशासक जोडा",
                icon = Icons.Default.SupervisorAccount,
                iconTint = AccentAmber,
                containerColor = AccentAmberContainer,
                testTag = "tile_school_admin_registration",
                modifier = Modifier.weight(1f),
                onClick = { onTileClick(OfficerAdminDialog.CreateSchoolAdmin) }
            )
        }

        // Row 2: Tile 3 (शाळा नोंदणी) & Tile 4 (शाळा व्यवस्थापन)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DashboardTile(
                title = "शाळा नोंदणी",
                subtitle = "नवीन शाळा जोडा",
                icon = Icons.Default.AddBusiness,
                iconTint = SecondaryGreen,
                containerColor = SecondaryGreenContainer,
                testTag = "tile_school_registration",
                modifier = Modifier.weight(1f),
                onClick = { onTileClick(OfficerAdminDialog.CreateSchool) }
            )

            DashboardTile(
                title = "शाळा व्यवस्थापन",
                subtitle = "शाळांची यादी व बदल",
                icon = Icons.Default.AccountBalance,
                iconTint = PrimaryIndigo,
                containerColor = PrimaryIndigoContainer,
                testTag = "tile_school_management",
                modifier = Modifier.weight(1f),
                onClick = { onTileClick(OfficerAdminDialog.ManageSchools) }
            )
        }

        // Row 3: Tile 5 (माझे प्रोफाइल) Full Width
        DashboardTile(
            title = "माझे प्रोफाइल",
            subtitle = "माझी वैयक्तिक माहिती व तपशील",
            icon = Icons.Default.AccountCircle,
            iconTint = PrimaryIndigo,
            containerColor = PrimaryIndigoContainer,
            testTag = "tile_my_profile",
            modifier = Modifier.fillMaxWidth(),
            isFullWidth = true,
            onClick = { onTileClick(OfficerAdminDialog.MyProfile) }
        )
    }
}

@Composable
fun OfficerUsersTabContent(
    profile: UserProfile?,
    onOpenDialog: (OfficerAdminDialog) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "वापरकर्ते व्यवस्थापन (User Management)",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DashboardTile(
                title = "अधिकारी नोंदणी",
                subtitle = "नवीन अधिकारी जोडा",
                icon = Icons.Default.PersonAdd,
                iconTint = PrimaryIndigo,
                containerColor = PrimaryIndigoContainer,
                testTag = "tile_officer_admin_registration",
                modifier = Modifier.weight(1f),
                onClick = { onOpenDialog(OfficerAdminDialog.CreateOfficerAdmin) }
            )

            DashboardTile(
                title = "शाळा प्रशासक नोंदणी",
                subtitle = "शाळेचा प्रशासक जोडा",
                icon = Icons.Default.SupervisorAccount,
                iconTint = AccentAmber,
                containerColor = AccentAmberContainer,
                testTag = "tile_school_admin_registration",
                modifier = Modifier.weight(1f),
                onClick = { onOpenDialog(OfficerAdminDialog.CreateSchoolAdmin) }
            )
        }
    }
}

@Composable
fun OfficerSchoolsTabContent(
    schools: List<School>,
    onOpenDialog: (OfficerAdminDialog) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "शाळा व्यवस्थापन (Schools Management)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Text(
                text = "${schools.size} शाळा",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TextSecondary
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DashboardTile(
                title = "शाळा नोंदणी",
                subtitle = "नवीन शाळा जोडा",
                icon = Icons.Default.AddBusiness,
                iconTint = SecondaryGreen,
                containerColor = SecondaryGreenContainer,
                testTag = "tile_school_registration",
                modifier = Modifier.weight(1f),
                onClick = { onOpenDialog(OfficerAdminDialog.CreateSchool) }
            )

            DashboardTile(
                title = "शाळा व्यवस्थापन",
                subtitle = "शाळांची यादी व बदल",
                icon = Icons.Default.AccountBalance,
                iconTint = PrimaryIndigo,
                containerColor = PrimaryIndigoContainer,
                testTag = "tile_school_management",
                modifier = Modifier.weight(1f),
                onClick = { onOpenDialog(OfficerAdminDialog.ManageSchools) }
            )
        }
    }
}

@Composable
fun OfficerProfileTabContent(
    profile: UserProfile?,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OfficerHeaderCard(
            profile = profile,
            onLogout = onLogout
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "माझे प्रोफाइल तपशील",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Text(
                    text = "नाव: ${profile?.fullName ?: "-"}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                )
                Text(
                    text = "ईमेल: ${profile?.email ?: "-"}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                )
                Text(
                    text = "मोबाईल: ${profile?.mobile ?: "-"}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                )
                Text(
                    text = "भूमिका: ${if (profile?.isPrimaryAdmin == true) "प्राथमिक अधिकारी प्रशासक (Primary Officer Admin)" else "अधिकारी प्रशासक (Officer Admin)"}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onEditProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("officer_edit_profile_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("माहिती बदला (Edit Profile)")
                }
            }
        }
    }
}

/**
 * Individual Compact Interactive Dashboard Tile
 */
@Composable
private fun DashboardTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    isFullWidth: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        if (isFullWidth) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// TILE 1: Create Officer Admin Dialog
// -------------------------------------------------------------------------------------
@Composable
private fun CreateOfficerAdminDialog(
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (fullName: String, email: String, mobile: String, password: String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("dialog_create_officer_admin"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "अधिकारी नोंदणी (Officer Admin)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "नवीन अधिकारी प्रशासक तयार करा. (role: officer_admin, is_primary_admin: false, school_id: NULL)",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF991B1B)),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Inputs
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("पूर्ण नाव (Full Name)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_officer_full_name")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("ईमेल पत्ता (Email)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_officer_email")
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("मोबाईल क्रमांक (Mobile Number)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_officer_mobile")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("प्रारंभिक पासवर्ड (Initial Password)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_officer_password")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("रद्द करा (Cancel)")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(fullName, email, mobile, password) },
                        enabled = !isLoading && fullName.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        modifier = Modifier.testTag("btn_submit_officer_admin")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("अधिकारी नोंदवा (Create)")
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// TILE 2: Create School Admin Dialog
// -------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateSchoolAdminDialog(
    activeSchools: List<School>,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (fullName: String, email: String, mobile: String, password: String, schoolId: String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedSchoolId by remember { mutableStateOf(activeSchools.firstOrNull()?.id ?: "") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val selectedSchoolName = activeSchools.firstOrNull { it.id == selectedSchoolId }?.name ?: "शाळा निवडा (Select School)"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("dialog_create_school_admin"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "शाळा प्रशासक नोंदणी",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "शाळेसाठी प्रशासक तयार करा. (role: school_admin, school-scoped access)",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF991B1B)),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Inputs
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("पूर्ण नाव (Full Name)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_school_admin_full_name")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("ईमेल पत्ता (Email)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_school_admin_email")
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("मोबाईल क्रमांक (Mobile Number)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_school_admin_mobile")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("प्रारंभिक पासवर्ड (Initial Password)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_school_admin_password")
                )

                // School Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedSchoolName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("शाळा निवडा (Select School)") },
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("dropdown_school_selector")
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        if (activeSchools.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("कोणतीही सक्रिय शाळा उपलब्ध नाही (No active schools)") },
                                onClick = { dropdownExpanded = false }
                            )
                        } else {
                            activeSchools.forEach { school ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = school.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                            )
                                            Text(
                                                text = "Code: ${school.code}",
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedSchoolId = school.id
                                        dropdownExpanded = false
                                    },
                                    modifier = Modifier.testTag("school_option_${school.code}")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("रद्द करा (Cancel)")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(fullName, email, mobile, password, selectedSchoolId) },
                        enabled = !isLoading && fullName.isNotBlank() && email.isNotBlank() && password.isNotBlank() && selectedSchoolId.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                        modifier = Modifier.testTag("btn_submit_school_admin")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("प्रशासक नोंदवा (Register)")
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// TILE 3: Create School Dialog
// -------------------------------------------------------------------------------------
@Composable
private fun CreateSchoolDialog(
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, code: String, address: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("dialog_create_school"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "नवीन शाळा नोंदणी (School Registration)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "नवीन शाळेची नोंदणी करा. शाळा कोड अद्वितीय असणे आवश्यक आहे.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF991B1B)),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("शाळेचे नाव (School Name)") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_school_name")
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("शाळा कोड (Unique School Code)") },
                    leadingIcon = { Icon(Icons.Default.Domain, contentDescription = null) },
                    placeholder = { Text("e.g. SCH-PUN-005") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_school_code")
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("पत्ता (Address)") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_school_address")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("रद्द करा (Cancel)")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(name, code, address) },
                        enabled = !isLoading && name.isNotBlank() && code.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen),
                        modifier = Modifier.testTag("btn_submit_school")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("शाळा नोंदवा (Register)")
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// TILE 4: Manage Schools Dialog (List, Edit, Activate/Deactivate)
// -------------------------------------------------------------------------------------
@Composable
private fun ManageSchoolsDialog(
    schools: List<School>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onEditSchool: (School) -> Unit,
    onToggleStatus: (School) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredSchools = schools.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.code.contains(searchQuery, ignoreCase = true)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(600.dp)
                .testTag("dialog_manage_schools"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "शाळा व्यवस्थापन (Manage Schools)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "एकूण शाळा: ${schools.size}",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("शाळा किंवा कोड शोधा (Search...)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_schools")
                )

                // List of Schools
                if (filteredSchools.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "कोणतीही शाळा आढळली नाही.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredSchools, key = { it.id }) { school ->
                            SchoolItemCard(
                                school = school,
                                onEdit = { onEditSchool(school) },
                                onToggleStatus = { onToggleStatus(school) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SchoolItemCard(
    school: School,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("school_card_${school.code}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = school.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (school.isActive) SecondaryGreenContainer else Color(0xFFFEE2E2)
                    ) {
                        Text(
                            text = if (school.isActive) "सक्रिय (Active)" else "निष्क्रिय (Inactive)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = if (school.isActive) OnSecondaryGreenContainer else Color(0xFF991B1B)
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "कोड: ${school.code}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )

                if (!school.address.isNullOrBlank()) {
                    Text(
                        text = school.address,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextTertiary,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_edit_school_${school.code}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit School",
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onToggleStatus,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_toggle_status_${school.code}")
                ) {
                    Icon(
                        imageVector = if (school.isActive) Icons.Default.PowerSettingsNew else Icons.Default.CheckCircle,
                        contentDescription = if (school.isActive) "Deactivate" else "Activate",
                        tint = if (school.isActive) Color(0xFFDC2626) else SecondaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// Edit School Dialog
// -------------------------------------------------------------------------------------
@Composable
private fun EditSchoolDialog(
    school: School,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, code: String, address: String, isActive: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(school.name) }
    var code by remember { mutableStateOf(school.code) }
    var address by remember { mutableStateOf(school.address ?: "") }
    var isActive by remember { mutableStateOf(school.isActive) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("dialog_edit_school"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "शाळा माहिती बदल (Edit School)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF991B1B)),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("शाळेचे नाव (School Name)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_school_name")
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("शाळा कोड (School Code)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_school_code")
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("पत्ता (Address)") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_school_address")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "सक्रिय स्थिती (Active Status)",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                    )
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        modifier = Modifier.testTag("switch_edit_school_status")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("रद्द करा (Cancel)")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(name, code, address, isActive) },
                        enabled = !isLoading && name.isNotBlank() && code.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        modifier = Modifier.testTag("btn_save_edit_school")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("जतन करा (Save)")
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// Confirm Deactivation Dialog
// -------------------------------------------------------------------------------------
@Composable
private fun ConfirmDeactivateSchoolDialog(
    school: School,
    staffCount: Int,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                Text("शाळा निष्क्रिय करायची आहे का?", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("तुम्ही '${school.name}' ही शाळा निष्क्रिय करत आहात.")
                if (staffCount > 0) {
                    Surface(
                        color = Color(0xFFFFFBEB),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "सूचना: या शाळेशी संलग्न $staffCount सक्रिय कर्मचारी (School Admin/Teachers) आहेत. शाळा निष्क्रिय केल्यास त्यांना नवीन सत्र सुरू करता येणार नाही.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF78350F)),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                Text("ही मऊ निष्क्रियता (soft deactivation) आहे, कोणतीही माहिती कायमस्वरूपी हटवली जाणार नाही.")
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                modifier = Modifier.testTag("btn_confirm_deactivate_school")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("होय, निष्क्रिय करा")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("रद्द करा")
            }
        },
        modifier = Modifier.testTag("dialog_confirm_deactivate_school")
    )
}

// -------------------------------------------------------------------------------------
// TILE 5: Officer Profile Dialog (Displays Full Name, Email, Mobile, Role, Status)
// -------------------------------------------------------------------------------------
@Composable
private fun OfficerProfileDialog(
    profile: UserProfile?,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (fullName: String, mobile: String) -> Unit
) {
    var fullName by remember { mutableStateOf(profile?.fullName ?: "") }
    var mobile by remember { mutableStateOf(profile?.mobile ?: "") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("dialog_officer_profile"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "माझे प्रोफाइल (Officer Profile)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF991B1B)),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Editable field: Full Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("पूर्ण नाव (Full Name)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_profile_full_name")
                )

                // Editable field: Mobile
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("मोबाईल क्रमांक (Mobile Number)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_profile_mobile")
                )

                // Read-only field: Email
                OutlinedTextField(
                    value = profile?.email ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("ईमेल पत्ता (Email - Non Editable)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = TextPrimary,
                        disabledBorderColor = BorderSubtle,
                        disabledLabelColor = TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Read-only field: Role
                OutlinedTextField(
                    value = if (profile?.isPrimaryAdmin == true) "Primary Officer Admin (officer_admin)" else "Officer Admin (officer_admin)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("भूमिका (Role - Non Editable)") },
                    leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Read-only field: School Scope
                OutlinedTextField(
                    value = "निरंक (System-Wide Scope / NULL)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("शाळा क्षेत्र (School Scope - Non Editable)") },
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Read-only field: Account Status
                OutlinedTextField(
                    value = if (profile?.isActive == true) "सक्रिय (Active)" else "निष्क्रिय (Inactive)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("खाते स्थिती (Account Status)") },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("रद्द करा (Cancel)")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(fullName, mobile) },
                        enabled = !isLoading && fullName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        modifier = Modifier.testTag("btn_save_profile")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("जतन करा (Save)")
                        }
                    }
                }
            }
        }
    }
}
