package com.jrla.edgeprint.io

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import kotlin.math.max
import kotlin.math.roundToInt

object ImageIO {
    private val srgb = ColorSpace.get(ColorSpace.Named.SRGB)
    private val displayP3 = ColorSpace.get(ColorSpace.Named.DISPLAY_P3)

    /**
     * Decodes with EXIF rotation applied, with the long side capped at [maxLong].
     * Power-of-two sampling first keeps memory low; an exact resize finishes the job.
     * Output is sRGB, or Display P3 when [wideGamut] is set and the photo uses it.
     */
    fun decode(context: Context, uri: Uri, maxLong: Int, wideGamut: Boolean = false): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bmp = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetColorSpace(if (wideGamut && info.colorSpace == displayP3) displayP3 else srgb)
            val long = max(info.size.width, info.size.height)
            var sample = 1
            while (long / (sample * 2) >= maxLong) sample *= 2
            decoder.setTargetSampleSize(sample)
        }
        val long = max(bmp.width, bmp.height)
        if (long <= maxLong) return bmp
        val s = maxLong.toFloat() / long
        val scaled = Bitmap.createScaledBitmap(
            bmp,
            max(1, (bmp.width * s).roundToInt()),
            max(1, (bmp.height * s).roundToInt()),
            true,
        )
        // The gain map is resolution-independent, so the original one still fits.
        if (Build.VERSION.SDK_INT >= 34 && bmp.hasGainmap() && !scaled.hasGainmap()) scaled.gainmap = bmp.gainmap
        if (scaled !== bmp) bmp.recycle()
        return scaled
    }

    fun displayName(context: Context, uri: Uri): String {
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0)?.let { if (it.isNotBlank()) return it }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "photo"
    }
}
