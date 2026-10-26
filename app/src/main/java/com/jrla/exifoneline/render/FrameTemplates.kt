package com.jrla.exifoneline.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Align
import kotlin.math.max

private const val FILM = 0xFF0D0D0D.toInt()
private const val FILM_TEXT = 0xFFF0A040.toInt()

/** Plain even border in the chosen colour. */
object FrameBorder : Template("fr_border", "Border", Family.FRAME) {
    override fun Ctx.draw(): Bitmap {
        val b = 4f * u * k
        val (bmp, cv) = frame(W + 2 * b, H + 2 * b)
        cv.photo(src, b, b)
        return bmp
    }
}

/** Instant-print look: deep bottom margin with a serif signature. */
object FrameInstant : Template("fr_instant", "Instant", Family.FRAME, defaultBg = 0xFFF7F5F0.toInt()) {
    override fun Ctx.draw(): Bitmap {
        val side = 4.5f * u * k
        val outW = W + 2 * side
        val outH = H + 2 * side + 12f * u
        val (bmp, cv) = frame(outW, outH)
        cv.photo(src, side, side)
        stack(
            cv, outW / 2, Align.CENTER, side + H, outH - side * 0.5f, 1.4f * u,
            listOf(
                Txt(author, textPaint(Fonts.serifItalic(400), 2.6f * u, fg)),
                Txt(join("  ·  ", date, location), sans(400, 1.15f, fg2)),
            ),
            maxWidth = W * 0.9f,
        )
        return bmp
    }
}

private fun Ctx.frameNumber() = (info.dateTime?.second ?: 23) % 36 + 1

private fun Ctx.filmPaint(size: Float, align: Align) =
    textPaint(Fonts.mono, size * u, FILM_TEXT, align, spacing = 0.06f)

/** 35mm negative: rounded gate, orange edge markings, author along the side. */
object FrameFilm35 : Template("fr_film35", "Film 35", Family.FRAME, defaultBg = FILM) {
    override fun padColor(c: Ctx): Int = c.style.background ?: FILM

    override fun Ctx.draw(): Bitmap {
        val side = 4.2f * u * k
        val tb = max(5.4f * u * k, 3.6f * u)
        val outW = W + 2 * side
        val outH = H + 2 * tb
        val (bmp, cv) = frame(outW, outH, FILM)
        cv.photo(src, side, tb, radius = 0.5f * u)

        val n = frameNumber()
        val left = filmPaint(1.25f, Align.LEFT)
        val center = filmPaint(1.25f, Align.CENTER)
        val right = filmPaint(1.25f, Align.RIGHT)
        val topY = tb / 2 + capH(left) / 2
        val bottomY = tb + H + tb / 2 + capH(left) / 2
        cv.drawText("EXIF 400", side, topY, left)
        cv.drawText("$n", outW - side, topY, right)
        cv.drawText("▶ $n", side, bottomY, left)
        cv.textFit(join("  ", info.shutter, info.aperture.uppercase(), info.iso), outW / 2, bottomY, center, W * 0.6f)
        cv.drawText("${n}A ▶", outW - side, bottomY, right)

        if (author.isNotEmpty()) {
            val vertical = filmPaint(0.95f, Align.CENTER)
            val x = outW - side / 2
            cv.save()
            cv.rotate(90f, x, outH / 2)
            cv.textFit(author, x, outH / 2 + capH(vertical) / 2, vertical, H * 0.5f)
            cv.restore()
        }
        return bmp
    }
}

/** Medium-format negative: square gate with frame counts top and bottom. */
object FrameFilm120 : Template("fr_film120", "Film 120", Family.FRAME, defaultBg = FILM) {
    override fun padColor(c: Ctx): Int = c.style.background ?: FILM

    override fun Ctx.draw(): Bitmap {
        val side = 3f * u * k
        val tb = max(4.6f * u * k, 3.6f * u)
        val outW = W + 2 * side
        val outH = H + 2 * tb
        val (bmp, cv) = frame(outW, outH, FILM)
        cv.photo(src, side, tb)

        val n = frameNumber()
        val left = filmPaint(1.3f, Align.LEFT)
        val center = filmPaint(1.1f, Align.CENTER)
        val right = filmPaint(1.3f, Align.RIGHT)
        val topY = tb / 2 + capH(left) / 2
        val bottomY = tb + H + tb / 2 + capH(left) / 2
        cv.drawText("400", side + 2f * u, topY, left)
        cv.drawText("${n + 12}", outW - side - 2f * u, topY, right)
        cv.drawText("▶ ${n % 12 + 1}", side + 2f * u, bottomY, left)
        cv.textFit(join("  ", info.focal, info.aperture.uppercase(), info.shutter), outW / 2, bottomY, center, W * 0.5f)
        cv.drawText("▶", outW - side - 2f * u, bottomY, right)
        return bmp
    }
}

/** Gallery mat: wide paper border, bevelled window, caption below. */
object FrameMat : Template("fr_mat", "Mat", Family.FRAME, defaultBg = 0xFFF2F0EB.toInt()) {
    override fun Ctx.draw(): Bitmap {
        val side = 12f * u * k
        val outW = W + 2 * side
        val outH = H + 2 * side + 8f * u
        val (bmp, cv) = frame(outW, outH)
        bevel(cv, side)
        cv.photo(src, side, side)
        stack(
            cv, outW / 2, Align.CENTER, side + H + 2f * u, outH - side * 0.5f, 1.2f * u,
            listOf(Txt(join("    ", location, date), sans(400, 1.3f)), Txt(author, sans(400, 1.2f, fg2))),
            maxWidth = W * 0.9f,
        )
        return bmp
    }

    private fun Ctx.bevel(cv: Canvas, side: Float) {
        val o = 0.7f * u
        val light = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.5f * u
            color = if (dark) withAlpha(WHITE, 0.08f) else 0xFFFDFCFA.toInt()
        }
        cv.drawRect(side - o, side - o, side + W + o, side + H + o, light)
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.12f * u
            color = withAlpha(BLACK, 0.18f)
        }
        cv.drawRect(side, side, side + W, side + H, edge)
    }
}
