package com.jrla.exifoneline.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.RectF
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private const val ACCENT_RED = 0xFFC4161C.toInt()

/** Photo with a side panel listing each setting against its label. */
object EditorialPanel : Template("ed_panel", "Side panel", Family.EDITORIAL) {
    override fun Ctx.draw(): Bitmap {
        val m = 1.8f * u * k
        val panelW = 30f * u
        val outW = m + W + panelW
        val outH = H + 2 * m
        val (bmp, cv) = frame(outW, outH)
        cv.photo(src, m, m)

        val x0 = m + W
        val colX = x0 + panelW * 0.42f
        val label = textPaint(face(400), 1.15f * u, fg2, Align.RIGHT)
        val value = textPaint(face(500), 1.25f * u, fg)
        val rule = Paint().apply { color = hair; strokeWidth = 0.1f * u }
        val groups = listOf(
            listOf("Date" to date, "Location" to location),
            listOf(
                "Focal" to info.focal, "Aperture" to info.aperture,
                "Shutter" to info.shutter, "ISO" to info.iso.removePrefix("ISO"),
            ),
            listOf("Photo by" to author),
            listOf("Shot on" to model),
        ).map { g -> g.map { it.first to it.second.trim() }.filter { it.second.isNotEmpty() } }.filter { it.isNotEmpty() }

        val rowH = 2.5f * u
        val sepGap = 2.6f * u
        val logoZone = 10f * u
        val total = groups.sumOf { it.size } * rowH + (groups.size - 1).coerceAtLeast(0) * sepGap
        var y = max(m + 3f * u, outH - m - logoZone - 4f * u - total)
        groups.forEachIndexed { gi, g ->
            g.forEach { (l, v) ->
                val base = y + rowH * 0.72f
                cv.drawText(l, colX - 0.9f * u, base, label)
                cv.textFit(v, colX + 0.9f * u, base, value, outW - 2.5f * u - (colX + 0.9f * u))
                y += rowH
            }
            if (gi < groups.lastIndex) {
                cv.drawLine(x0 + 3f * u, y + sepGap / 2, outW - 3f * u, y + sepGap / 2, rule)
                y += sepGap
            }
        }
        stack(cv, x0 + panelW / 2, Align.CENTER, outH - m - logoZone, outH - m - 1f * u, 0f, listOf(LogoItem(3.2f * u, dark)))
        return bmp
    }
}

/** Large month (or custom title) with the year in red, settings in a right column. */
object EditorialMagazine : Template("ed_magazine", "Magazine", Family.EDITORIAL) {
    override fun Ctx.draw(): Bitmap {
        val barH = 25f * u
        val (bmp, cv) = frame(W, H + barH)
        cv.photo(src, 0f, 0f)
        val pad = 4.5f * u
        val y0 = H
        val title = texts.title.trim()
        val month = if (style.showDate) info.dateTime?.month?.getDisplayName(TextStyle.FULL, Locale.ENGLISH).orEmpty() else ""
        val headline = title.ifEmpty { month }
        val sub = if (title.isNotEmpty()) date else if (style.showDate) info.dateTime?.year?.toString().orEmpty() else ""

        val right = listOf(
            Txt(params, sans(500, 2.0f)),
            Txt(model, sans(400, 1.5f)),
            Txt(location, sans(400, 1.5f)),
            Txt(by("Photo by "), sans(400, 1.5f)),
        )
        val rightW = stackWidth(right)
        val hp = sans(300, 4.4f)
        val sp = textPaint(face(600), 2.0f * u, ACCENT_RED)
        val leftMax = W - 2 * pad - rightW - 4f * u
        val hy = y0 + 4.8f * u + capH(hp)
        cv.textFit(headline, pad, hy, hp, leftMax)
        cv.textFit(sub, pad, hy + 1.8f * u + capH(sp), sp, leftMax)
        stack(cv, pad, Align.LEFT, y0 + barH - 10f * u, y0 + barH - 3.5f * u, 0f, listOf(LogoItem(4.6f * u, dark)))
        stack(cv, W - pad, Align.RIGHT, y0 + 3.5f * u, y0 + barH - 3.5f * u, 1.25f * u, right, maxWidth = W * 0.55f)
        return bmp
    }
}

