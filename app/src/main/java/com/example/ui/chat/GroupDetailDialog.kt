package com.example.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Group
import com.example.data.model.GroupMember
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
fun GroupDetailDialog(
    group: Group,
    members: List<GroupMember>,
    currentUser: UserProfile?,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onOpenAddMember: () -> Unit,
    onRemoveMember: (String) -> Unit
) {
    val isOfficerAdmin = currentUser?.userRole == UserRole.OFFICER_ADMIN
    val isTeacherCreator = currentUser?.userRole == UserRole.TEACHER && group.createdBy == currentUser.id

    val canManageMembers = if (group.isAdministrative) isOfficerAdmin else isTeacherCreator

    val groupIcon = if (group.isAdministrative) Icons.Default.AdminPanelSettings else Icons.Default.School
    val groupColor = if (group.isAdministrative) AccentAmber else PrimaryIndigo
    val groupContainerColor = if (group.isAdministrative) AccentAmberContainer else PrimaryIndigoContainer

    var memberToRemove by remember { mutableStateOf<GroupMember?>(null) }

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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(groupContainerColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = groupIcon,
                                contentDescription = null,
                                tint = groupColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = group.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = groupContainerColor
                                ) {
                                    Text(
                                        text = group.typedGroupType.marathiTitle,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = groupColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (group.schoolName != null) {
                                    Text(
                                        text = "• ${group.schoolName}",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "बंद करा", tint = TextSecondary)
                    }
                }

                // Info banner: creator and scope
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8F9FA),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "निर्माता (Creator):", fontSize = 11.sp, color = TextSecondary)
                            Text(text = group.creatorName ?: "अज्ञात", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "एकूण सदस्य (Members):", fontSize = 11.sp, color = TextSecondary)
                            Text(text = "${members.size} सदस्य", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                        }
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                // Members Header + Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "सदस्य सूची (Members)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (canManageMembers) {
                        Button(
                            onClick = onOpenAddMember,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("open_add_member_button")
                        ) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ सदस्य जोडा", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Member list
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryIndigo, modifier = Modifier.size(28.dp))
                    }
                } else if (members.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8F9FA),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "या गटात अद्याप कोणतेही सदस्य नाहीत.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(members, key = { it.id }) { member ->
                            val profile = member.userProfile
                            val isOwner = member.roleInGroup == "owner" || member.userId == group.createdBy
                            val isSelf = member.userId == currentUser?.id

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8F9FA),
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(if (isOwner) AccentAmberContainer else PrimaryIndigoContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isOwner) Icons.Default.Star else Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            tint = if (isOwner) AccentAmber else PrimaryIndigo,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = profile?.fullName ?: "वापरकर्ता",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                            if (isOwner) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = AccentAmberContainer
                                                ) {
                                                    Text(
                                                        text = "मालक (Owner)",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = AccentAmber,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = profile?.email ?: "",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    if (profile != null) {
                                        RoleBadgeSmall(role = profile.role)
                                    }

                                    // Remove Member action (only authorized manager, cannot remove self/owner)
                                    if (canManageMembers && !isOwner && !isSelf) {
                                        IconButton(
                                            onClick = { memberToRemove = member },
                                            enabled = !isLoading,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("remove_member_button_${member.userId}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PersonRemove,
                                                contentDescription = "काढा (Remove)",
                                                tint = Color(0xFFD32F2F),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Error note if any
                if (!errorMessage.isNullOrBlank()) {
                    Text(text = errorMessage, fontSize = 12.sp, color = Color(0xFFD32F2F))
                }

                // Close button
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

    // Confirmation Dialog for Member Removal
    if (memberToRemove != null) {
        val targetMember = memberToRemove!!

        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = {
                Text(
                    text = "सदस्य काढून टाकायचा आहे का?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "सदस्य या ग्रुप मधून काढून टाकला जाईल. आपण खात्रीने पुढे जायचे आहे का?",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uid = targetMember.userId
                        memberToRemove = null
                        onRemoveMember(uid)
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_remove_member_button")
                ) {
                    Text("काढून टाका", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { memberToRemove = null },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.testTag("cancel_remove_member_button")
                ) {
                    Text("रद्द करा", fontSize = 12.sp, color = TextSecondary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("remove_member_confirmation_dialog")
        )
    }
}
