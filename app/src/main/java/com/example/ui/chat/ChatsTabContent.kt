package com.example.ui.chat

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Group
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
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

@Composable
fun ChatsTabContent(
    viewModel: ChatGroupViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadCurrentUserAndGroups()
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val currentRole = uiState.currentProfile?.userRole
    val canCreateGroup = currentRole == UserRole.OFFICER_ADMIN || currentRole == UserRole.TEACHER

    val filteredGroups = if (searchQuery.isBlank()) {
        uiState.groups
    } else {
        uiState.groups.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.typedGroupType.marathiTitle.contains(searchQuery, ignoreCase = true)
        }
    }

    if (uiState.activeChatGroup != null) {
        GroupChatScreen(
            group = uiState.activeChatGroup!!,
            messages = uiState.messages,
            members = uiState.selectedGroupMembers,
            currentUser = uiState.currentProfile,
            messageInput = uiState.messageInput,
            isLoading = uiState.isMessagesLoading,
            isSending = uiState.isSendingMessage,
            errorMessage = uiState.errorMessage,
            onBackClick = { viewModel.closeChatGroup() },
            onInfoClick = { viewModel.openGroupInfo() },
            onMessageInputChange = { viewModel.setMessageInput(it) },
            onSendMessage = { content -> viewModel.sendMessage(uiState.activeChatGroup!!.id, content) },
            onRetryLoadMessages = { viewModel.loadMessages(uiState.activeChatGroup!!.id) },
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Bar Action Area
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "चॅट्स (Chats)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "${filteredGroups.size} गट उपलब्ध",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    if (canCreateGroup) {
                        Button(
                            onClick = { viewModel.openCreateGroupDialog() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("create_group_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+ नवीन गट तयार करा",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Search Filter
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("गट शोधा...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "शोधा",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_search_input")
                )

                // Group List or Empty State
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryIndigo)
                    }
                } else if (filteredGroups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyGroupsView(
                            canCreateGroup = canCreateGroup,
                            onCreateClick = { viewModel.openCreateGroupDialog() }
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredGroups, key = { it.id }) { group ->
                            GroupCardItem(
                                group = group,
                                onClick = { viewModel.openChatGroup(group) }
                            )
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // Create Group Dialog
    if (uiState.showCreateDialog) {
        CreateGroupDialog(
            userProfile = uiState.currentProfile,
            isLoading = uiState.isActionLoading,
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.closeCreateGroupDialog() },
            onConfirm = { name, groupType ->
                viewModel.createGroup(name, groupType)
            }
        )
    }

    // Group Details Dialog
    if (uiState.showGroupDetailDialog && uiState.selectedGroup != null) {
        GroupDetailDialog(
            group = uiState.selectedGroup!!,
            members = uiState.selectedGroupMembers,
            currentUser = uiState.currentProfile,
            isLoading = uiState.isDetailLoading,
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.closeGroupDetail() },
            onOpenAddMember = { viewModel.openAddMemberDialog() },
            onRemoveMember = { userId -> viewModel.removeMember(userId) }
        )
    }

    // Add Member Dialog
    if (uiState.showAddMemberDialog && uiState.selectedGroup != null) {
        AddMemberDialog(
            group = uiState.selectedGroup!!,
            eligibleUsers = uiState.eligibleUsers,
            selectedUserIds = uiState.selectedUserIds,
            activeSchools = uiState.activeSchools,
            emailSearchQuery = uiState.emailSearchQuery,
            searchedUserByEmail = uiState.searchedUserByEmail,
            emailSearchMessage = uiState.emailSearchMessage,
            isLoading = uiState.isActionLoading,
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.closeAddMemberDialog() },
            onEmailSearch = { email -> viewModel.searchUserByEmail(email) },
            onToggleSelectUser = { userId -> viewModel.toggleUserSelection(userId) },
            onSearchQueryChange = { q -> viewModel.setSearchQuery(q) },
            onRoleFilterChange = { r -> viewModel.setRoleFilter(r) },
            onSchoolFilterChange = { s -> viewModel.setSchoolFilter(s) },
            onAddSingleUser = { userId -> viewModel.addSingleMember(userId) },
            onAddSelectedBatch = { viewModel.addSelectedBatchMembers() }
        )
    }
}

@Composable
private fun GroupCardItem(
    group: Group,
    onClick: () -> Unit
) {
    val isAdministrative = group.isAdministrative
    val icon = if (isAdministrative) Icons.Default.AdminPanelSettings else Icons.Default.School
    val color = if (isAdministrative) AccentAmber else PrimaryIndigo
    val containerColor = if (isAdministrative) AccentAmberContainer else PrimaryIndigoContainer

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("group_card_${group.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Group Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = group.name,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Group Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = group.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = containerColor
                    ) {
                        Text(
                            text = group.typedGroupType.marathiTitle,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "•  ${group.memberCount} सदस्य",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SecondaryGreenContainer
                    ) {
                        Text(
                            text = "सक्रिय",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryGreen,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EmptyGroupsView(
    canCreateGroup: Boolean,
    onCreateClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(PrimaryIndigoContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "अजून कोणताही गट उपलब्ध नाही.",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )

            Text(
                text = if (canCreateGroup)
                    "आपण अद्याप कोणत्याही गटाचे सदस्य नाही. संवादासाठी नवीन गट तयार करा."
                else
                    "आपण अद्याप कोणत्याही गटाचे सदस्य नाही. आपल्याला संबंधित प्रशासक किंवा शिक्षकांकडून गटात जोडले जाईल.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )

            if (canCreateGroup) {
                Button(
                    onClick = onCreateClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("empty_state_create_group_button")
                ) {
                    Icon(imageVector = Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ नवीन गट तयार करा", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
