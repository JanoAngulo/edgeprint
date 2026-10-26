package com.jrla.exifoneline.io

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface

object Exporter {
    const val FOLDER = "Edgeprint"

    /** Where exports went before the rename; still listed on the home screen. */
    const val LEGACY_FOLDER = "EXIF Oneline"

    private val tags = arrayOf(
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_OFFSET_TIME,
        ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
        ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
        ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_APERTURE_VALUE,
        ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
        ExifInterface.TAG_EXPOSURE_PROGRAM,
        ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
        ExifInterface.TAG_METERING_MODE,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_WHITE_BALANCE,
        ExifInterface.TAG_LENS_MAKE,
        ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT,
    )

    private val gpsTags = arrayOf(
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
    )

    /** Writes a JPEG to Pictures/Edgeprint and copies the source photo's metadata onto it. */
    fun save(
        context: Context,
        bitmap: Bitmap,
        sourceName: String,
        exifSource: Uri,
        quality: Int,
        keepGps: Boolean,
    ): Uri {
        val cr = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, sourceName.substringBeforeLast('.') + "_edgeprint.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$FOLDER")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val target = cr.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: error("Couldn't create the output file")
        try {
            cr.openOutputStream(target)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
                ?: error("Couldn't write the output file")
            // Metadata is best-effort: a failure here still leaves a good image.
            runCatching { copyMetadata(context, exifSource, target, bitmap, keepGps) }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            cr.update(target, values, null, null)
        } catch (e: Exception) {
            cr.delete(target, null, null)
            throw e
        }
        return target
    }

    private fun copyMetadata(context: Context, from: Uri, to: Uri, bitmap: Bitmap, keepGps: Boolean) {
        val cr = context.contentResolver
        val src = ExifReader.open(cr, from) ?: return
        cr.openFileDescriptor(to, "rw")?.use { pfd ->
            val dst = ExifInterface(pfd.fileDescriptor)
            val all = if (keepGps) tags + gpsTags else tags
            for (tag in all) src.getAttribute(tag)?.let { dst.setAttribute(tag, it) }
            dst.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            dst.setAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION, bitmap.width.toString())
            dst.setAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION, bitmap.height.toString())
            dst.setAttribute(ExifInterface.TAG_SOFTWARE, "Edgeprint")
            dst.saveAttributes()
        }
    }
}
