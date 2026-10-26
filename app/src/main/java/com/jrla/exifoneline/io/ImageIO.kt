package com.jrla.exifoneline.io

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.OpenableColumns
import kotlin.math.max
import kotlin.math.roundToInt

object ImageIO {
    /**
     * Decodes with EXIF rotation applied, in sRGB, with the long side capped at [maxLong].
     * Power-of-two sampling first keeps memory low; an exact resize finishes the job.
     */
    fun decode(context: Context, uri: Uri, maxLong: Int): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bmp = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
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
