package com.example.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Group
import com.example.data.model.School
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
fun AddMemberDialog(
    group: Group,
    eligibleUsers: List<UserProfile>,
    selectedUserIds: Set<String>,
    activeSchools: List<School>,
    emailSearchQuery: String,
    searchedUserByEmail: UserProfile?,
    emailSearchMessage: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onEmailSearch: (String) -> Unit,
    onToggleSelectUser: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRoleFilterChange: (String?) -> Unit,
    onSchoolFilterChange: (String?) -> Unit,
    onAddSingleUser: (String) -> Unit,
    onAddSelectedBatch: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var emailInput by remember { mutableStateOf(emailSearchQuery) }
    var searchInput by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf<String?>(null) }
    var selectedSchoolFilter by remember { mutableStateOf<String?>(null) }
    var isSchoolDropdownExpanded by remember { mutableStateOf(false) }

    val tabs = listOf("ई-मेलने शोधा (Email Search)", "वापरकर्त्यांमधून निवडा (Select Users)")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PrimaryIndigoContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "सदस्य जोडा (Add Member)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = group.name,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "बंद करा", tint = TextSecondary)
                    }
                }

                // Tabs for selection mode
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF1F3F5),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = PrimaryIndigo
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) PrimaryIndigo else TextSecondary
                                )
                            }
                        )
                    }
                }

                // Mode A: Search by Email
                if (selectedTab == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "नोंदणीकृत वापरकर्त्याचा Email प्रविष्ट करा:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = {
                                    emailInput = it
                                    onEmailSearch(it)
                                },
                                placeholder = { Text("उदा. admin@educhat.edu", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryIndigo,
                                    unfocusedBorderColor = BorderSubtle
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("member_email_search_input")
                            )

                            Button(
                                onClick = { onEmailSearch(emailInput) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "शोधा", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("शोधा", fontSize = 13.sp)
                            }
                        }

                        // Search result feedback
                        if (!emailSearchMessage.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AccentAmberContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = emailSearchMessage,
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // Found User Card
                        if (searchedUserByEmail != null) {
                            UserFoundCard(
                                user = searchedUserByEmail,
                                activeSchools = activeSchools,
                                isLoading = isLoading,
                                onAdd = { onAddSingleUser(searchedUserByEmail.id) }
                            )
                        }
                    }
                }

                // Mode B: Select from Registered Users with Filter & Multi-Select
                if (selectedTab == 1) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Search bar
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = {
                                searchInput = it
                                onSearchQueryChange(it)
                            },
                            placeholder = { Text("नाव किंवा ई-मेल द्वारे शोधा...", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryIndigo,
                                unfocusedBorderColor = BorderSubtle
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("registered_users_search_input")
                        )

                        // Role Filters (if administrative group)
                        if (group.isAdministrative) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChipCustom(
                                    title = "सर्व",
                                    isSelected = selectedRoleFilter == null,
                                    onClick = {
                                        selectedRoleFilter = null
                                        onRoleFilterChange(null)
                                    }
                                )
                                FilterChipCustom(
                                    title = "Admin",
                                    isSelected = selectedRoleFilter == "officer_admin",
                                    onClick = {
                                        selectedRoleFilter = "officer_admin"
                                        onRoleFilterChange("officer_admin")
                                    }
                                )
                                FilterChipCustom(
                                    title = "शाळा प्रशासक",
                                    isSelected = selectedRoleFilter == "school_admin",
                                    onClick = {
                                        selectedRoleFilter = "school_admin"
                                        onRoleFilterChange("school_admin")
                                    }
                                )
                                FilterChipCustom(
                                    title = "शिक्षक",
                                    isSelected = selectedRoleFilter == "teacher",
                                    onClick = {
                                        selectedRoleFilter = "teacher"
                                        onRoleFilterChange("teacher")
                                    }
                                )
                            }

                            // School Filter Dropdown
                            ExposedDropdownMenuBox(
                                expanded = isSchoolDropdownExpanded,
                                onExpandedChange = { isSchoolDropdownExpanded = !isSchoolDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val selectedSchoolName = activeSchools.firstOrNull { it.id == selectedSchoolFilter }?.name ?: "सर्व शाळा (All Schools)"
                                OutlinedTextField(
                                    value = selectedSchoolName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("शाळा फिल्टर (School Filter)", fontSize = 11.sp) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSchoolDropdownExpanded) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryIndigo,
                                        unfocusedBorderColor = BorderSubtle
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                )

                                ExposedDropdownMenu(
                                    expanded = isSchoolDropdownExpanded,
                                    onDismissRequest = { isSchoolDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("सर्व शाळा (All Schools)", fontSize = 13.sp) },
                                        onClick = {
                                            selectedSchoolFilter = null
                                            onSchoolFilterChange(null)
                                            isSchoolDropdownExpanded = false
                                        }
                                    )
                                    activeSchools.forEach { sch ->
                                        DropdownMenuItem(
                                            text = { Text(sch.name, fontSize = 13.sp) },
                                            onClick = {
                                                selectedSchoolFilter = sch.id
                                                onSchoolFilterChange(sch.id)
                                                isSchoolDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Eligible User List
                        if (eligibleUsers.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8F9FA),
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (group.isTeacherGroup) "अद्याप कोणतेही विद्यार्थी उपलब्ध नाहीत." else "कोणतेही पात्र वापरकर्ते आढळले नाहीत.",
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(eligibleUsers, key = { it.id }) { user ->
                                    val isSelected = selectedUserIds.contains(user.id)
                                    val school = activeSchools.firstOrNull { it.id == user.schoolId }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) PrimaryIndigoContainer.copy(alpha = 0.4f) else Color.White,
                                        border = BorderStroke(1.dp, if (isSelected) PrimaryIndigo else BorderSubtle),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onToggleSelectUser(user.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { onToggleSelectUser(user.id) },
                                                colors = CheckboxDefaults.colors(checkedColor = PrimaryIndigo)
                                            )

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = user.fullName ?: "अज्ञात",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = user.email ?: "",
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                                if (school != null) {
                                                    Text(
                                                        text = "🏫 ${school.name}",
                                                        fontSize = 10.sp,
                                                        color = TextTertiary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            RoleBadgeSmall(role = user.role)
                                        }
                                    }
                                }
                            }

                            // Add Selected Batch Action
                            Button(
                                onClick = onAddSelectedBatch,
                                enabled = selectedUserIds.isNotEmpty() && !isLoading,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("add_selected_batch_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(imageVector = Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "निवडलेले जोडा (${selectedUserIds.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Error message
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = Color(0xFFD32F2F)
                    )
                }

                // Footer Cancel
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("बंद करा", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun UserFoundCard(
    user: UserProfile,
    activeSchools: List<School>,
    isLoading: Boolean,
    onAdd: () -> Unit
) {
    val school = activeSchools.firstOrNull { it.id == user.schoolId }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = user.fullName ?: "अज्ञात",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = user.email ?: "",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    if (school != null) {
                        Text(
                            text = "🏫 ${school.name}",
                            fontSize = 11.sp,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                RoleBadgeSmall(role = user.role)
            }

            Button(
                onClick = onAdd,
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("add_searched_user_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("जोडा (Add)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun FilterChipCustom(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) PrimaryIndigo else Color.White,
        border = BorderStroke(1.dp, if (isSelected) PrimaryIndigo else BorderSubtle),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else TextSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun RoleBadgeSmall(role: String) {
    val (color, containerColor, label) = when (role.lowercase()) {
        "officer_admin" -> Triple(AccentAmber, AccentAmberContainer, "Admin")
        "school_admin" -> Triple(AccentAmber, AccentAmberContainer, "शाळा Admin")
        "teacher" -> Triple(PrimaryIndigo, PrimaryIndigoContainer, "शिक्षक")
        "student" -> Triple(SecondaryGreen, SecondaryGreenContainer, "विद्यार्थी")
        else -> Triple(TextSecondary, Color(0xFFF1F3F5), role)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = containerColor
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}
