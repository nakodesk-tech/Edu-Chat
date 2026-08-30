package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.model.R2ImageUploadResult
import com.example.util.ImagePreparationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class R2ImageUploadManager(
    private val context: Context,
    private val r2StorageRepository: R2StorageRepository = R2StorageRepository(context)
) {
    /**
     * Complete image selection to R2 upload pipeline:
     * 1. Validates selected image URI.
     * 2. Corrects EXIF rotation and compresses/downsamples image to a temporary file.
     * 3. Requests a fresh presigned upload URL from the Edge Function.
     * 4. Uploads binary data to R2 via HTTP PUT stream.
     * 5. Cleans up temporary files.
     * 6. Returns structured R2ImageUploadResult.
     */
    suspend fun uploadImageFromUri(
        uri: Uri,
        customFileName: String? = null
    ): Result<R2ImageUploadResult> = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            // 1. Prepare & compress image
            val prepResult = ImagePreparationHelper.prepareImage(context, uri)
            if (prepResult.isFailure) {
                return@withContext Result.failure(prepResult.exceptionOrNull()!!)
            }

            val preparedImage = prepResult.getOrNull()!!
            tempFile = preparedImage.file

            val fileExt = when (preparedImage.contentType) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }

            val uploadFileName = if (!customFileName.isNullOrBlank()) {
                val clean = customFileName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
                if (clean.contains(".")) clean else "$clean.$fileExt"
            } else {
                "img_${UUID.randomUUID()}.$fileExt"
            }

            // 2. Request presigned upload URL from Supabase Edge Function
            val presignedResult = r2StorageRepository.createUploadUrl(
                fileName = uploadFileName,
                contentType = preparedImage.contentType
            )

            if (presignedResult.isFailure) {
                return@withContext Result.failure(presignedResult.exceptionOrNull()!!)
            }

            val presignedResponse = presignedResult.getOrNull()!!
            val uploadUrl = presignedResponse.effectiveUploadUrl
                ?: return@withContext Result.failure(IllegalStateException("Edge function returned an empty upload URL."))

            // 3. Upload binary file via HTTP PUT to presigned URL
            val binaryResult = r2StorageRepository.uploadBinaryFile(
                uploadUrl = uploadUrl,
                contentType = preparedImage.contentType,
                file = preparedImage.file,
                objectKey = presignedResponse.key ?: presignedResponse.fileKey ?: uploadFileName,
                publicUrl = presignedResponse.effectivePublicUrl
            )

            if (binaryResult.isFailure) {
                return@withContext Result.failure(binaryResult.exceptionOrNull()!!)
            }

            val uploadResult = binaryResult.getOrNull()!!
            Result.success(
                R2ImageUploadResult(
                    isSuccess = true,
                    publicUrl = uploadResult.publicUrl ?: presignedResponse.effectivePublicUrl,
                    objectKey = uploadResult.objectKey ?: presignedResponse.key ?: uploadFileName,
                    contentType = preparedImage.contentType,
                    fileSizeBytes = preparedImage.fileSizeBytes,
                    width = preparedImage.width,
                    height = preparedImage.height
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            // Cleanup temp file safely
            try {
                tempFile?.let {
                    if (it.exists()) it.delete()
                }
            } catch (_: Exception) {}
        }
    }
}
