package com.example.ui.students

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.data.model.School
import com.example.data.model.UserProfile
import com.example.ui.theme.*

/**
 * Compact, polished Student Management UI following the existing Registered Users style.
 */
@Composable
fun StudentManagementContent(
    viewModel: StudentManagementViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundLight,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Header & Quick Stats Row
            StudentHeaderStats(
                totalCount = uiState.students.size,
                activeCount = uiState.students.count { it.isActive },
                inactiveCount = uiState.students.count { !it.isActive }
            )

            // 2. Register Student Action Card
            RegisterStudentActionCard(
                onClick = { viewModel.openDialog(StudentDialogState.Register) }
            )

            // 3. Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        text = "विद्यार्थ्याचे नाव, ईमेल, मोबाईल किंवा इयत्ता शोधा...",
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
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
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
                    focusedBorderColor = SecondaryGreen,
                    unfocusedBorderColor = BorderSubtle
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("students_search_input")
            )

            // 4. Standard / Class Filter Chips (All, 1st, 2nd, 3rd ... 10th)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "इयत्ता:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )

                StudentStandardUtils.FILTER_STANDARDS.forEach { std ->
                    val isSelected = uiState.selectedStandardFilter == std
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setStandardFilter(std) },
                        label = {
                            Text(
                                text = StudentStandardUtils.getFilterChipLabel(std),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SecondaryGreenContainer,
                            selectedLabelColor = SecondaryGreen,
                            containerColor = Color.White,
                            labelColor = TextSecondary
                        ),
                        border = BorderStroke(1.dp, if (isSelected) SecondaryGreen else BorderSubtle),
                        modifier = Modifier.testTag("std_filter_$std")
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Status Filters
                listOf("सर्व", "सक्रिय", "निष्क्रीय").forEach { status ->
                    val isSelected = uiState.selectedStatusFilter == status
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setStatusFilter(status) },
                        label = {
                            Text(
                                text = status,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (status == "निष्क्रीय") Color(0xFFFEE2E2) else SecondaryGreenContainer,
                            selectedLabelColor = if (status == "निष्क्रीय") Color(0xFFDC2626) else SecondaryGreen,
                            containerColor = Color.White,
                            labelColor = TextSecondary
                        ),
                        border = BorderStroke(1.dp, if (isSelected) SecondaryGreen else BorderSubtle),
                        modifier = Modifier.testTag("status_filter_$status")
                    )
                }
            }

            // 5. Students Content List
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = SecondaryGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else if (uiState.errorMessage != null) {
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
                            text = uiState.errorMessage ?: "",
                            fontSize = 14.sp,
                            color = Color(0xFFDC2626),
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(
                            onClick = { viewModel.loadStudents() },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, SecondaryGreen)
                        ) {
                            Text("पुन्हा प्रयत्न करा (Retry)", color = SecondaryGreen, fontSize = 13.sp)
                        }
                    }
                }
            } else if (uiState.filteredStudents.isEmpty()) {
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
                            color = SecondaryGreenContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = SecondaryGreen,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Text(
                            text = "या विभागात कोणतेही विद्यार्थी आढळले नाहीत.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        if (uiState.searchQuery.isNotEmpty()) {
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
                        .testTag("students_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(uiState.filteredStudents, key = { it.id }) { student ->
                        val schoolName = student.schoolId?.let { uiState.schoolsMap[it] }
                        CompactStudentCard(
                            student = student,
                            schoolName = schoolName,
                            onClick = { viewModel.openDialog(StudentDialogState.Detail(student)) },
                            onEdit = { viewModel.openDialog(StudentDialogState.Edit(student)) },
                            onToggleStatus = { viewModel.openDialog(StudentDialogState.ConfirmToggleStatus(student)) },
                            onDelete = { viewModel.openDialog(StudentDialogState.ConfirmDelete(student)) }
                        )
                    }
                }
            }
        }

        // Dialogs Handling
        when (val dialog = uiState.dialogState) {
            is StudentDialogState.Register -> {
                RegisterStudentDialog(
                    schools = uiState.schools,
                    currentSchoolId = uiState.currentSchoolId,
                    isOfficerAdmin = uiState.isOfficerAdmin,
                    isLoading = uiState.isActionLoading,
                    errorMessage = uiState.actionErrorMessage,
                    onDismiss = { viewModel.dismissDialog() },
                    onRegister = { name, email, pass, mobile, std, schId, academicYear ->
                        viewModel.registerStudent(name, email, pass, mobile, std, schId, academicYear)
                    }
                )
            }
            is StudentDialogState.Edit -> {
                EditStudentDialog(
                    student = dialog.user,
                    schools = uiState.schools,
                    isOfficerAdmin = uiState.isOfficerAdmin,
                    isLoading = uiState.isActionLoading,
                    errorMessage = uiState.actionErrorMessage,
                    onDismiss = { viewModel.dismissDialog() },
                    onSave = { name, mobile, std, schId, isActive ->
                        viewModel.updateStudent(dialog.user.id, name, mobile, std, schId, isActive)
                    }
                )
            }
            is StudentDialogState.ConfirmToggleStatus -> {
                ConfirmToggleStudentStatusDialog(
                    student = dialog.user,
                    isLoading = uiState.isActionLoading,
                    onDismiss = { viewModel.dismissDialog() },
                    onConfirm = { targetStatus ->
                        viewModel.toggleStatus(dialog.user, targetStatus)
                    }
                )
            }
            is StudentDialogState.ConfirmDelete -> {
                ConfirmDeleteStudentDialog(
                    student = dialog.user,
                    isLoading = uiState.isActionLoading,
                    onDismiss = { viewModel.dismissDialog() },
                    onConfirm = {
                        viewModel.deleteStudent(dialog.user)
                    }
                )
            }
            is StudentDialogState.Detail -> {
                val schoolName = dialog.user.schoolId?.let { uiState.schoolsMap[it] }
                StudentDetailDialog(
                    student = dialog.user,
                    schoolName = schoolName,
                    onDismiss = { viewModel.dismissDialog() }
                )
            }
            is StudentDialogState.None -> {}
        }
    }
}

