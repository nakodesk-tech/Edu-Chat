package com.example.ui.users

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentAmberContainer
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.OnSecondaryGreenContainer
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoContainer
import com.example.ui.theme.SecondaryGreen
import com.example.ui.theme.SecondaryGreenContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

enum class RoleFilter(val key: String?, val marathiLabel: String) {
    ALL(null, "सर्व"),
    OFFICER_ADMIN("officer_admin", "Officer Admin"),
    SCHOOL_ADMIN("school_admin", "School Admin"),
    TEACHER("teacher", "Teacher"),
    STUDENT("student", "Student")
}

@Composable
fun RegisteredUsersContent(
    users: List<UserProfile>,
    schoolsMap: Map<String, String>,
    isLoading: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onUserClick: (UserProfile) -> Unit,
    onEditUser: (UserProfile) -> Unit = {},
    onToggleUserStatus: (UserProfile) -> Unit = {},
    onDeleteUser: (UserProfile) -> Unit = {},
    onOpenOfficerAdminRegistration: (() -> Unit)? = null,
    onOpenSchoolAdminRegistration: (() -> Unit)? = null,
    headerActions: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(RoleFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(users, selectedFilter, searchQuery) {
        users.filter { user ->
            // Role filter
            val matchesRole = when (selectedFilter) {
                RoleFilter.ALL -> true
                RoleFilter.OFFICER_ADMIN -> user.role.equals("officer_admin", ignoreCase = true)
                RoleFilter.SCHOOL_ADMIN -> user.role.equals("school_admin", ignoreCase = true)
                RoleFilter.TEACHER -> user.role.equals("teacher", ignoreCase = true)
                RoleFilter.STUDENT -> user.role.equals("student", ignoreCase = true)
            }

            // Search query filter
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                (user.fullName ?: "").lowercase().contains(q) ||
                        (user.email ?: "").lowercase().contains(q) ||
                        (user.mobile ?: "").contains(q) ||
                        (user.schoolId?.let { schoolsMap[it] } ?: "").lowercase().contains(q)
            }

            matchesRole && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header Bar: Title & Count & Refresh (as in reference image)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "नोंदणीकृत वापरकर्ते",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "एकूण ${users.size} वापरकर्ते (Registered Users)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("btn_refresh_users")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "ताजे करा (Refresh)",
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // 2. Top Registration Cards (Horizontally centered icon and all text)
        if (onOpenOfficerAdminRegistration != null && onOpenSchoolAdminRegistration != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RegistrationCard(
                    title = "अधिकारी नोंदणी",
                    description = "शिक्षण विस्तार अधिकारी, केंद्रप्रमुख यांची प्रशासन अधिकारी म्हणून नोंदणी करता येईल.",
                    icon = Icons.Default.PersonAdd,
                    iconTint = PrimaryIndigo,
                    iconBgColor = PrimaryIndigoContainer,
                    testTag = "tile_officer_admin_registration",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenOfficerAdminRegistration
                )

                RegistrationCard(
                    title = "शाळा प्रशासक नोंदणी",
                    description = "शाळेचे मुख्याध्यापक यांची मुख्य शाळा प्रशासक म्हणून नोंदणी करता येईल. येथे शिक्षक,विद्यार्थी नोंदणी करू नये",
                    icon = Icons.Default.SupervisorAccount,
                    iconTint = AccentAmber,
                    iconBgColor = AccentAmberContainer,
                    testTag = "tile_school_admin_registration",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSchoolAdminRegistration
                )
            }
        } else if (headerActions != null) {
            headerActions()
        }

        // 3. Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "नाव, ईमेल किंवा मोबाइल शोधा...",
                    fontSize = 13.sp,
                    color = TextTertiary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "शोधा",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "साफ करा",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
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
                .testTag("registered_users_search_input")
        )

        // 4. Role Filter Tabs/Chips with Filter Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoleFilter.values().forEach { filter ->
                    val count = remember(users, filter) {
                        when (filter) {
                            RoleFilter.ALL -> users.size
                            RoleFilter.OFFICER_ADMIN -> users.count { it.role.equals("officer_admin", ignoreCase = true) }
                            RoleFilter.SCHOOL_ADMIN -> users.count { it.role.equals("school_admin", ignoreCase = true) }
                            RoleFilter.TEACHER -> users.count { it.role.equals("teacher", ignoreCase = true) }
                            RoleFilter.STUDENT -> users.count { it.role.equals("student", ignoreCase = true) }
                        }
                    }

                    val isSelected = selectedFilter == filter

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = "${filter.marathiLabel} ($count)",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryIndigoContainer,
                            selectedLabelColor = PrimaryIndigo,
                            containerColor = Color.White,
                            labelColor = TextSecondary
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) PrimaryIndigo else BorderSubtle
                        ),
                        modifier = Modifier.testTag("role_filter_${filter.name.lowercase()}")
                    )
                }
            }

            // Filter Icon Button matching reference design
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { /* Filter shortcut */ }
                    .testTag("btn_filter_icon")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = "फिल्टर",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 5. Content Area: Loading / Error / Empty / List
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = PrimaryIndigo,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = errorMessage,
                        fontSize = 14.sp,
                        color = Color(0xFFDC2626),
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = onRefresh,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PrimaryIndigo)
                    ) {
                        Text("पुन्हा प्रयत्न करा (Retry)", color = PrimaryIndigo, fontSize = 13.sp)
                    }
                }
            }
        } else if (filteredUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PrimaryIndigoContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Text(
                        text = "या विभागात कोणतेही नोंदणीकृत वापरकर्ते नाहीत.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = "शोध शब्दाशी जुळणारे निकाल सापडले नाहीत.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("registered_users_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filteredUsers, key = { it.id }) { user ->
                    val schoolName = user.schoolId?.let { schoolsMap[it] }
                    RegisteredUserCard(
                        user = user,
                        schoolName = schoolName,
                        onClick = { onUserClick(user) },
                        onEdit = { onEditUser(user) },
                        onToggleStatus = { onToggleUserStatus(user) },
                        onDelete = { onDeleteUser(user) }
                    )
                }
            }
        }
    }
}

