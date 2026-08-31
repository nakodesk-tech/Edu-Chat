package com.example.ui.chat

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.ChatMessage
import com.example.data.model.Group
import com.example.data.model.GroupMember
import com.example.data.model.UserProfile
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentAmberContainer
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.OnPrimaryIndigoContainer
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PrimaryIndigoContainer
import com.example.ui.theme.SurfaceLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GroupChatScreen(
    group: Group,
    messages: List<ChatMessage>,
    members: List<GroupMember>,
    currentUser: UserProfile?,
    messageInput: String,
    isLoading: Boolean,
    isSending: Boolean,
    errorMessage: String?,
    imageUploadState: ChatImageUploadState = ChatImageUploadState.Idle,
    onBackClick: () -> Unit,
    onInfoClick: () -> Unit,
    onMessageInputChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onSendImage: (Uri) -> Unit = {},
    onRetryImageUpload: () -> Unit = {},
    onDismissImageUpload: () -> Unit = {},
    onRetryLoadMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        onBackClick()
    }

    val listState = rememberLazyListState()
    val activeMessages = remember(messages) {
        messages.filter { !it.isDeleted }
    }

    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onSendImage(uri)
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(activeMessages.size) {
        if (activeMessages.isNotEmpty()) {
            listState.animateScrollToItem(activeMessages.size - 1)
        }
    }

    val isAdministrative = group.isAdministrative
    val headerIcon = if (isAdministrative) Icons.Default.AdminPanelSettings else Icons.Default.School
    val headerIconTint = if (isAdministrative) AccentAmber else PrimaryIndigo
    val headerIconContainer = if (isAdministrative) AccentAmberContainer else PrimaryIndigoContainer

    val memberCountText = if (members.isNotEmpty()) {
        "${members.size} सदस्य"
    } else {
        group.typedGroupType.marathiTitle
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .imePadding()
    ) {
        // TOP CHAT HEADER
        Surface(
            color = SurfaceLight,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("chat_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "मागे जा",
                        tint = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(headerIconContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = headerIcon,
                        contentDescription = null,
                        tint = headerIconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = group.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = memberCountText,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.testTag("chat_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "गट माहिती",
                        tint = PrimaryIndigo
                    )
                }
            }
        }

        // CONVERSATION AREA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (isLoading && activeMessages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryIndigo,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "संदेश लोड होत आहेत...",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else if (errorMessage != null && activeMessages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = errorMessage,
                                fontSize = 13.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Button(
                                onClick = onRetryLoadMessages,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("पुन्हा प्रयत्न करा", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else if (activeMessages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryIndigoContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = "अजून कोणताही संदेश नाही.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "पहिला संदेश किंवा चित्र पाठवा.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(activeMessages, key = { _, item -> item.id }) { index, message ->
                        val isOutgoing = currentUser?.id != null && message.senderId == currentUser.id
                        val prevMessage = if (index > 0) activeMessages[index - 1] else null
                        val isSameSenderAsPrev = prevMessage != null && prevMessage.senderId == message.senderId

                        ChatMessageBubble(
                            message = message,
                            isOutgoing = isOutgoing,
                            showSenderName = !isOutgoing && !isSameSenderAsPrev,
                            onImageClick = { url -> previewImageUrl = url }
                        )
                    }
                }
            }
        }

        // IMAGE UPLOAD PROGRESS / FAILURE BANNER
        when (imageUploadState) {
            is ChatImageUploadState.Uploading -> {
                Surface(
                    color = PrimaryIndigoContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryIndigo,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = imageUploadState.progressMessage,
                            fontSize = 12.5.sp,
                            color = OnPrimaryIndigoContainer,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            is ChatImageUploadState.Failed -> {
                Surface(
                    color = Color(0xFFFEE2E2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = imageUploadState.errorMessage,
                            fontSize = 12.sp,
                            color = Color(0xFF991B1B),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = onRetryImageUpload,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("पुन्हा प्रयत्न", fontSize = 11.sp, color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDismissImageUpload,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "बंद करा",
                                tint = Color(0xFF991B1B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            ChatImageUploadState.Idle -> { /* No banner */ }
        }

        // BOTTOM MESSAGE COMPOSER
        Surface(
            color = SurfaceLight,
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ATTACH PHOTO BUTTON
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = imageUploadState !is ChatImageUploadState.Uploading && !isSending,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("chat_attach_image_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "चित्र जोडा",
                        tint = if (imageUploadState is ChatImageUploadState.Uploading) Color(0xFF94A3B8) else PrimaryIndigo,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedTextField(
                    value = messageInput,
                    onValueChange = onMessageInputChange,
                    placeholder = {
                        Text(
                            text = "संदेश टाइप करा...",
                            fontSize = 13.sp,
                            color = TextTertiary
                        )
                    },
                    maxLines = 4,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = Color(0xFFF9FAFB),
                        unfocusedContainerColor = Color(0xFFF9FAFB)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input")
                )

                Spacer(modifier = Modifier.width(6.dp))

                val canSend = messageInput.trim().isNotBlank() && !isSending

                IconButton(
                    onClick = {
                        if (canSend) {
                            onSendMessage(messageInput)
                        }
                    },
                    enabled = canSend,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (canSend) PrimaryIndigo else Color(0xFFE2E8F0),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFE2E8F0),
                        disabledContentColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .testTag("chat_send_button")
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = PrimaryIndigo,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "संदेश पाठवा",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // FULL SCREEN IMAGE PREVIEW DIALOG
    previewImageUrl?.let { url ->
        Dialog(
            onDismissRequest = { previewImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = "चित्र पूर्वावलोकन",
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(36.dp))
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "चित्र लोड करण्यात अयशस्वी झाले",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )

                IconButton(
                    onClick = { previewImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 40.dp, end = 16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "बंद करा",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isOutgoing: Boolean,
    showSenderName: Boolean,
    onImageClick: (String) -> Unit = {}
) {
    val senderName = message.senderProfile?.fullName ?: "सदस्य"
    val timeFormatted = remember(message.createdAt) {
        formatMessageTime(message.createdAt)
    }

    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    val bubbleBackground = if (isOutgoing) PrimaryIndigoContainer else Color.White
    val textColor = if (isOutgoing) OnPrimaryIndigoContainer else TextPrimary
    val bubbleBorder = if (isOutgoing) null else BorderStroke(1.dp, BorderSubtle)

    val isImage = message.isImageMessage && !message.mediaUrl.isNullOrBlank()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = if (isImage) 260.dp else 280.dp),
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
        ) {
            Card(
                shape = bubbleShape,
                colors = CardDefaults.cardColors(containerColor = bubbleBackground),
                border = bubbleBorder,
                elevation = CardDefaults.cardElevation(defaultElevation = if (isOutgoing) 0.5.dp else 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = if (isImage) 6.dp else 12.dp,
                        vertical = if (isImage) 6.dp else 8.dp
                    )
                ) {
                    if (showSenderName) {
                        Text(
                            text = senderName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigo,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = if (isImage) Modifier.padding(horizontal = 4.dp, vertical = 2.dp) else Modifier
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    if (isImage) {
                        val mediaUrl = message.mediaUrl!!
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE2E8F0))
                                .clickable { onImageClick(mediaUrl) }
                        ) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(mediaUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "गट चित्र संदेश",
                                contentScale = ContentScale.Crop,
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .background(Color(0xFFE2E8F0)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = PrimaryIndigo,
                                            strokeWidth = 2.5.dp,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .background(Color(0xFFF1F5F9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BrokenImage,
                                                contentDescription = null,
                                                tint = TextTertiary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Text(
                                                text = "चित्र लोड झाले नाही",
                                                fontSize = 11.sp,
                                                color = TextTertiary
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                        }

                        if (message.content.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message.content,
                                fontSize = 13.5.sp,
                                color = textColor,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = message.content,
                            fontSize = 14.sp,
                            color = textColor,
                            lineHeight = 19.sp
                        )
                    }

                    if (timeFormatted.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = timeFormatted,
                            fontSize = 9.5.sp,
                            color = if (isOutgoing) PrimaryIndigo.copy(alpha = 0.7f) else TextTertiary,
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(end = if (isImage) 4.dp else 0.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatMessageTime(isoString: String?): String {
    if (isoString.isNullOrBlank()) return ""
    return try {
        val instant = Instant.parse(isoString)
        val zoneId = ZoneId.systemDefault()
        val localTime = instant.atZone(zoneId).toLocalTime()
        val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
        localTime.format(formatter)
    } catch (e: Exception) {
        try {
            val offset = OffsetDateTime.parse(isoString)
            val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
            offset.format(formatter)
        } catch (e2: Exception) {
            if (isoString.length >= 16 && isoString.contains("T")) {
                isoString.substring(11, 16)
            } else {
                ""
            }
        }
    }
}
