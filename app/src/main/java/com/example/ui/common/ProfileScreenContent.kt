package com.example.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.model.UserRole

/**
 * Premium Profile Screen Content matching reference UI closely.
 */
@Composable
fun ProfileScreenContent(
    profile: UserProfile?,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFC))
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header: Profile Icon + Title & Subtitle + Logout Button
        ProfileHeaderSection(
            onLogout = onLogout
        )

        // 2. Profile Summary Card (Hero Card)
        ProfileSummaryCard(
            profile = profile
        )

        // 3. Profile Details Card (Icon + Label + Value rows)
        ProfileDetailsCard(
            profile = profile
        )

        // 4. Action Button: Edit Profile
        Button(
            onClick = onEditProfile,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("officer_edit_profile_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
            shape = RoundedCornerShape(14.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "माहिती बदला (Edit Profile)",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )
            )
        }

        // 5. Footer Contribution Badge
        ProfileFooterContribution()

        // Small bottom spacer for smooth scrolling above navigation bar
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Top Header: Profile avatar icon, Marathi title & subtitle, and logout button.
 */
@Composable
private fun ProfileHeaderSection(
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Yellow circular profile icon
            Surface(
                shape = CircleShape,
                color = Color(0xFFFEF3C7),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "माझे प्रोफाइल",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3730A3),
                        fontSize = 24.sp
                    )
                )
                Text(
                    text = "तुमची माहिती आणि खाते तपशील",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        fontSize = 13.sp
                    )
                )
            }
        }

        // Rounded white logout button
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
            shadowElevation = 1.dp,
            modifier = Modifier
                .size(46.dp)
                .testTag("officer_logout_button")
        ) {
            IconButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Logout",
                    tint = Color(0xFF4F46E5),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Hero Card: User avatar, name, email and Primary Admin / Role badge with dot grid texture.
 */
@Composable
private fun ProfileSummaryCard(
    profile: UserProfile?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("officer_header_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F0FE)),
        border = BorderStroke(1.dp, Color(0xFFE4DCFD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Decorative background dots at bottom right
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(width = 88.dp, height = 42.dp)
            ) {
                val dotRadius = 1.8.dp.toPx()
                val stepX = 11.dp.toPx()
                val stepY = 10.dp.toPx()
                for (col in 0..7) {
                    for (row in 0..3) {
                        drawCircle(
                            color = Color(0xFFC4B5FD).copy(alpha = 0.40f),
                            radius = dotRadius,
                            center = Offset(col * stepX, row * stepY)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // User Avatar with warm double ring
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFDE68A),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                // Shield/Admin Icon in warm amber tone
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Avatar",
                                    tint = Color(0xFF92400E),
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }
                }

                // Name and Email
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = profile?.fullName?.ifBlank { "User Profile" } ?: "Sachin Kacharu Nakode",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1B4B),
                            fontSize = 17.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = profile?.email?.ifBlank { "-" } ?: "nakodesk@gmail.com",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280),
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Role Badge (Gold / Yellow Pill)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFDE68A),
                    border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = when {
                                profile?.isPrimaryAdmin == true -> "PRIMARY\nADMIN"
                                profile?.userRole == UserRole.OFFICER_ADMIN -> "OFFICER\nADMIN"
                                profile?.userRole == UserRole.SCHOOL_ADMIN -> "SCHOOL\nADMIN"
                                profile?.userRole == UserRole.TEACHER -> "TEACHER"
                                else -> "PRIMARY\nADMIN"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.5.sp,
                                color = Color(0xFF1E3A8A),
                                lineHeight = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * "प्रोफाइल तपशील" Card with clean individual row items for Name, Email, Mobile and Role.
 */
@Composable
private fun ProfileDetailsCard(
    profile: UserProfile?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Icon + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEEF2FF),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "प्रोफाइल तपशील",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3730A3),
                        fontSize = 16.sp
                    )
                )
            }

            HorizontalDivider(
                color = Color(0xFFF1F5F9),
                thickness = 1.dp
            )

            // Row 1: Name (नाव)
            ProfileDetailItem(
                icon = Icons.Default.Person,
                label = "नाव",
                value = profile?.fullName?.ifBlank { "-" } ?: "Sachin Kacharu Nakode"
            )

            // Row 2: Email (ईमेल)
            ProfileDetailItem(
                icon = Icons.Default.Email,
                label = "ईमेल",
                value = profile?.email?.ifBlank { "-" } ?: "nakodesk@gmail.com"
            )

            // Row 3: Mobile (मोबाईल)
            ProfileDetailItem(
                icon = Icons.Default.Phone,
                label = "मोबाईल",
                value = profile?.mobile?.ifBlank { "-" } ?: "-"
            )

            // Row 4: Role (भूमिका)
            val rolePrimaryMarathi = when {
                profile?.isPrimaryAdmin == true -> "प्राथमिक अधिकारी प्रशासक"
                profile?.userRole == UserRole.OFFICER_ADMIN -> "अधिकारी प्रशासक"
                profile?.userRole == UserRole.SCHOOL_ADMIN -> "शाळा प्रशासक"
                profile?.userRole == UserRole.TEACHER -> "शिक्षक"
                else -> "प्राथमिक अधिकारी प्रशासक"
            }
            val roleEnglishSubtitle = when {
                profile?.isPrimaryAdmin == true -> "(Primary Officer Admin)"
                profile?.userRole == UserRole.OFFICER_ADMIN -> "(Officer Admin)"
                profile?.userRole == UserRole.SCHOOL_ADMIN -> "(School Admin)"
                profile?.userRole == UserRole.TEACHER -> "(Teacher)"
                else -> "(Primary Officer Admin)"
            }

            ProfileDetailItem(
                icon = Icons.Default.BusinessCenter,
                label = "भूमिका",
                value = rolePrimaryMarathi,
                subValue = roleEnglishSubtitle
            )
        }
    }
}

