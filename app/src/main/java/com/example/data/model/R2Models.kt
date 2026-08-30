package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class R2UploadUrlRequest(
    @Json(name = "fileName") val fileName: String,
    @Json(name = "contentType") val contentType: String
)

@JsonClass(generateAdapter = true)
data class R2UploadUrlResponse(
    @Json(name = "uploadUrl") val uploadUrl: String? = null,
    @Json(name = "signedUrl") val signedUrl: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "publicUrl") val publicUrl: String? = null,
    @Json(name = "downloadUrl") val downloadUrl: String? = null,
    @Json(name = "key") val key: String? = null,
    @Json(name = "fileKey") val fileKey: String? = null,
    @Json(name = "fileName") val fileName: String? = null,
    @Json(name = "expiresIn") val expiresIn: Long? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "message") val message: String? = null
) {
    /**
     * Resolves the presigned PUT upload URL from the Edge Function response.
     */
    val effectiveUploadUrl: String?
        get() = uploadUrl ?: signedUrl ?: url

    /**
     * Resolves the public CDN or object key reference from the Edge Function response.
     */
    val effectivePublicUrl: String?
        get() = publicUrl ?: downloadUrl ?: key ?: fileKey
}

data class R2BinaryUploadResult(
    val isSuccess: Boolean,
    val httpCode: Int,
    val errorMessage: String? = null,
    val objectKey: String? = null,
    val publicUrl: String? = null
)

data class R2ImageUploadResult(
    val isSuccess: Boolean,
    val publicUrl: String? = null,
    val objectKey: String? = null,
    val contentType: String? = null,
    val fileSizeBytes: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val errorMessage: String? = null
)