/**
 * Top Header & Summary Stats
 */
@Composable
fun StudentHeaderStats(
    totalCount: Int,
    activeCount: Int,
    inactiveCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "विद्यार्थी व्यवस्थापन (Students)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "शाळेतील विद्यार्थ्यांची नोंदणी व नियंत्रण",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatItemCard(
                label = "एकूण विद्यार्थी",
                value = totalCount.toString(),
                containerColor = SecondaryGreenContainer,
                contentColor = SecondaryGreen,
                modifier = Modifier.weight(1f)
            )
            StatItemCard(
                label = "सक्रिय",
                value = activeCount.toString(),
                containerColor = Color(0xFFE8F5E9),
                contentColor = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
            StatItemCard(
                label = "निष्क्रीय",
                value = inactiveCount.toString(),
                containerColor = Color(0xFFFEE2E2),
                contentColor = Color(0xFFDC2626),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatItemCard(
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(0.5.dp, contentColor.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }
    }
}

/**
 * Register Student Action Card
 */
@Composable
fun RegisterStudentActionCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("tile_student_registration")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = SecondaryGreenContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "नवीन विद्यार्थी नोंदणी",
                        tint = SecondaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "नवीन विद्यार्थी नोंदणी (Register Student)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "विद्यार्थ्याचे नाव, इयत्ता, ईमेल आणि पासवर्डसह नवीन नोंदणी करा",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = SecondaryGreen,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Compact Student Card matching Registered Users style
 */
@Composable
fun CompactStudentCard(
    student: UserProfile,
    schoolName: String?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("student_card_${student.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. LEFT: Avatar
            Surface(
                shape = CircleShape,
                color = SecondaryGreenContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "विद्यार्थी",
                        tint = SecondaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 2. MIDDLE: Student Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Name
                Text(
                    text = student.fullName ?: "अनामिक विद्यार्थी",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Badges Row: Role + Standard + Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Role Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SecondaryGreenContainer,
                        border = BorderStroke(0.5.dp, SecondaryGreen.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "विद्यार्थी",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SecondaryGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Standard Badge
                    val standardText = StudentStandardUtils.parseStandard(student.standard) ?: student.standard
                    if (!standardText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryIndigoContainer,
                            border = BorderStroke(0.5.dp, PrimaryIndigo.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "इयत्ता $standardText",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryIndigo,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Section Badge (shown separately when available)
                    val sectionText = StudentStandardUtils.parseSection(student.standard)
                    if (!sectionText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(0.5.dp, Color(0xFFF59E0B).copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "तुकडी $sectionText",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Status Dot Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (student.isActive) Color(0xFFE8F5E9) else Color(0xFFFEE2E2),
                        border = BorderStroke(
                            0.5.dp,
                            if (student.isActive) SecondaryGreen.copy(alpha = 0.35f) else Color(0xFFEF4444).copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (student.isActive) SecondaryGreen else Color(0xFFDC2626))
                            )
                            Text(
                                text = if (student.isActive) "सक्रिय" else "निष्क्रीय",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (student.isActive) Color(0xFF2E7D32) else Color(0xFF991B1B)
                            )
                        }
                    }
                }

                // Email / Contact Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!student.email.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "ईमेल",
                                tint = TextTertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = student.email,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!student.mobile.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "फोन",
                                tint = TextTertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = student.mobile,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 3. RIGHT: Three-dot action menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("btn_student_menu_${student.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "पर्याय",
                        tint = TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("माहिती बदला (Edit)", fontSize = 13.sp) },
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
                        modifier = Modifier.testTag("menu_edit_student_${student.id}")
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (student.isActive) "निष्क्रीय करा" else "सक्रिय करा",
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (student.isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (student.isActive) AccentAmber else SecondaryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleStatus()
                        },
                        modifier = Modifier.testTag("menu_toggle_status_student_${student.id}")
                    )

                    DropdownMenuItem(
                        text = { Text("काढून टाका (Delete)", fontSize = 13.sp, color = Color(0xFFDC2626)) },
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
                        modifier = Modifier.testTag("menu_delete_student_${student.id}")
                    )
                }
            }
        }
    }
}