/**
 * Cleanly styled row container for each profile item.
 */
@Composable
private fun ProfileDetailItem(
    icon: ImageVector,
    label: String,
    value: String,
    subValue: String? = null
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFFFFFF),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon container
            Surface(
                shape = CircleShape,
                color = Color(0xFFEEF2FF),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Label & Value
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp
                    )
                )

                if (!subValue.isNullOrBlank()) {
                    Text(
                        text = subValue,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}

/**
 * Footer contribution box:
 * "Created in ♥️ with Teacher and Students by Sachin Nakode"
 */
@Composable
private fun ProfileFooterContribution() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF5F3FF),
        border = BorderStroke(1.dp, Color(0xFFEDE9FE)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left decorative leaf branch
            DecorativeFoliageBranch(isLeft = true)

            // Center Heart & Contribution Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Purple circle with heart icon
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF4F46E5),
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Love",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Contribution text
                val contributionText = buildAnnotatedString {
                    append("Created in ")
                    withStyle(SpanStyle(color = Color(0xFFEF4444))) {
                        append("♥️")
                    }
                    append(" with ")
                    withStyle(SpanStyle(color = Color(0xFF475569), fontWeight = FontWeight.Medium)) {
                        append("Teacher and Students")
                    }
                    append(" by ")
                    withStyle(SpanStyle(color = Color(0xFF4338CA), fontWeight = FontWeight.Bold)) {
                        append("Sachin Nakode")
                    }
                }

                Text(
                    text = contributionText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                )
            }

            // Right decorative leaf branch
            DecorativeFoliageBranch(isLeft = false)
        }
    }
}

/**
 * Subtle aesthetic leaf vector drawing on footer sides.
 */
@Composable
private fun DecorativeFoliageBranch(
    isLeft: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(width = 24.dp, height = 36.dp)
    ) {
        val branchColor = Color(0xFFDDD6FE)
        val leafColor = Color(0xFFC4B5FD).copy(alpha = 0.8f)

        val startX = if (isLeft) size.width * 0.7f else size.width * 0.3f
        val endX = if (isLeft) size.width * 0.3f else size.width * 0.7f

        // Stem
        val stemPath = Path().apply {
            moveTo(startX, size.height * 0.95f)
            quadraticTo(
                size.width * 0.5f,
                size.height * 0.5f,
                endX,
                size.height * 0.05f
            )
        }
        drawPath(stemPath, color = branchColor, style = Stroke(width = 1.5.dp.toPx()))

        // Leaves along the stem
        val leaf1Center = Offset(size.width * 0.35f, size.height * 0.35f)
        val leaf2Center = Offset(size.width * 0.65f, size.height * 0.55f)
        val leaf3Center = Offset(size.width * 0.40f, size.height * 0.75f)

        drawCircle(color = leafColor, radius = 3.5.dp.toPx(), center = leaf1Center)
        drawCircle(color = leafColor, radius = 3.2.dp.toPx(), center = leaf2Center)
        drawCircle(color = leafColor, radius = 3.0.dp.toPx(), center = leaf3Center)
    }
}
