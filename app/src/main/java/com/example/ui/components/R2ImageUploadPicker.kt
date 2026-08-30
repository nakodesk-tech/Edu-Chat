package com.example.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.R2ImageUploadResult
import com.example.data.repository.R2ImageUploadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed interface ImageUploadUiState {
    object Idle : ImageUploadUiState
    data class Processing(val uri: Uri, val message: String = "चित्र प्रक्रिया सुरू आहे...") : ImageUploadUiState
    data class Uploading(val uri: Uri, val message: String = "R2 वर अपलोड सुरू आहे...") : ImageUploadUiState
    data class Success(val result: R2ImageUploadResult) : ImageUploadUiState
    data class Error(val errorMessage: String, val lastUri: Uri? = null) : ImageUploadUiState
}

class ImageUploadPickerState(
    private val context: Context,
    private val scope: CoroutineScope,
    private val uploadManager: R2ImageUploadManager = R2ImageUploadManager(context),
    private val onUploadSuccess: ((R2ImageUploadResult) -> Unit)? = null
) {
    var uiState by mutableStateOf<ImageUploadUiState>(ImageUploadUiState.Idle)
        private set

    var lastSelectedUri by mutableStateOf<Uri?>(null)
        private set

    fun onImageSelected(uri: Uri?) {
        if (uri == null) {
            // User cancelled photo picker
            return
        }
        lastSelectedUri = uri
        performUpload(uri)
    }

    fun retry() {
        val uri = lastSelectedUri ?: return
        performUpload(uri)
    }

    fun reset() {
        uiState = ImageUploadUiState.Idle
        lastSelectedUri = null
    }

    private fun performUpload(uri: Uri) {
        scope.launch {
            uiState = ImageUploadUiState.Processing(uri)
            val result = uploadManager.uploadImageFromUri(uri)
            if (result.isSuccess) {
                val uploadResult = result.getOrNull()!!
                uiState = ImageUploadUiState.Success(uploadResult)
                onUploadSuccess?.invoke(uploadResult)
            } else {
                val error = result.exceptionOrNull()?.localizedMessage
                    ?: "चित्र अपलोड करण्यात अयशस्वी."
                uiState = ImageUploadUiState.Error(error, uri)
            }
        }
    }
}

@Composable
fun rememberImageUploadPickerState(
    uploadManager: R2ImageUploadManager? = null,
    onUploadSuccess: ((R2ImageUploadResult) -> Unit)? = null
): ImageUploadPickerState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = uploadManager ?: remember { R2ImageUploadManager(context) }
    return remember {
        ImageUploadPickerState(
            context = context,
            scope = scope,
            uploadManager = manager,
            onUploadSuccess = onUploadSuccess
        )
    }
}

/**
 * Isolated visual component to test and render image upload state machine.
 */
@Composable
fun ImageUploadStatusCard(
    pickerState: ImageUploadPickerState,
    modifier: Modifier = Modifier
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        pickerState.onImageSelected(uri)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .testTag("image_upload_card"),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (val state = pickerState.uiState) {
                is ImageUploadUiState.Idle -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "चित्र निवडा आणि अपलोड करा",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("select_image_button")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Select Image")
                            Spacer(Modifier.width(8.dp))
                            Text("चित्र निवडा")
                        }
                    }
                }

                is ImageUploadUiState.Processing -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).testTag("processing_indicator"))
                        Spacer(Modifier.width(12.dp))
                        Text(state.message, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                is ImageUploadUiState.Uploading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).testTag("uploading_indicator"))
                        Spacer(Modifier.width(12.dp))
                        Text(state.message, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                is ImageUploadUiState.Success -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("upload_success_icon")
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("चित्र यशस्वीरित्या अपलोड झाले!", style = MaterialTheme.typography.titleSmall)
                            state.result.publicUrl?.let {
                                Text("URL: $it", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                        OutlinedButton(
                            onClick = { pickerState.reset() },
                            modifier = Modifier.testTag("reset_upload_button")
                        ) {
                            Text("पुन्हा")
                        }
                    }
                }

                is ImageUploadUiState.Error -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("upload_error_icon")
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Button(
                            onClick = { pickerState.retry() },
                            modifier = Modifier.testTag("retry_upload_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry")
                            Spacer(Modifier.width(4.dp))
                            Text("पुन्हा प्रयत्न करा")
                        }
                    }
                }
            }
        }
    }
}
