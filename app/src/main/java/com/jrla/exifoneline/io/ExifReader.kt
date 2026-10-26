package com.jrla.exifoneline.io

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.jrla.exifoneline.model.PhotoInfo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

class ExifResult(
    val info: PhotoInfo,
    val rawModel: String,
    val lat: Double?,
    val lon: Double?,
    /** Where full metadata (including GPS when allowed) can be re-read for export. */
    val exifUri: Uri,
)

object ExifReader {
    private val exifDate = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
    val displayDate: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

    fun read(context: Context, uri: Uri, modelOverride: (String) -> String?): ExifResult {
        val cr = context.contentResolver
        val basic = open(cr, uri)
        var exif = basic
        var exifUri = uri
        // Picker and shared URIs have GPS redacted; the original file keeps it when we hold
        // media + ACCESS_MEDIA_LOCATION permission. Only trust it if it's the same shot.
        originalUri(uri)?.let { orig ->
            val full = open(cr, orig)
            if (full != null && (basic == null || sameShot(basic, full))) {
                exif = full
                exifUri = orig
            }
        }
        val e = exif ?: return ExifResult(PhotoInfo(), "", null, null, uri)

        val make = e.getAttribute(ExifInterface.TAG_MAKE)?.trim().orEmpty()
        val rawModel = e.getAttribute(ExifInterface.TAG_MODEL)?.trim().orEmpty()
        val dateTime = (e.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: e.getAttribute(ExifInterface.TAG_DATETIME))
            ?.let { runCatching { LocalDateTime.parse(it.trim(), exifDate) }.getOrNull() }
        val latLong = e.latLong

        val info = PhotoInfo(
            brand = Models.brand(make),
            model = modelOverride(rawModel) ?: Models.friendly(rawModel),
            focal = focal(e),
            aperture = aperture(e.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)),
            shutter = shutter(e.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)),
            iso = e.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0).let { if (it > 0) "ISO$it" else "" },
            date = dateTime?.format(displayDate).orEmpty(),
            dateTime = dateTime,
        )
        return ExifResult(info, rawModel, latLong?.get(0), latLong?.get(1), exifUri)
    }

    fun open(cr: ContentResolver, uri: Uri): ExifInterface? =
        try {
            cr.openInputStream(uri)?.use { ExifInterface(it) }
        } catch (e: Exception) {
            null
        }

    private fun sameShot(a: ExifInterface, b: ExifInterface) =
        a.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) == b.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) &&
            a.getAttribute(ExifInterface.TAG_MODEL) == b.getAttribute(ExifInterface.TAG_MODEL)

    private fun originalUri(uri: Uri): Uri? {
        val id = when (uri.authority) {
            MediaStore.AUTHORITY -> uri.lastPathSegment?.toLongOrNull()
            "com.android.providers.media.documents" ->
                runCatching { DocumentsContract.getDocumentId(uri).substringAfter(':').toLong() }.getOrNull()
            else -> null
        } ?: return null
        val isMediaUri = uri.authority == MediaStore.AUTHORITY && uri.pathSegments.firstOrNull()?.startsWith("picker") != true
        val media = if (isMediaUri) uri else ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        return runCatching { MediaStore.setRequireOriginal(media) }.getOrNull()
    }

    private fun focal(e: ExifInterface): String {
        val eq = e.getAttributeInt(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, 0)
        if (eq > 0) return "${eq}mm"
        val f = e.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
        return if (f > 0) "${trim(f)}mm" else ""
    }

    private fun aperture(f: Double): String = when {
        f <= 0 -> ""
        f >= 10 -> "f/${f.roundToInt()}"
        else -> "f/" + String.format(Locale.US, "%.1f", f)
    }

    private fun shutter(t: Double): String {
        if (t <= 0) return ""
        if (t < 1) {
            val inv = 1 / t
            if (inv >= 4 || abs(inv - inv.roundToInt()) < 0.05) return "1/${inv.roundToInt()}s"
        }
        return "${trim(t)}s"
    }

    private fun trim(d: Double) = if (d == floor(d)) d.toInt().toString() else String.format(Locale.US, "%.1f", d)
}
