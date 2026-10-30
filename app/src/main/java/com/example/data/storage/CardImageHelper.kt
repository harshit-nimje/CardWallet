package com.example.data.storage

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import java.io.File

object CardImageHelper {

    /**
     * Checks if an image file or content Uri has a portrait / vertical orientation
     * (height > width), taking into account EXIF rotation metadata.
     * Uses inJustDecodeBounds so no bitmap memory is allocated.
     */
    fun isImageVertical(context: Context, uriString: String?): Boolean? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriString)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            var exifOrientation = ExifInterface.ORIENTATION_NORMAL

            if (uri.scheme == "file" || uri.path?.startsWith("/") == true) {
                val file = if (uri.path != null) File(uri.path!!) else File(uriString)
                if (!file.exists()) return null
                BitmapFactory.decodeFile(file.absolutePath, options)
                try {
                    val exif = ExifInterface(file.absolutePath)
                    exifOrientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } catch (_: Exception) {}
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val exif = ExifInterface(stream)
                        exifOrientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                    }
                } catch (_: Exception) {}
            }

            val rawWidth = options.outWidth
            val rawHeight = options.outHeight

            if (rawWidth <= 0 || rawHeight <= 0) return null

            // Account for 90 or 270 degree rotation in EXIF
            val isRotated90or270 = exifOrientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                    exifOrientation == ExifInterface.ORIENTATION_ROTATE_270

            val effectiveWidth = if (isRotated90or270) rawHeight else rawWidth
            val effectiveHeight = if (isRotated90or270) rawWidth else rawHeight

            effectiveHeight > effectiveWidth
        } catch (_: Exception) {
            null
        }
    }
}