/**
 * Register Student Dialog with separate Standard (1st..10th) and Section (A..F) dropdowns,
 * and auto-fetched School & UDISE code display according to user role.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterStudentDialog(
    schools: List<School>,
    currentSchoolId: String?,
    isOfficerAdmin: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onRegister: (fullName: String, email: String, password: String, mobile: String?, standard: String?, schoolId: String, academicYear: String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var mobile by remember { mutableStateOf("") }
    var selectedAcademicYear by remember { mutableStateOf(StudentStandardUtils.DEFAULT_ACADEMIC_YEAR) }
    var academicYearDropdownExpanded by remember { mutableStateOf(false) }
    var selectedStandard by remember { mutableStateOf("10th") }
    var standardDropdownExpanded by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf("A") }
    var sectionDropdownExpanded by remember { mutableStateOf(false) }
    var selectedSchoolId by remember {
        mutableStateOf(currentSchoolId ?: schools.firstOrNull()?.id ?: "")
    }
    var schoolDropdownExpanded by remember { mutableStateOf(false) }

    val isFormValid = fullName.isNotBlank() &&
            email.isNotBlank() &&
            password.length >= 6 &&
            password == confirmPassword &&
            selectedSchoolId.isNotBlank() &&
            selectedAcademicYear.isNotBlank()

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "नवीन विद्यार्थी नोंदणी (Register Student)",
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = Color(0xFFDC2626)
                    )
                }

                // Full Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("विद्यार्थ्याचे पूर्ण नाव *") },
                    placeholder = { Text("उदा. प्रतीक सचिन मोरे") },
                    enabled = !isLoading,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecondaryGreen,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_student_fullname")
                )

                // Academic Year Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = academicYearDropdownExpanded,
                        onExpandedChange = { if (!isLoading) academicYearDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedAcademicYear,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("शैक्षणिक वर्ष (Academic Year) *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = academicYearDropdownExpanded) },
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SecondaryGreen,
                                unfocusedBorderColor = BorderSubtle
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("select_student_academic_year")
                        )
                        ExposedDropdownMenu(
                            expanded = academicYearDropdownExpanded,
                            onDismissRequest = { academicYearDropdownExpanded = false }
                        ) {
                            StudentStandardUtils.ACADEMIC_YEARS.forEach { year ->
                                DropdownMenuItem(
                                    text = { Text("वर्ष $year", fontSize = 13.sp) },
                                    onClick = {
                                        selectedAcademicYear = year
                                        academicYearDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Standard & Section 2 Dropdowns Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Standard Dropdown (1st..10th)
                    Box(modifier = Modifier.weight(1.2f)) {
                        ExposedDropdownMenuBox(
                            expanded = standardDropdownExpanded,
                            onExpandedChange = { if (!isLoading) standardDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = StudentStandardUtils.getStandardLabel(selectedStandard),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("इयत्ता *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = standardDropdownExpanded) },
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SecondaryGreen,
                                    unfocusedBorderColor = BorderSubtle
                                ),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("select_student_standard")
                            )
                            ExposedDropdownMenu(
                                expanded = standardDropdownExpanded,
                                onDismissRequest = { standardDropdownExpanded = false }
                            ) {
                                StudentStandardUtils.STANDARDS.forEach { std ->
                                    DropdownMenuItem(
                                        text = { Text(StudentStandardUtils.getStandardLabel(std), fontSize = 13.sp) },
                                        onClick = {
                                            selectedStandard = std
                                            standardDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Section Dropdown (A..F)
                    Box(modifier = Modifier.weight(1f)) {
                        ExposedDropdownMenuBox(
                            expanded = sectionDropdownExpanded,
                            onExpandedChange = { if (!isLoading) sectionDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = StudentStandardUtils.getSectionLabel(selectedSection),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("तुकडी *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionDropdownExpanded) },
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SecondaryGreen,
                                    unfocusedBorderColor = BorderSubtle
                                ),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("select_student_section")
                            )
                            ExposedDropdownMenu(
                                expanded = sectionDropdownExpanded,
                                onDismissRequest = { sectionDropdownExpanded = false }
                            ) {
                                StudentStandardUtils.SECTIONS.forEach { sec ->
                                    DropdownMenuItem(
                                        text = { Text(StudentStandardUtils.getSectionLabel(sec), fontSize = 13.sp) },
                                        onClick = {
                                            selectedSection = sec
                                            sectionDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("ईमेल पत्ता *") },
                    placeholder = { Text("उदा. student@educhat.edu") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    enabled = !isLoading,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecondaryGreen,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_student_email")
                )

                // Mobile
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("मोबाईल क्रमांक (पर्यायी)") },
                    placeholder = { Text("उदा. 9822012345") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    enabled = !isLoading,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecondaryGreen,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_student_mobile")
                )

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("पासवर्ड * (किमान ६ अक्षरे)") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    },
                    enabled = !isLoading,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecondaryGreen,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_student_password")
                )

                // Confirm Password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("पासवर्ड पुष्टी करा *") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                    enabled = !isLoading,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecondaryGreen,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_student_confirm_password")
                )

                // School and UDISE Display / Selector
                val assignedSchool = schools.firstOrNull { it.id == selectedSchoolId }
                if (isOfficerAdmin && schools.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = schoolDropdownExpanded,
                        onExpandedChange = { if (!isLoading) schoolDropdownExpanded = it }
                    ) {
                        val selectedName = assignedSchool?.let { "${it.name} (${it.code})" } ?: "शाळा निवडा"
                        OutlinedTextField(
                            value = selectedName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("शाळा निवडा (School & UDISE) *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = schoolDropdownExpanded) },
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SecondaryGreen,
                                unfocusedBorderColor = BorderSubtle
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("select_student_school")
                        )
                        ExposedDropdownMenu(
                            expanded = schoolDropdownExpanded,
                            onDismissRequest = { schoolDropdownExpanded = false }
                        ) {
                            schools.forEach { school ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(school.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Text("UDISE / कोड: ${school.code}", fontSize = 11.sp, color = TextSecondary)
                                        }
                                    },
                                    onClick = {
                                        selectedSchoolId = school.id
                                        schoolDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else if (assignedSchool != null) {
                    // Auto-fetched school and UDISE for Teacher / School Admin
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = SecondaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = assignedSchool.name,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "UDISE कोड: ${assignedSchool.code} • शाळा स्वयंचलित निश्चित",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onRegister(
                        fullName.trim(),
                        email.trim(),
                        password,
                        mobile.trim(),
                        StudentStandardUtils.formatStoredStandard(selectedStandard, selectedSection),
                        selectedSchoolId,
                        selectedAcademicYear.trim()
                    )
                },
                enabled = !isLoading && isFormValid,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryGreen,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("btn_submit_student_register")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("नोंदणी करा (Register)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
 * Edit Student Dialog with separate Standard (1st..10th) and Section (A..F) dropdowns
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStudentDialog(
    student: UserProfile,
    schools: List<School>,
    isOfficerAdmin: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (fullName: String, mobile: String, standard: String, schoolId: String?, isActive: Boolean) -> Unit
) {
    var fullName by remember(student) { mutableStateOf(student.fullName ?: "") }
    var mobile by remember(student) { mutableStateOf(student.mobile ?: "") }
    var selectedStandard by remember(student) {
        mutableStateOf(StudentStandardUtils.parseStandard(student.standard) ?: "10th")
    }
    var standardDropdownExpanded by remember { mutableStateOf(false) }
    var selectedSection by remember(student) {
        mutableStateOf(StudentStandardUtils.parseSection(student.standard) ?: "A")
    }
    var sectionDropdownExpanded by remember { mutableStateOf(false) }
    var isActive by remember(student) { mutableStateOf(student.isActive) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "विद्यार्थी माहिती संपादित करा (Edit Student)",
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    value = student.email ?: "",
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
                    label = { Text("पूर्ण नाव (Full Name) *") },
                    enabled = !isLoading,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecondaryGreen,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_student_name")
                )

                // Standard & Section 2 Dropdowns Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Standard Dropdown (1st..10th)
                    Box(modifier = Modifier.weight(1.2f)) {
                        ExposedDropdownMenuBox(
                            expanded = standardDropdownExpanded,
                            onExpandedChange = { if (!isLoading) standardDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = StudentStandardUtils.getStandardLabel(selectedStandard),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("इयत्ता *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = standardDropdownExpanded) },
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SecondaryGreen,
                                    unfocusedBorderColor = BorderSubtle
                                ),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("input_edit_student_standard")
                            )
                            ExposedDropdownMenu(
                                expanded = standardDropdownExpanded,
                                onDismissRequest = { standardDropdownExpanded = false }
                            ) {
                                StudentStandardUtils.STANDARDS.forEach { std ->
                                    DropdownMenuItem(
                                        text = { Text(StudentStandardUtils.getStandardLabel(std), fontSize = 13.sp) },
                                        onClick = {
                                            selectedStandard = std
                                            standardDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Section Dropdown (A..F)
                    Box(modifier = Modifier.weight(1f)) {
                        ExposedDropdownMenuBox(
                            expanded = sectionDropdownExpanded,
                            onExpandedChange = { if (!isLoading) sectionDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = StudentStandardUtils.getSectionLabel(selectedSection),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("तुकडी *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionDropdownExpanded) },
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SecondaryGreen,
                                    unfocusedBorderColor = BorderSubtle
                                ),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("input_edit_student_section")
                            )
                            ExposedDropdownMenu(
                                expanded = sectionDropdownExpanded,
                                onDismissRequest = { sectionDropdownExpanded = false }
                            ) {
                                StudentStandardUtils.SECTIONS.forEach { sec ->
                                    DropdownMenuItem(
                                        text = { Text(StudentStandardUtils.getSectionLabel(sec), fontSize = 13.sp) },
                                        onClick = {
                                            selectedSection = sec
                                            sectionDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Mobile
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("मोबाईल क्रमांक (Mobile)") },
                    enabled = !isLoading,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecondaryGreen,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_student_mobile")
                )

                // Active Status Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isActive) "खाते स्थिती: सक्रिय (Active)" else "खाते स्थिती: निष्क्रीय (Inactive)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isActive) Color(0xFF2E7D32) else Color(0xFFDC2626)
                    )
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        enabled = !isLoading,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SecondaryGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFD1D5DB)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        fullName.trim(),
                        mobile.trim(),
                        StudentStandardUtils.formatStoredStandard(selectedStandard, selectedSection),
                        student.schoolId,
                        isActive
                    )
                },
                enabled = !isLoading && fullName.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryGreen,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("btn_save_student_edit")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("जतन करा (Save)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
 * Confirmation Dialog for Active / Inactive Status Toggle
 */
