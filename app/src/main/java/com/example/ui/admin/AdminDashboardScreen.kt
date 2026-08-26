package com.example.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
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
fun AdminDashboardScreen(
    viewModel: AdminDashboardViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Compact Admin Top Section
            AdminTopHeader(
                adminProfile = uiState.adminProfile,
                onLogout = onLogout
            )

            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

            // 2. User Management Section Header & Actions
            UserManagementHeader(
                totalCount = uiState.filteredUsers.size,
                onAddUserClicked = { viewModel.openAddDialog() }
            )

            // 3. Filter Chips & Search Bar
            CompactFilterAndSearchBar(
                selectedFilter = uiState.filter,
                onFilterSelected = { viewModel.setFilter(it) },
                searchQuery = uiState.searchQuery,
                onSearchQueryChanged = { viewModel.setSearchQuery(it) }
            )

            // 4. User List Area
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = PrimaryIndigo,
                        strokeWidth = 3.dp
                    )
                }
            } else if (uiState.filteredUsers.isEmpty()) {
                EmptyUserListView(
                    filter = uiState.filter,
                    isSearching = uiState.searchQuery.isNotBlank(),
                    onResetSearch = { viewModel.setSearchQuery("") }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.filteredUsers,
                        key = { it.id }
                    ) { user ->
                        val isCurrentAdmin = user.id == uiState.adminProfile?.id
                        UserCardItem(
                            user = user,
                            isCurrentAdmin = isCurrentAdmin,
                            onEdit = { viewModel.openEditDialog(user) },
                            onDeactivate = { viewModel.promptDeactivate(user) },
                            onReactivate = { viewModel.reactivateUser(user) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Add User Dialog
    if (uiState.showAddDialog) {
        AddUserDialog(
            isSubmitting = uiState.isActionLoading,
            onDismiss = { viewModel.closeAddDialog() },
            onSubmit = { name, email, pass, role, isActive ->
                viewModel.createUser(name, email, pass, role, isActive)
            }
        )
    }

    // Edit User Dialog
    uiState.userToEdit?.let { user ->
        EditUserDialog(
            user = user,
            isSubmitting = uiState.isActionLoading,
            onDismiss = { viewModel.closeEditDialog() },
            onSubmit = { name, role, isActive ->
                viewModel.updateUser(user.id, name, role, isActive)
            }
        )
    }

    // Deactivate Confirmation Dialog
    uiState.userToDeactivate?.let { user ->
        DeactivateConfirmDialog(
            user = user,
            isSubmitting = uiState.isActionLoading,
            onDismiss = { viewModel.dismissDeactivatePrompt() },
            onConfirm = { viewModel.confirmDeactivate() }
        )
    }
}

/**
 * Compact Top Section displaying Admin info, ADMIN badge, and Logout button.
 */
@Composable
private fun AdminTopHeader(
    adminProfile: UserProfile?,
    onLogout: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Admin Icon Badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AccentAmberContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SupervisorAccount,
                        contentDescription = "Admin Avatar",
                        tint = OnAccentAmberContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = adminProfile?.fullName ?: "Administrator",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // ADMIN Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AccentAmberContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (adminProfile?.isPrimaryAdmin == true) "PRIMARY OFFICER ADMIN" else "OFFICER ADMIN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = OnAccentAmberContainer,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Text(
                        text = adminProfile?.email ?: "admin@educhat.edu",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Logout Button
            OutlinedButton(
                onClick = onLogout,
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("admin_logout_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Logout",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Logout",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

/**
 * Section title with "+ Add User" primary action.
 */
@Composable
private fun UserManagementHeader(
    totalCount: Int,
    onAddUserClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "User Management",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryIndigoContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$totalCount",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = OnPrimaryIndigoContainer
                    )
                )
            }
        }

        Button(
            onClick = onAddUserClicked,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryIndigo,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.testTag("add_user_button")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add User",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Add User",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

/**
 * Filter chips & compact search bar.
 */
@Composable
private fun CompactFilterAndSearchBar(
    selectedFilter: UserRoleFilter,
    onFilterSelected: (UserRoleFilter) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search textfield
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Search by name, email, or role...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(18.dp),
                    tint = TextTertiary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChanged("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            modifier = Modifier.size(16.dp),
                            tint = TextSecondary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryIndigo,
                unfocusedBorderColor = BorderSubtle,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("user_search_input")
        )

        // Filter chips row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            UserRoleFilter.entries.forEach { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterSelected(filter) },
                    label = {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryIndigoContainer,
                        selectedLabelColor = OnPrimaryIndigoContainer,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = BorderSubtle,
                        selectedBorderColor = PrimaryIndigo
                    ),
                    modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                )
            }
        }
    }
}