/**
 * Top Registration Card Component with horizontally centered icon, title, and description
 */
@Composable
fun RegistrationCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    iconBgColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconBgColor,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = description,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Compact Horizontal User Card matching reference image:
 * - Left side: Vertically centered role icon with soft circular background
 * - Middle/Right side: Left-aligned bold name, role badge + status badge, email + mobile
 * - Top-right: Three-dot menu button (Edit, Active/Inactive, Delete)
 */
@Composable
fun RegisteredUserCard(
    user: UserProfile,
    schoolName: String?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val roleColor = when (user.role.lowercase()) {
        "officer_admin" -> PrimaryIndigo
        "school_admin" -> AccentAmber
        "teacher" -> PrimaryIndigo
        "student" -> SecondaryGreen
        else -> TextSecondary
    }

    val roleBgColor = when (user.role.lowercase()) {
        "officer_admin" -> PrimaryIndigoContainer
        "school_admin" -> AccentAmberContainer
        "teacher" -> PrimaryIndigoContainer
        "student" -> SecondaryGreenContainer
        else -> Color(0xFFF3F4F6)
    }

    val roleIcon: ImageVector = when (user.role.lowercase()) {
        "officer_admin" -> Icons.Default.AdminPanelSettings
        "school_admin" -> Icons.Default.SupervisorAccount
        "teacher" -> Icons.Default.School
        "student" -> Icons.Default.Face
        else -> Icons.Default.Person
    }

    val roleDisplayName = when (user.role.lowercase()) {
        "officer_admin" -> if (user.isPrimaryAdmin) "Primary Officer Admin" else "Officer Admin"
        "school_admin" -> "School Admin"
        "teacher" -> "Teacher"
        "student" -> "Student"
        else -> user.role.replaceFirstChar { it.uppercase() }
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("user_card_${user.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. LEFT SIDE: Role Icon inside soft circular background (vertically centered)
            Surface(
                shape = CircleShape,
                color = roleBgColor,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = roleIcon,
                        contentDescription = roleDisplayName,
                        tint = roleColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 2. RIGHT / CONTENT SIDE: Left aligned
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // User Name: bold, slightly larger, visually dominant
                Text(
                    text = user.fullName ?: "अनामिक वापरकर्ता",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Role badge + Status badge Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Role Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = roleBgColor,
                        border = BorderStroke(0.5.dp, roleColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = roleDisplayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = roleColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    // Standard / Class Badge
                    if (!user.standard.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryIndigoContainer,
                            border = BorderStroke(0.5.dp, PrimaryIndigo.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = user.standard,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryIndigo,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (user.isActive) SecondaryGreenContainer else Color(0xFFFEE2E2),
                        border = BorderStroke(
                            0.5.dp,
                            if (user.isActive) SecondaryGreen.copy(alpha = 0.35f) else Color(0xFFEF4444).copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (user.isActive) SecondaryGreen else Color(0xFFDC2626))
                            )
                            Text(
                                text = if (user.isActive) "सक्रिय" else "निष्क्रीय",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (user.isActive) OnSecondaryGreenContainer else Color(0xFF991B1B)
                            )
                        }
                    }
                }

                // Contact Row: Email + Phone
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!user.email.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "ईमेल",
                                tint = TextTertiary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = user.email,
                                fontSize = 11.5.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!user.mobile.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "फोन",
                                tint = TextTertiary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = user.mobile,
                                fontSize = 11.5.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 3. TOP-RIGHT: Three-dot menu (⋮)
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_user_menu_${user.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "पर्याय (Options)",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                        modifier = Modifier.testTag("menu_edit_user_${user.id}")
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (user.isActive) "निष्क्रीय करा" else "सक्रिय करा",
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (user.isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (user.isActive) AccentAmber else SecondaryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleStatus()
                        },
                        modifier = Modifier.testTag("menu_toggle_status_${user.id}")
                    )

                    DropdownMenuItem(
                        text = { Text("Delete", fontSize = 13.sp, color = Color(0xFFDC2626)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                        modifier = Modifier.testTag("menu_delete_user_${user.id}")
                    )
                }
            }
        }
    }
}

/**
 * Edit User Dialog (Material 3)
 */
@Composable
fun EditUserDialog(
    user: UserProfile,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (fullName: String, mobile: String) -> Unit
) {
    var fullName by remember(user) { mutableStateOf(user.fullName ?: "") }
    var mobile by remember(user) { mutableStateOf(user.mobile ?: "") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "वापरकर्ता माहिती संपादित करा (Edit User)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = Color(0xFFDC2626)
                    )
                }

                // Email display (Read-only)
                OutlinedTextField(
                    value = user.email ?: "",
                    onValueChange = {},
                    label = { Text("ईमेल (Read-only)") },
                    enabled = false,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledContainerColor = Color(0xFFF3F4F6),
                        disabledBorderColor = BorderSubtle,
                        disabledTextColor = TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Full Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("पूर्ण नाव (Full Name)") },
                    enabled = !isLoading,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_user_name")
                )

                // Mobile
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("मोबाईल क्रमांक (Mobile)") },
                    enabled = !isLoading,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_user_mobile")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(fullName.trim(), mobile.trim()) },
                enabled = !isLoading && fullName.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryIndigo,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("btn_save_user_edit")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("जतन करा (Save)", fontSize = 13.sp)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Text("रद्द करा", fontSize = 13.sp, color = TextSecondary)
            }
        },
        shape = RoundedCornerShape(18.dp),
        containerColor = Color.White
    )
}