/** Grey panel under the photo: logo, model, credit and an optional quote. */
object SpecialDark : Template("sp_dark", "Dark panel", Family.SPECIAL, defaultBg = 0xFF4D4D4F.toInt()) {
    override fun Ctx.draw(): Bitmap {
        val barH = 19f * u
        val (bmp, cv) = frame(W, H + barH)
        cv.photo(src, 0f, 0f)
        stack(
            cv, W / 2, Align.CENTER, H + 2f * u, H + barH - 2f * u, 1.2f * u,
            listOf(
                LogoItem(4f * u, dark),
                Gap(0.5f * u),
                Txt(model, sans(400, 1.35f, fg2)),
                Txt(by("Photo by "), sans(500, 1.6f)),
                Txt(quote, textPaint(face(300, italic = true), 1.2f * u, fg2)),
            ),
            maxWidth = W * 0.86f,
        )
        return bmp
    }
}

/** The photo floating on a blurred, darkened copy of itself. */
object SpecialBlur : Template("sp_blur", "Blur", Family.SPECIAL) {
    override fun padColor(c: Ctx): Int = c.style.background ?: 0xFF111111.toInt()

    override fun Ctx.draw(): Bitmap {
        val m = 5f * u * k
        val bottom = m + 11f * u
        val outW = W + 2 * m
        val outH = H + m + bottom
        val (bmp, cv) = frame(outW, outH, BLACK)
        backdrop(cv, outW, outH)
        cv.photo(src, m, m, radius = 1.4f * u, shadow = 2.2f * u, shadowColor = 0x80000000.toInt())
        stack(
            cv, outW / 2, Align.CENTER, m + H, outH - 1f * u, 1.1f * u,
            listOf(
                LogoItem(3.2f * u, true),
                Gap(0.4f * u),
                Txt(params, light(700, 1.6f)),
                Txt(join("   ", location, date), light(400, 1.15f, 0.75f)),
            ),
            maxWidth = W * 0.9f,
        )
        return bmp
    }

    private fun Ctx.backdrop(cv: Canvas, outW: Float, outH: Float) {
        val tiny = downscale(src, 24)
        val soft = Bitmap.createScaledBitmap(tiny, 96, max(1, (96f * tiny.height / tiny.width).roundToInt()), true)
        tiny.recycle()
        val scale = max(outW / soft.width, outH / soft.height)
        val w = soft.width * scale
        val h = soft.height * scale
        val dst = RectF((outW - w) / 2, (outH - h) / 2, (outW + w) / 2, (outH + h) / 2)
        cv.drawBitmap(soft, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
        soft.recycle()
        cv.drawColor(withAlpha(BLACK, 0.35f))
    }
}

/** Details in a header band above the photo. */
object SpecialHeader : Template("sp_header", "Header", Family.SPECIAL) {
    override fun Ctx.draw(): Bitmap {
        val headH = 17f * u
        val m = 2f * u * k
        val outW = W + 2 * m
        val (bmp, cv) = frame(outW, headH + H + m)
        cv.photo(src, m, headH)
        stack(
            cv, outW / 2, Align.CENTER, 2f * u, headH - 1.2f * u, 1.1f * u,
            listOf(
                LogoItem(3.8f * u, dark),
                Gap(0.3f * u),
                Txt(model, sans(400, 1.15f, fg2)),
                Txt(join("  /  ", info.focal, info.aperture, info.shutter, info.iso), sans(500, 1.3f)),
                Txt(by("By "), sans(700, 1.25f)),
            ),
            maxWidth = W * 0.9f,
        )
        return bmp
    }
}
