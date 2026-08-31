package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImagePreparationHelper {

    const val MAX_IMAGE_DIMENSION = 1920
    const val DEFAULT_JPEG_QUALITY = 82
    const val MAX_RAW_FILE_SIZE_BYTES = 25 * 1024 * 1024L // 25 MB raw input threshold

    val SUPPORTED_MIME_TYPES = setOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    )

    data class PreparedImage(
        val file: File,
        val contentType: String,
        val width: Int,
        val height: Int,
        val fileSizeBytes: Long,
        val originalFileName: String
    )

    /**
     * Resolves the actual MIME type of the given Uri.
     */
    fun getMimeType(context: Context, uri: Uri): String? {
        val resolverType = context.contentResolver.getType(uri)?.lowercase()
        if (!resolverType.isNullOrBlank() && resolverType != "application/octet-stream") {
            return if (resolverType == "image/jpg") "image/jpeg" else resolverType
        }

        // Check file extension if path or URI is available
        val path = uri.path ?: uri.toString()
        val ext = path.substringAfterLast('.', "").lowercase()
        val fromExt = when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> null
        }
        if (fromExt != null) {
            return fromExt
        }

        // Fallback: Check header magic bytes
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                detectMimeTypeFromStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Detects image MIME type from magic bytes.
     */
    fun detectMimeTypeFromStream(stream: InputStream): String? {
        val header = ByteArray(12)
        val read = stream.read(header, 0, 12)
        if (read < 4) return null

        // JPEG: FF D8 FF
        if (header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte()) {
            return "image/jpeg"
        }

        // PNG: 89 50 4E 47
        if (header[0] == 0x89.toByte() && header[1] == 0x50.toByte() &&
            header[2] == 0x4E.toByte() && header[3] == 0x47.toByte()
        ) {
            return "image/png"
        }

        // WebP: RIFF .... WEBP
        if (read >= 12 &&
            header[0] == 'R'.code.toByte() && header[1] == 'I'.code.toByte() &&
            header[2] == 'F'.code.toByte() && header[3] == 'F'.code.toByte() &&
            header[8] == 'W'.code.toByte() && header[9] == 'E'.code.toByte() &&
            header[10] == 'B'.code.toByte() && header[11] == 'P'.code.toByte()
        ) {
            return "image/webp"
        }

        return null
    }

    /**
     * Validates if the given Uri points to a supported, readable image.
     */
    fun validateImage(context: Context, uri: Uri): Result<String> {
        val mimeType = getMimeType(context, uri)
            ?: return Result.failure(IllegalArgumentException("Unsupported or unrecognized image format."))

        if (!SUPPORTED_MIME_TYPES.contains(mimeType)) {
            return Result.failure(IllegalArgumentException("Unsupported format '$mimeType'. Please select JPEG, PNG, or WebP."))
        }

        // Verify readable
        try {
            val length = context.contentResolver.openInputStream(uri)?.use { stream ->
                var total = 0L
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    total += bytesRead
                    if (total > MAX_RAW_FILE_SIZE_BYTES) {
                        return Result.failure(IllegalArgumentException("Image exceeds maximum permitted size of 25MB."))
                    }
                }
                total
            } ?: 0L

            if (length <= 0) {
                return Result.failure(IllegalArgumentException("Image file is empty or unreadable."))
            }
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("Unable to read selected image: ${e.localizedMessage}"))
        }

        return Result.success(mimeType)
    }

    /**
     * Prepares and compresses the image for upload:
     * 1. Extracts EXIF orientation and corrects rotation/flip.
     * 2. Downsamples large images to fit MAX_IMAGE_DIMENSION.
     * 3. Compresses to optimized JPEG/PNG/WebP temporary file in cacheDir.
     */
    suspend fun prepareImage(
        context: Context,
        uri: Uri,
        maxDimension: Int = MAX_IMAGE_DIMENSION,
        quality: Int = DEFAULT_JPEG_QUALITY
    ): Result<PreparedImage> = withContext(Dispatchers.IO) {
        val validation = validateImage(context, uri)
        if (validation.isFailure) {
            return@withContext Result.failure(validation.exceptionOrNull()!!)
        }

        val detectedMime = validation.getOrNull() ?: "image/jpeg"

        try {
            // Read image dimensions without allocating pixel buffer
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val rawWidth = boundsOptions.outWidth
            val rawHeight = boundsOptions.outHeight
            if (rawWidth <= 0 || rawHeight <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Failed to decode image dimensions."))
            }

            // Read EXIF orientation
            val orientation = getExifOrientation(context, uri)

            // Compute inSampleSize to prevent OutOfMemoryError
            val sampleSize = calculateInSampleSize(rawWidth, rawHeight, maxDimension, maxDimension)

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext Result.failure(IllegalStateException("Failed to decode image bitmap."))

            // Apply EXIF transformation and final proportional scaling if needed
            val processedBitmap = transformAndScaleBitmap(decodedBitmap, orientation, maxDimension)

            // Create temporary file
            val uploadDir = File(context.cacheDir, "r2_uploads").apply { if (!exists()) mkdirs() }
            val fileExtension = when (detectedMime) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            val tempFile = File(uploadDir, "img_${UUID.randomUUID()}.$fileExtension")

            // Compress to temp file
            val compressFormat = when (detectedMime) {
                "image/png" -> Bitmap.CompressFormat.PNG
                "image/webp" -> Bitmap.CompressFormat.WEBP
                else -> Bitmap.CompressFormat.JPEG
            }

            FileOutputStream(tempFile).use { outStream ->
                processedBitmap.compress(compressFormat, quality, outStream)
                outStream.flush()
            }

            if (processedBitmap != decodedBitmap) {
                processedBitmap.recycle()
            }
            decodedBitmap.recycle()

            val finalContentType = when (detectedMime) {
                "image/png" -> "image/png"
                "image/webp" -> "image/webp"
                else -> "image/jpeg"
            }

            Result.success(
                PreparedImage(
                    file = tempFile,
                    contentType = finalContentType,
                    width = processedBitmap.width,
                    height = processedBitmap.height,
                    fileSizeBytes = tempFile.length(),
                    originalFileName = tempFile.name
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getExifOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    fun calculateInSampleSize(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight || (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun transformAndScaleBitmap(
        source: Bitmap,
        orientation: Int,
        maxDimension: Int
    ): Bitmap {
        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
        }

        val rotatedWidth = if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            source.height
        } else {
            source.width
        }

        val rotatedHeight = if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            source.width
        } else {
            source.height
        }

        if (rotatedWidth > maxDimension || rotatedHeight > maxDimension) {
            val scale = (maxDimension.toFloat() / maxOf(rotatedWidth, rotatedHeight).toFloat())
            matrix.postScale(scale, scale)
        }

        return if (matrix.isIdentity) {
            source
        } else {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }
    }
}