/**
 * Confirmation AlertDialog for Active / Inactive Status Toggle
 */
@Composable
fun ConfirmToggleUserStatusDialog(
    user: UserProfile,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    val targetStatus = !user.isActive
    val actionText = if (targetStatus) "सक्रिय करा" else "निष्क्रीय करा"

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "स्थिती बदला ($actionText)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Text(
                text = "आपण सदर वापरकर्ता सक्रिय किंवा निष्क्रीय करू इच्छिता का? पुन्हा केव्हाही आपण त्यांना सक्रिय करू शकतात.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = TextPrimary
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(targetStatus) },
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (targetStatus) SecondaryGreen else AccentAmber,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("btn_confirm_toggle_status")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(actionText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Text("रद्द करा", fontSize = 13.sp, color = TextSecondary)
            }
        },
        shape = RoundedCornerShape(18.dp),
        containerColor = Color.White
    )
}

/**
 * Destructive Confirmation Dialog for Deleting User
 */
@Composable
fun ConfirmDeleteUserDialog(
    user: UserProfile,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "वापरकर्ता कायमचा काढून टाकायचा आहे का?",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFDC2626)
            )
        },
        text = {
            Text(
                text = "हा वापरकर्ता (${user.fullName ?: user.email}) नोंदणीकृत वापरकर्त्यांच्या यादीतून काढून टाकला जाईल. ही कृती करण्यापूर्वी खात्री करा.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = TextPrimary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("btn_confirm_delete_user")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("काढून टाका", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Text("रद्द करा", fontSize = 13.sp, color = TextSecondary)
            }
        },
        shape = RoundedCornerShape(18.dp),
        containerColor = Color.White
    )
}