@Composable
fun ConfirmToggleStudentStatusDialog(
    student: UserProfile,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    val targetStatus = !student.isActive
    val actionText = if (targetStatus) "सक्रिय करा" else "निष्क्रीय करा"

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "विद्यार्थी खाते स्थिती बदला ($actionText)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Text(
                text = "आपण '${student.fullName ?: student.email}' चे खाते $actionText इच्छिता का? आपण कधीही ही स्थिती बदलू शकता.",
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
                modifier = Modifier.testTag("btn_confirm_toggle_student_status")
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
 * Destructive Confirmation Dialog for Deleting Student
 */
@Composable
fun ConfirmDeleteStudentDialog(
    student: UserProfile,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "विद्यार्थी कायमचा काढून टाकायचा आहे का?",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFDC2626)
            )
        },
        text = {
            Text(
                text = "हा विद्यार्थी (${student.fullName ?: student.email}) यादीतून काढून टाकला जाईल. ही क्रिया पूर्ववत करता येणार नाही. खात्री करा.",
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
                modifier = Modifier.testTag("btn_confirm_delete_student")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("काढून टाका (Delete)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
 * Student Detail Dialog
 */
@Composable
fun StudentDetailDialog(
    student: UserProfile,
    schoolName: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            val parsedStd = StudentStandardUtils.parseStandard(student.standard) ?: student.standard
            val parsedSec = StudentStandardUtils.parseSection(student.standard)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = SecondaryGreenContainer,
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = SecondaryGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = student.fullName ?: "अनामिक विद्यार्थी",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                // Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SecondaryGreenContainer,
                        border = BorderStroke(0.5.dp, SecondaryGreen.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "विद्यार्थी (Student)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SecondaryGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (!parsedStd.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryIndigoContainer,
                            border = BorderStroke(0.5.dp, PrimaryIndigo.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "इयत्ता $parsedStd",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryIndigo,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (!parsedSec.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(0.5.dp, Color(0xFFF59E0B).copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "तुकडी $parsedSec",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (student.isActive) Color(0xFFE8F5E9) else Color(0xFFFEE2E2),
                        border = BorderStroke(
                            0.5.dp,
                            if (student.isActive) SecondaryGreen.copy(alpha = 0.35f) else Color(0xFFEF4444).copy(alpha = 0.35f)
                        )
                    ) {
                        Text(
                            text = if (student.isActive) "सक्रिय" else "निष्क्रीय",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (student.isActive) Color(0xFF2E7D32) else Color(0xFF991B1B),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 4.dp))

                // Detail Rows
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!student.email.isNullOrBlank()) {
                        DetailItemRow(icon = Icons.Default.Email, label = "ईमेल:", value = student.email)
                    }
                    if (!student.mobile.isNullOrBlank()) {
                        DetailItemRow(icon = Icons.Default.Phone, label = "मोबाईल:", value = student.mobile)
                    }
                    if (!parsedStd.isNullOrBlank()) {
                        DetailItemRow(icon = Icons.Default.Class, label = "इयत्ता:", value = StudentStandardUtils.getStandardLabel(parsedStd))
                    }
                    if (!parsedSec.isNullOrBlank()) {
                        DetailItemRow(icon = Icons.Default.Grade, label = "तुकडी:", value = StudentStandardUtils.getSectionLabel(parsedSec))
                    }
                    if (!schoolName.isNullOrBlank()) {
                        DetailItemRow(icon = Icons.Default.AccountBalance, label = "शाळा:", value = schoolName)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen)
            ) {
                Text("बंद करा", color = Color.White, fontSize = 13.sp)
            }
        },
        shape = RoundedCornerShape(18.dp),
        containerColor = Color.White
    )
}

@Composable
private fun DetailItemRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
