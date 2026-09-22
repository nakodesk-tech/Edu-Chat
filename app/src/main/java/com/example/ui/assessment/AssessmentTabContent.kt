package com.example.ui.assessment

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AssessmentQuestionInput
import com.example.data.model.AssessmentSummary
import com.example.data.model.Group
import com.example.data.model.UserRole
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentAmberContainer
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoContainer
import com.example.ui.theme.SecondaryGreen
import com.example.ui.theme.SecondaryGreenContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun AssessmentTabContent(
    viewModel: AssessmentViewModel,
    modifier: Modifier = Modifier,
    roleColor: Color = AccentAmber,
    roleContainerColor: Color = AccentAmberContainer
) {
    val state by viewModel.uiState.collectAsState()
    val role = viewModel.currentSession?.profile?.userRole
    val isStudent = role == UserRole.STUDENT
    val canCreate = role == UserRole.TEACHER || role == UserRole.SCHOOL_ADMIN || role == UserRole.OFFICER_ADMIN
    var showCreate by remember { mutableStateOf(false) }
    var shareTarget by remember { mutableStateOf<AssessmentSummary?>(null) }
    var filter by remember { mutableStateOf("सर्व") }

    if (state.selectedAssessment != null) {
        AssessmentRunner(
            assessment = state.selectedAssessment!!,
            questions = state.questions,
            answers = state.answers,
            isStudent = isStudent,
            isLoading = state.isActionLoading,
            errorMessage = state.errorMessage,
            onAnswer = viewModel::setAnswer,
            onSubmit = { viewModel.submitAssessment {} },
            onClose = viewModel::closeAssessment,
            modifier = modifier
        )
        return
    }

    val filtered = state.assessments.filter {
        val q = state.searchQuery.trim()
        val matchesSearch = q.isBlank() || it.title.contains(q, true) || it.subject.contains(q, true) || it.standard.contains(q, true)
        val attempt = state.attempts.firstOrNull { a -> a.assessmentId == it.id }
        val matchesFilter = when (filter) {
            "माझ्या चाचण्या" -> !isStudent && it.createdBy == viewModel.currentSession?.profile?.id
            "प्रकाशित" -> it.status == "published"
            "मसुदा" -> it.status == "draft"
            "सोडवलेल्या" -> isStudent && attempt != null
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("चाचण्या", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("शिका • तयार करा • मूल्यमापन करा", fontSize = 14.sp, color = TextSecondary)
            }
            if (canCreate) {
                Button(
                    onClick = { showCreate = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("नवीन चाचणी तयार करा", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::setSearchQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text("चाचणीचे नाव, विषय किंवा इयत्ता शोधा...") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) }
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val filters = if (isStudent) listOf("सर्व", "सोडवलेल्या", "प्रकाशित") else listOf("सर्व", "माझ्या चाचण्या", "प्रकाशित", "मसुदा")
            filters.forEach { value ->
                FilterChip(selected = filter == value, onClick = { filter = value }, label = { Text(value, fontSize = 11.sp) })
            }
            IconButton(onClick = { viewModel.load() }) {
                Icon(Icons.Default.FilterList, null, tint = TextSecondary)
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = roleColor) }
        } else if (filtered.isEmpty()) {
            EmptyAssessmentState(isStudent, roleColor, roleContainerColor)
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filtered, key = { it.id }) { assessment ->
                    val attempt = state.attempts.firstOrNull { a -> a.assessmentId == assessment.id }
                    AssessmentCard(
                        assessment = assessment,
                        isStudent = isStudent,
                        attemptScore = attempt?.score,
                        attemptTotal = attempt?.totalMarks,
                        roleColor = roleColor,
                        roleContainerColor = roleContainerColor,
                        onClick = {
                            if (isStudent) viewModel.openAssessmentForStudent(assessment)
                            else viewModel.openAssessment(assessment)
                        },
                        onShare = { shareTarget = assessment }
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreateAssessmentDialog(
            isLoading = state.isActionLoading,
            errorMessage = state.errorMessage,
            onDismiss = { if (!state.isActionLoading) showCreate = false },
            onCreate = { title, subject, standard, duration, questions ->
                viewModel.createAssessment(title, subject, standard, duration, questions) {
                    showCreate = false
                }
            }
        )
    }

    shareTarget?.let { assessment ->
        ShareAssessmentDialog(
            assessment = assessment,
            groups = state.groups,
            isLoading = state.isActionLoading,
            onDismiss = { if (!state.isActionLoading) shareTarget = null },
            onShare = { groupId -> viewModel.shareAssessment(assessment.id, groupId) { shareTarget = null } }
        )
    }
}

@Composable
private fun AssessmentCard(
    assessment: AssessmentSummary,
    isStudent: Boolean,
    attemptScore: Int?,
    attemptTotal: Int?,
    roleColor: Color,
    roleContainerColor: Color,
    onClick: () -> Unit,
    onShare: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val visual = subjectVisual(assessment.subject)
    val statusColor = when (assessment.status) {
        "published" -> SecondaryGreen
        "scheduled" -> Color(0xFFF59E0B)
        else -> Color(0xFF4F46E5)
    }
    val statusBg = when (assessment.status) {
        "published" -> SecondaryGreenContainer
        "scheduled" -> Color(0xFFFFF4D6)
        else -> Color(0xFFE7E9FF)
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(56.dp), RoundedCornerShape(16.dp), color = visual.second) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(visual.first, null, tint = roleColor, modifier = Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(assessment.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(assessment.subject + "  |  इयत्ता " + assessment.standard, fontSize = 12.sp, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetaItem(Icons.Default.Description, assessment.totalQuestions.toString() + " प्रश्न")
                    MetaItem(Icons.Default.AccessTime, assessment.durationMinutes.toString() + " मिनिटे")
                    if (isStudent && attemptScore != null) {
                        MetaItem(Icons.Default.BarChart, "गुण " + attemptScore + "/" + (attemptTotal ?: 0))
                    } else if (!isStudent) {
                        MetaItem(Icons.Default.People, assessment.sharedStudentCount.toString() + " विद्यार्थी दिली")
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(shape = RoundedCornerShape(10.dp), color = statusBg) {
                    Text(
                        if (isStudent && attemptScore != null) "सोडवली" else if (assessment.status == "published") "प्रकाशित" else if (assessment.status == "scheduled") "नियोजित" else "मसुदा",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
                Text(assessment.createdAt?.take(10) ?: "", fontSize = 10.sp, color = TextTertiary, modifier = Modifier.padding(top = 4.dp))
            }
            if (!isStudent) {
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, null, tint = TextTertiary) }
                    DropdownMenu(menuOpen, { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("गटात शेअर करा") },
                            leadingIcon = { Icon(Icons.Default.Send, null) },
                            onClick = { menuOpen = false; onShare() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = TextTertiary, modifier = Modifier.size(14.dp))
        Text(text, fontSize = 11.sp, color = TextSecondary)
    }
}

private fun subjectVisual(subject: String): Pair<ImageVector, Color> = when {
    subject.contains("गणित", true) -> Icons.Default.Calculate to Color(0xFFEAF0FF)
    subject.contains("विज्ञान", true) -> Icons.Default.Science to Color(0xFFE8F7EF)
    subject.contains("इंग्रजी", true) -> Icons.Default.InsertDriveFile to Color(0xFFFFF4D6)
    subject.contains("मराठी", true) -> Icons.Default.Book to Color(0xFFFFEAF1)
    else -> Icons.Default.Assessment to Color(0xFFF0EDFF)
}

@Composable
private fun EmptyAssessmentState(isStudent: Boolean, roleColor: Color, roleContainerColor: Color) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(Modifier.size(72.dp), RoundedCornerShape(22.dp), color = roleContainerColor) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Assessment, null, tint = roleColor, modifier = Modifier.size(36.dp)) }
            }
            Text(if (isStudent) "अजून कोणतीही चाचणी उपलब्ध नाही." else "अजून कोणतीही चाचणी तयार केलेली नाही.", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
            Text(
                if (isStudent) "शिक्षकांनी गटात शेअर केलेल्या चाचण्या येथे दिसतील." else "नवीन चाचणी तयार करून ती संबंधित गटात शेअर करा.",
                color = TextSecondary
            )
        }
    }
}

private data class QuestionDraft(
    var question: String = "",
    var optionA: String = "",
    var optionB: String = "",
    var optionC: String = "",
    var optionD: String = "",
    var correct: String = "A"
)

@Composable
private fun CreateAssessmentDialog(
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, Int, List<AssessmentQuestionInput>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var standard by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }
    val questions = remember { mutableStateListOf(QuestionDraft()) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(0.94f), RoundedCornerShape(22.dp), color = Color.White) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("नवीन चाचणी तयार करा", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    IconButton(onClick = onDismiss, enabled = !isLoading) { Icon(Icons.Default.Close, null) }
                }
                errorMessage?.let { Text(it, color = Color(0xFFDC2626), fontSize = 12.sp) }
                OutlinedTextField(title, { title = it }, label = { Text("चाचणीचे नाव *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(subject, { subject = it }, label = { Text("विषय *") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(standard, { standard = it }, label = { Text("इयत्ता *") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(duration, { duration = it.filter(Char::isDigit) }, label = { Text("वेळ (मिनिटे) *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                questions.forEachIndexed { index, draft ->
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("प्रश्न " + (index + 1), fontWeight = FontWeight.Bold, color = TextPrimary)
                                if (questions.size > 1) {
                                    TextButton(onClick = { questions.removeAt(index) }, enabled = !isLoading) {
                                        Text("काढा", color = Color(0xFFDC2626))
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = draft.question,
                                onValueChange = { draft.question = it },
                                label = { Text("प्रश्न *") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                            OptionField("A", draft.optionA, { draft.optionA = it }, draft.correct == "A") { draft.correct = "A" }
                            OptionField("B", draft.optionB, { draft.optionB = it }, draft.correct == "B") { draft.correct = "B" }
                            OptionField("C", draft.optionC, { draft.optionC = it }, draft.correct == "C") { draft.correct = "C" }
                            OptionField("D", draft.optionD, { draft.optionD = it }, draft.correct == "D") { draft.correct = "D" }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { questions.add(QuestionDraft()) },
                    enabled = !isLoading && questions.size < 50,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("आणखी प्रश्न जोडा")
                }

                Button(
                    onClick = {
                        val inputs = questions.mapIndexed { index, draft ->
                            AssessmentQuestionInput(
                                questionNo = index + 1,
                                questionText = draft.question.trim(),
                                options = mapOf(
                                    "A" to draft.optionA.trim(),
                                    "B" to draft.optionB.trim(),
                                    "C" to draft.optionC.trim(),
                                    "D" to draft.optionD.trim()
                                ),
                                correctOption = draft.correct
                            )
                        }
                        onCreate(title.trim(), subject.trim(), standard.trim(), duration.toIntOrNull() ?: 30, inputs)
                    },
                    enabled = !isLoading &&
                        title.isNotBlank() && subject.isNotBlank() && standard.isNotBlank() &&
                        questions.isNotEmpty() && questions.all {
                            it.question.isNotBlank() && it.optionA.isNotBlank() && it.optionB.isNotBlank() &&
                                it.optionC.isNotBlank() && it.optionD.isNotBlank()
                        },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    else Text("चाचणी तयार करा", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun OptionField(label: String, value: String, onValueChange: (String) -> Unit, selected: Boolean, onSelect: () -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("पर्याय " + label + " *") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        trailingIcon = { RadioButton(selected = selected, onClick = onSelect) }
    )
}

@Composable
private fun ShareAssessmentDialog(
    assessment: AssessmentSummary,
    groups: List<Group>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit
) {
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("गटात चाचणी शेअर करा", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(assessment.title, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                groups.forEach { group ->
                    Surface(
                        Modifier.fillMaxWidth().clickable { selectedGroup = group },
                        RoundedCornerShape(10.dp),
                        color = if (selectedGroup?.id == group.id) PrimaryIndigoContainer else Color(0xFFF8F8FA)
                    ) {
                        Text(group.name, Modifier.padding(12.dp), color = TextPrimary)
                    }
                }
                if (groups.isEmpty()) Text("शेअर करण्यासाठी कोणताही सक्रिय गट उपलब्ध नाही.", color = TextSecondary)
            }
        },
        confirmButton = {
            Button(onClick = { selectedGroup?.let(onShare) }, enabled = selectedGroup != null && !isLoading) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                else Text("शेअर करा")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("रद्द करा") } }
    )
}

@Composable
private fun AssessmentRunner(
    assessment: AssessmentSummary,
    questions: List<com.example.data.model.AssessmentQuestion>,
    answers: Map<String, String>,
    isStudent: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onAnswer: (Int, String) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier
) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Back") }
            Column(Modifier.weight(1f)) {
                Text(assessment.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    assessment.subject + " • इयत्ता " + assessment.standard + " • " + assessment.durationMinutes + " मिनिटे",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Icon(Icons.Default.Timer, null, tint = PrimaryIndigo)
        }
        errorMessage?.let { Text(it, color = Color(0xFFDC2626), fontSize = 12.sp) }
        if (questions.isEmpty() && isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(questions, key = { it.id }) { question ->
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(question.questionNo.toString() + ". " + question.questionText, fontWeight = FontWeight.Bold, color = TextPrimary)
                            listOf("A", "B", "C", "D").forEach { key ->
                                val option = question.options[key].orEmpty()
                                Row(
                                    Modifier.fillMaxWidth().clickable { if (isStudent) onAnswer(question.questionNo, key) }.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = answers[question.questionNo.toString()] == key,
                                        onClick = { if (isStudent) onAnswer(question.questionNo, key) }
                                    )
                                    Text(key + ". " + option, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
                if (isStudent && questions.isNotEmpty()) {
                    item {
                        Button(
                            onClick = onSubmit,
                            enabled = !isLoading && answers.size == questions.size,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            else Text("चाचणी जमा करा", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