/**
 * Detailed User Dialog showing full profile attributes
 */
@Composable
fun RegisteredUserDetailDialog(
    user: UserProfile,
    schoolName: String?,
    onDismiss: () -> Unit
) {
    val roleColor = when (user.role.lowercase()) {
        "officer_admin" -> PrimaryIndigo
        "school_admin" -> AccentAmber
        "teacher" -> PrimaryIndigo
        "student" -> SecondaryGreen
        else -> TextSecondary
    }

    val roleBgColor = when (user.role.lowercase()) {
        "officer_admin" -> PrimaryIndigoContainer
        "school_admin" -> AccentAmberContainer
        "teacher" -> PrimaryIndigoContainer
        "student" -> SecondaryGreenContainer
        else -> Color(0xFFF3F4F6)
    }

    val roleIcon: ImageVector = when (user.role.lowercase()) {
        "officer_admin" -> Icons.Default.AdminPanelSettings
        "school_admin" -> Icons.Default.SupervisorAccount
        "teacher" -> Icons.Default.School
        "student" -> Icons.Default.Face
        else -> Icons.Default.Person
    }

    val roleDisplayName = when (user.role.lowercase()) {
        "officer_admin" -> if (user.isPrimaryAdmin) "Primary Officer Admin" else "Officer Admin (अधिकारी)"
        "school_admin" -> "School Admin (शाळा प्रशासक)"
        "teacher" -> "Teacher (शिक्षक)"
        "student" -> "Student (विद्यार्थी)"
        else -> user.role.replaceFirstChar { it.uppercase() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Avatar
                Surface(
                    shape = CircleShape,
                    color = roleBgColor,
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = roleIcon,
                            contentDescription = null,
                            tint = roleColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Name & Role
                Text(
                    text = user.fullName ?: "अनामिक वापरकर्ता",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = roleBgColor,
                    border = BorderStroke(1.dp, roleColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = roleDisplayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = roleColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = BorderSubtle)

                // Details List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow(label = "ईमेल (Email)", value = user.email ?: "-")
                    DetailRow(label = "मोबाईल (Mobile)", value = user.mobile ?: "उपलब्ध नाही")
                    DetailRow(
                        label = "शाळा (School)",
                        value = schoolName ?: if (user.role.equals("officer_admin", ignoreCase = true)) "लागू नाही (N/A)" else "नियुक्त नाही"
                    )
                    DetailRow(
                        label = "खाते स्थिती (Status)",
                        value = if (user.isActive) "सक्रिय (Active)" else "निष्क्रिय (Inactive)",
                        valueColor = if (user.isActive) SecondaryGreen else Color(0xFFDC2626)
                    )
                    if (user.createdAt != null) {
                        DetailRow(label = "नोंदणी तारीख (Created)", value = user.createdAt.take(10))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryIndigo,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_close_user_detail")
            ) {
                Text(
                    text = "बंद करा (Close)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        shape = RoundedCornerShape(18.dp),
        containerColor = Color.White
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
