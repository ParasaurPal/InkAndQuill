package com.parasaurpal.inkandquill.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

data class PendingCameraCapture(
    val mediaId: String,
    val contentUri: Uri,
    val file: File
)

object MediaManager {
    val CHARACTER_KINDS = setOf("Character", "NPC", "Dynasty", "Lineage", "Deity", "Fauna", "Monster", "Species", "Dragon")
    val LOCATION_KINDS = setOf("Location", "Region", "Kingdom", "Settlement", "City", "Dungeon", "Stronghold", "Biome", "Realm", "World")

    fun attachmentsDir(context: Context): File {
        return File(context.filesDir, "attachments").apply {
            if (!exists()) mkdirs()
        }
    }

    fun createCameraDestination(context: Context): PendingCameraCapture {
        val mediaId = UUID.randomUUID().toString()
        val file = File(attachmentsDir(context), "$mediaId.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return PendingCameraCapture(mediaId = mediaId, contentUri = uri, file = file)
    }

    fun finalizeCameraCapture(
        db: InkDatabase,
        pending: PendingCameraCapture,
        recordId: String,
        templateId: String,
        fieldId: String = "",
        subjectKind: String = "",
        caption: String = "",
        displayName: String = "Camera photo"
    ): MediaAsset? {
        if (!pending.file.exists() || pending.file.length() <= 0L) {
            runCatching { pending.file.delete() }
            return null
        }
        return db.addMediaAsset(
            id = pending.mediaId,
            recordId = recordId,
            uri = pending.contentUri.toString(),
            mimeType = "image/jpeg",
            displayName = displayName,
            size = pending.file.length(),
            templateId = templateId,
            fieldId = fieldId,
            subjectKind = subjectKind,
            caption = caption,
            source = "camera"
        )
    }

    fun importPickedPhoto(
        context: Context,
        db: InkDatabase,
        sourceUri: Uri,
        recordId: String,
        templateId: String,
        fieldId: String = "",
        subjectKind: String = "",
        caption: String = ""
    ): MediaAsset? {
        val mime = context.contentResolver.getType(sourceUri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
        val ext = when {
            mime.contains("png") -> ".png"
            mime.contains("webp") -> ".webp"
            else -> ".jpg"
        }
        val mediaId = UUID.randomUUID().toString()
        val destFile = File(attachmentsDir(context), "$mediaId$ext")
        val bytes = runCatching {
            context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
        }.getOrNull() ?: return null
        if (bytes.isEmpty()) return null
        destFile.writeBytes(bytes)
        val localUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            destFile
        )
        val name = sourceUri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null } ?: "Photo$ext"
        return db.addMediaAsset(
            id = mediaId,
            recordId = recordId,
            uri = localUri.toString(),
            mimeType = mime,
            displayName = name,
            size = destFile.length(),
            templateId = templateId,
            fieldId = fieldId,
            subjectKind = subjectKind,
            caption = caption,
            source = "gallery"
        )
    }

    fun decodeImageBitmap(context: Context, uriOrData: String, maxDim: Int = 1024): ImageBitmap? {
        if (uriOrData.isBlank()) return null
        return runCatching {
            if (uriOrData.startsWith("data:")) {
                val raw = uriOrData.substringAfter("base64,", "")
                if (raw.isBlank()) return null
                val bytes = Base64.decode(raw, Base64.DEFAULT)
                decodeSampledByteArray(bytes, maxDim)?.asImageBitmap()
            } else {
                val parsed = Uri.parse(uriOrData)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(parsed)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, bounds)
                }
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDim, maxDim)
                }
                context.contentResolver.openInputStream(parsed)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
                }
            }
        }.getOrNull()
    }

    fun toInlineDataUri(context: Context, uriOrData: String, maxDim: Int = 1200): String? {
        if (uriOrData.isBlank()) return null
        if (uriOrData.startsWith("data:")) return uriOrData
        return runCatching {
            val parsed = Uri.parse(uriOrData)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDim, maxDim)
            }
            val bmp = context.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return@runCatching null
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 86, out)
            "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }.getOrNull()
    }

    fun subjectGroupLabel(kind: String): String = when {
        kind in CHARACTER_KINDS -> "Character"
        kind in LOCATION_KINDS -> "Location"
        kind == "Template" -> "Template"
        kind.isBlank() -> "General"
        else -> kind
    }

    private fun decodeSampledByteArray(bytes: ByteArray, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDim, maxDim)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }
}
