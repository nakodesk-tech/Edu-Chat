package com.example.ui.auth

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AuthSession
import com.example.data.model.UserRole
import com.example.ui.chat.ChatGroupViewModel
import com.example.ui.chat.ChatsTabContent
import com.example.ui.students.StudentManagementContent
import com.example.ui.students.StudentManagementViewModel
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
fun RolePlaceholderScreen(
    session: AuthSession,
    onLogout: () -> Unit
) {
    val profile = session.profile
    val role = profile.userRole
    val chatViewModel: ChatGroupViewModel = viewModel()
    val studentViewModel: StudentManagementViewModel = viewModel()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val roleColor = when (role) {
        UserRole.TEACHER -> PrimaryIndigo
        UserRole.STUDENT -> SecondaryGreen
        UserRole.OFFICER_ADMIN, UserRole.SCHOOL_ADMIN -> AccentAmber
    }

    val roleContainerColor = when (role) {
        UserRole.TEACHER -> PrimaryIndigoContainer
        UserRole.STUDENT -> SecondaryGreenContainer
        UserRole.OFFICER_ADMIN, UserRole.SCHOOL_ADMIN -> AccentAmberContainer
    }

    val roleIcon = when (role) {
        UserRole.TEACHER -> Icons.Default.MenuBook
        UserRole.STUDENT -> Icons.Default.People
        UserRole.OFFICER_ADMIN, UserRole.SCHOOL_ADMIN -> Icons.Default.AdminPanelSettings
    }

    val tab2Title = if (role == UserRole.TEACHER) "विद्यार्थी" else "अभ्यास"
    val tab2Icon = if (role == UserRole.TEACHER) Icons.Default.People else Icons.Default.MenuBook
    val tab3Title = if (role == UserRole.TEACHER) "चाचण्या" else "प्रगती"
    val tab3Icon = if (role == UserRole.TEACHER) Icons.Default.Assessment else Icons.Default.AutoGraph

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = roleContainerColor,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = roleIcon,
                                    contentDescription = null,
                                    tint = roleColor,
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
                                text = "${role.displayName} डॅशबोर्ड",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Log Out",
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
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "चॅट्स",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "चॅट्स",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = roleColor,
                        selectedTextColor = roleColor,
                        indicatorColor = roleContainerColor,
                        unselectedIconColor = TextTertiary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("tab_chats")
                )

                // Tab 2: विद्यार्थी / अभ्यास
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = {
                        Icon(
                            imageVector = tab2Icon,
                            contentDescription = tab2Title,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = tab2Title,
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = roleColor,
                        selectedTextColor = roleColor,
                        indicatorColor = roleContainerColor,
                        unselectedIconColor = TextTertiary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("tab_secondary")
                )

                // Tab 3: चाचण्या / प्रगती
                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    icon = {
                        Icon(
                            imageVector = tab3Icon,
                            contentDescription = tab3Title,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = tab3Title,
                            fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = roleColor,
                        selectedTextColor = roleColor,
                        indicatorColor = roleContainerColor,
                        unselectedIconColor = TextTertiary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("tab_tertiary")
                )

                // Tab 4: माझे प्रोफाइल
                NavigationBarItem(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "माझे प्रोफाइल",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "माझे प्रोफाइल",
                            fontWeight = if (selectedTabIndex == 3) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = roleColor,
                        selectedTextColor = roleColor,
                        indicatorColor = roleContainerColor,
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
            when (selectedTabIndex) {
                0 -> {
                    ChatsTabContent(viewModel = chatViewModel)
                }
                1 -> {
                    if (role == UserRole.TEACHER) {
                        StudentManagementContent(viewModel = studentViewModel)
                    } else {
                        RolePlaceholderUpcomingTab(
                            title = tab2Title,
                            subtitle = "हे वैशिष्ट्य पुढील टप्प्यात उपलब्ध होईल (Coming Soon in next phase)",
                            icon = tab2Icon,
                            roleColor = roleColor,
                            roleContainerColor = roleContainerColor
                        )
                    }
                }
                2 -> {
                    RolePlaceholderUpcomingTab(
                        title = tab3Title,
                        subtitle = "हे वैशिष्ट्य पुढील टप्प्यात उपलब्ध होईल (Coming Soon in next phase)",
                        icon = tab3Icon,
                        roleColor = roleColor,
                        roleContainerColor = roleContainerColor
                    )
                }
                3 -> {
                    RoleProfileContent(
                        session = session,
                        roleColor = roleColor,
                        roleContainerColor = roleContainerColor,
                        roleIcon = roleIcon,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}

@Composable
private fun RolePlaceholderUpcomingTab(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    roleColor: Color,
    roleContainerColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(roleContainerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = roleColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RoleProfileContent(
    session: AuthSession,
    roleColor: Color,
    roleContainerColor: Color,
    roleIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onLogout: () -> Unit
) {
    val profile = session.profile
    val role = profile.userRole

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Success Badge Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(roleContainerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = roleIcon,
                        contentDescription = role.displayName,
                        tint = roleColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = profile.fullName ?: "Authenticated User",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = profile.email ?: "",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Verified Role Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = roleContainerColor,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified Role",
                            tint = roleColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Verified ${role.displayName} Account",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = roleColor
                        )
                    }
                }
            }
        }

        // Security & Guard Status Verification Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security Guard",
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Authentication Guard Status",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                HorizontalDivider(color = BorderSubtle)

                StatusRow(
                    label = "Supabase Auth",
                    value = "Authenticated (Active Token)",
                    isSuccess = true
                )
                StatusRow(
                    label = "Database Profile",
                    value = "UUID: ${profile.id.take(13)}...",
                    isSuccess = true
                )
                StatusRow(
                    label = "Database Role",
                    value = "${profile.role.uppercase()} (Matched UI Selection)",
                    isSuccess = true
                )
                StatusRow(
                    label = "Account Status",
                    value = if (profile.isActive) "Active" else "Inactive",
                    isSuccess = profile.isActive
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Logout Action
        Button(
            onClick = onLogout,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = PrimaryIndigo
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp))
                .testTag("placeholder_signout_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "LOG OUT / SWITCH ROLE",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, isSuccess: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isSuccess) SecondaryGreen else AccentAmber,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = value,
                fontSize = 13.sp,
                color = if (isSuccess) TextPrimary else AccentAmber,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
