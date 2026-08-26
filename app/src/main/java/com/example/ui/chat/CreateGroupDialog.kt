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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.GroupType
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

@Composable
fun CreateGroupDialog(
    userProfile: UserProfile?,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, groupType: String) -> Unit
) {
    val isOfficer = userProfile?.userRole == UserRole.OFFICER_ADMIN
    val isTeacher = userProfile?.userRole == UserRole.TEACHER

    val defaultType = if (isOfficer) GroupType.ADMINISTRATIVE else GroupType.TEACHER
    val defaultName = if (isOfficer) "प्रशासकीय गट" else "इयत्ता ५ वी - मराठी"

    var groupName by remember { mutableStateOf(defaultName) }
    var selectedType by remember { mutableStateOf(defaultType) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                    imageVector = Icons.Default.GroupAdd,
                                    contentDescription = null,
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "नवीन गट तयार करा",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isOfficer) "प्रशासकीय गट (System-wide)" else "वर्ग / विषय गट (School-scoped)",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "बंद करा",
                            tint = TextSecondary
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                // Group Type Selector (Only officer can choose administrative, teacher is locked to teacher)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "गट प्रकार (Group Type)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isOfficer) {
                            GroupTypeCard(
                                title = "प्रशासकीय गट",
                                subtitle = "अधिकारी, शाळा प्रशासक, शिक्षक",
                                isSelected = selectedType == GroupType.ADMINISTRATIVE,
                                icon = Icons.Default.AdminPanelSettings,
                                color = AccentAmber,
                                containerColor = AccentAmberContainer,
                                onClick = { selectedType = GroupType.ADMINISTRATIVE },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (isTeacher) {
                            GroupTypeCard(
                                title = "शिक्षक गट",
                                subtitle = "शिक्षक व वर्ग विद्यार्थी",
                                isSelected = selectedType == GroupType.TEACHER,
                                icon = Icons.Default.School,
                                color = PrimaryIndigo,
                                containerColor = PrimaryIndigoContainer,
                                onClick = { selectedType = GroupType.TEACHER },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Group Name Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "गटाचे नाव (Group Name)*",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = {
                            groupName = it
                            validationError = null
                        },
                        placeholder = { Text("उदा. प्रशासकीय गट किंवा इयत्ता ५ वी", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = BorderSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_name_input")
                    )
                }

                // Scope Info Note
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedType == GroupType.ADMINISTRATIVE) AccentAmberContainer.copy(alpha = 0.5f) else PrimaryIndigoContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (selectedType == GroupType.ADMINISTRATIVE)
                            "📌 हा गट सर्व शाळांसाठी (System-wide) असून यामध्ये अधिकारी, शाळा प्रशासक व शिक्षक सदस्य असू शकतात."
                        else
                            "📌 हा गट आपल्या शाळेतील विद्यार्थ्यांसाठी सुरक्षितपणे जोडला जाईल.",
                        fontSize = 12.sp,
                        color = if (selectedType == GroupType.ADMINISTRATIVE) TextPrimary else PrimaryIndigo,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // Error Message
                val displayError = validationError ?: errorMessage
                if (!displayError.isNullOrBlank()) {
                    Text(
                        text = displayError,
                        fontSize = 12.sp,
                        color = Color(0xFFD32F2F),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("रद्द करा", color = TextSecondary, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            val trimmed = groupName.trim()
                            if (trimmed.isBlank()) {
                                validationError = "कृपया गटाचे नाव प्रविष्ट करा."
                                return@Button
                            }
                            onConfirm(trimmed, selectedType.dbValue)
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("submit_create_group_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("तयार करा", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupTypeCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) containerColor else Color.White,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) color else BorderSubtle),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