/**
 * User card representation showing details, role badges, active status, and actions.
 */
@Composable
private fun UserCardItem(
    user: UserProfile,
    isCurrentAdmin: Boolean,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onReactivate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_card_${user.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left avatar & user info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val role = user.userRole
                    val (avatarBg, avatarIconTint, avatarIcon) = when (role) {
                        UserRole.OFFICER_ADMIN, UserRole.SCHOOL_ADMIN -> Triple(AccentAmberContainer, OnAccentAmberContainer, Icons.Default.SupervisorAccount)
                        UserRole.TEACHER -> Triple(PrimaryIndigoContainer, OnPrimaryIndigoContainer, Icons.Default.School)
                        UserRole.STUDENT -> Triple(SecondaryGreenContainer, OnSecondaryGreenContainer, Icons.Default.Person)
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(avatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = avatarIcon,
                            contentDescription = role.displayName,
                            tint = avatarIconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = user.fullName ?: "Unnamed User",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (user.isActive) TextPrimary else TextTertiary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (isCurrentAdmin) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(BorderSubtle)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "You",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextSecondary,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                        }

                        Text(
                            text = user.email ?: "No email",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status Indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (user.isActive) SecondaryGreenContainer else Color(0xFFF3F4F6)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (user.isActive) SecondaryGreen else Color(0xFF9CA3AF))
                        )
                        Text(
                            text = if (user.isActive) "Active" else "Inactive",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (user.isActive) OnSecondaryGreenContainer else Color(0xFF6B7280),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Badges & Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Role badge
                val (roleBg, roleTextColor) = when (user.userRole) {
                    UserRole.OFFICER_ADMIN, UserRole.SCHOOL_ADMIN -> Pair(AccentAmberContainer, OnAccentAmberContainer)
                    UserRole.TEACHER -> Pair(PrimaryIndigoContainer, OnPrimaryIndigoContainer)
                    UserRole.STUDENT -> Pair(SecondaryGreenContainer, OnSecondaryGreenContainer)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(roleBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (user.isPrimaryAdmin) "PRIMARY ADMIN" else user.userRole.displayName.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = roleTextColor,
                            fontSize = 10.sp
                        )
                    )
                }

                // Actions (Only manageable if not current admin and not officer admin)
                if (!isCurrentAdmin && user.userRole != UserRole.OFFICER_ADMIN && user.userRole != UserRole.SCHOOL_ADMIN && !user.isPrimaryAdmin) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Edit action
                        TextButton(
                            onClick = onEdit,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("edit_user_${user.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(14.dp),
                                tint = PrimaryIndigo
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Edit",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = PrimaryIndigo,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        // Deactivate or Reactivate action
                        if (user.isActive) {
                            TextButton(
                                onClick = onDeactivate,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("deactivate_user_${user.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = "Deactivate",
                                    modifier = Modifier.size(14.dp),
                                    tint = AccentAmber
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Deactivate",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AccentAmber,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        } else {
                            TextButton(
                                onClick = onReactivate,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("reactivate_user_${user.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Reactivate",
                                    modifier = Modifier.size(14.dp),
                                    tint = SecondaryGreen
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Reactivate",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = SecondaryGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Empty list illustration / placeholder.
 */
@Composable
private fun EmptyUserListView(
    filter: UserRoleFilter,
    isSearching: Boolean,
    onResetSearch: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = if (isSearching) "No matching users found" else "No users in '${filter.label}'",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            )
            if (isSearching) {
                TextButton(onClick = onResetSearch) {
                    Text("Clear search query", color = PrimaryIndigo)
                }
            }
        }
    }
}

/**
 * Add User Modal Dialog.
 */
@Composable
private fun AddUserDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (name: String, email: String, pass: String, role: UserRole, isActive: Boolean) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }
    var isActive by remember { mutableStateOf(true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(
                text = "Add New User",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; validationError = null },
                    label = { Text("Full Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_user_fullname_input")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; validationError = null },
                    label = { Text("Email Address") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_user_email_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; validationError = null },
                    label = { Text("Initial Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_user_password_input")
                )

                // Role Selector (Teacher / Student only - Admin NOT allowed)
                Text(
                    text = "Role",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val allowableRoles = listOf(UserRole.TEACHER, UserRole.STUDENT)
                    allowableRoles.forEach { role ->
                        val isSelected = selectedRole == role
                        val (chipBg, textColor) = if (isSelected) {
                            when (role) {
                                UserRole.TEACHER -> Pair(PrimaryIndigoContainer, OnPrimaryIndigoContainer)
                                UserRole.STUDENT -> Pair(SecondaryGreenContainer, OnSecondaryGreenContainer)
                                else -> Pair(Color.LightGray, Color.Black)
                            }
                        } else {
                            Pair(MaterialTheme.colorScheme.surface, TextSecondary)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipBg)
                                .border(
                                    1.dp,
                                    if (isSelected) PrimaryIndigo else BorderSubtle,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedRole = role }
                                .padding(vertical = 10.dp)
                                .testTag("select_role_${role.dbValue}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = role.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor
                                )
                            )
                        }
                    }
                }

                // Active toggle switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Account Active",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                    )
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SecondaryGreen
                        ),
                        modifier = Modifier.testTag("add_user_active_switch")
                    )
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isBlank()) {
                        validationError = "Please enter the user's full name."
                        return@Button
                    }
                    if (email.isBlank() || !email.contains("@")) {
                        validationError = "Please enter a valid email address."
                        return@Button
                    }
                    if (password.length < 6) {
                        validationError = "Password must be at least 6 characters."
                        return@Button
                    }
                    onSubmit(fullName, email, password, selectedRole, isActive)
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_create_user_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create Account")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Edit User Modal Dialog.
 */
@Composable
private fun EditUserDialog(
    user: UserProfile,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (name: String, role: UserRole, isActive: Boolean) -> Unit
) {
    var fullName by remember { mutableStateOf(user.fullName ?: "") }
    var selectedRole by remember { mutableStateOf(user.userRole) }
    var isActive by remember { mutableStateOf(user.isActive) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(
                text = "Edit User",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; validationError = null },
                    label = { Text("Full Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_user_fullname_input")
                )

                OutlinedTextField(
                    value = user.email ?: "",
                    onValueChange = {},
                    label = { Text("Email (Locked)") },
                    enabled = false,
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = BorderSubtle,
                        disabledTextColor = TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Role Selector (Teacher / Student only)
                Text(
                    text = "Role",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val allowableRoles = listOf(UserRole.TEACHER, UserRole.STUDENT)
                    allowableRoles.forEach { role ->
                        val isSelected = selectedRole == role
                        val (chipBg, textColor) = if (isSelected) {
                            when (role) {
                                UserRole.TEACHER -> Pair(PrimaryIndigoContainer, OnPrimaryIndigoContainer)
                                UserRole.STUDENT -> Pair(SecondaryGreenContainer, OnSecondaryGreenContainer)
                                else -> Pair(Color.LightGray, Color.Black)
                            }
                        } else {
                            Pair(MaterialTheme.colorScheme.surface, TextSecondary)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipBg)
                                .border(
                                    1.dp,
                                    if (isSelected) PrimaryIndigo else BorderSubtle,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedRole = role }
                                .padding(vertical = 10.dp)
                                .testTag("edit_role_${role.dbValue}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = role.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor
                                )
                            )
                        }
                    }
                }

                // Active toggle switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Account Active",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                    )
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SecondaryGreen
                        ),
                        modifier = Modifier.testTag("edit_user_active_switch")
                    )
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isBlank()) {
                        validationError = "Full name cannot be blank."
                        return@Button
                    }
                    onSubmit(fullName, selectedRole, isActive)
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_save_user_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Changes")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Deactivate Confirmation Dialog.
 */
@Composable
private fun DeactivateConfirmDialog(
    user: UserProfile,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = AccentAmber,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Deactivate User?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Text(
                text = "Are you sure you want to deactivate ${user.fullName ?: "this user"}?\n\nThey will be prevented from logging in, but their profile and historical data will be safely preserved.",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentAmber,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_deactivate_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Deactivate User")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("Cancel")
            }
        }
    )
}
