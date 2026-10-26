package com.jrla.exifoneline.render

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.RectF
import kotlin.math.max

// Even border around the photo with the details centred underneath.

private class Poster(val bmp: Bitmap, val cv: android.graphics.Canvas, val outW: Float, val textTop: Float, val textBottom: Float)

private fun Ctx.poster(side: Float, bottom: Float, radius: Float = 0f, shadow: Float = 0f): Poster {
    val outW = W + 2 * side
    val outH = H + side + bottom
    val (bmp, cv) = frame(outW, outH)
    cv.photo(src, side, side, radius = radius, shadow = shadow, shadowColor = 0x40000000)
    return Poster(bmp, cv, outW, side + H, outH)
}

object PosterGallery : Template("poster_gallery", "Gallery", Family.POSTER) {
    override fun Ctx.draw(): Bitmap {
        val side = 2.6f * u * k
        val p = poster(side, side + 8.5f * u)
        stack(
            p.cv, p.outW / 2, Align.CENTER, p.textTop, p.textBottom, 1.2f * u,
            listOf(Txt(by("Photo by "), sans(600, 1.7f)), Txt(join("   ", params, location, date), sans(400, 1.3f, fg2))),
            maxWidth = W * 0.92f,
        )
        return p.bmp
    }
}

object PosterLine : Template("poster_line", "Line", Family.POSTER) {
    override fun Ctx.draw(): Bitmap {
        val side = 3f * u * k
        val p = poster(side, side + 12.5f * u)
        stack(
            p.cv, p.outW / 2, Align.CENTER, p.textTop, p.textBottom, 1.3f * u,
            listOf(
                LogoItem(3.4f * u, dark),
                Gap(0.6f * u),
                Txt(join("   |   ", model, params, location), sans(500, 1.4f)),
                Txt(author, sans(400, 1.25f, fg2)),
            ),
            maxWidth = W * 0.92f,
        )
        return p.bmp
    }
}

object PosterShadow : Template("poster_shadow", "Shadow", Family.POSTER, defaultBg = 0xFFFBFAF7.toInt()) {
    override fun Ctx.draw(): Bitmap {
        val side = 6f * u * k
        val p = poster(side, side + 13f * u, shadow = 1.6f * u)
        stack(
            p.cv, p.outW / 2, Align.CENTER, p.textTop, p.textBottom, 1.3f * u,
            listOf(
                LogoItem(3.6f * u, dark),
                Gap(0.4f * u),
                Txt(model, sans(400, 1.5f)),
                Txt(params, sans(700, 1.7f)),
            ),
            maxWidth = W * 0.92f,
        )
        return p.bmp
    }
}

object PosterCard : Template("poster_card", "Card", Family.POSTER, defaultBg = 0xFFFBFAF7.toInt()) {
    override fun Ctx.draw(): Bitmap {
        val side = 5f * u * k
        val p = poster(side, side + 12f * u, radius = 1.1f * u, shadow = 1.8f * u)
        stack(
            p.cv, p.outW / 2, Align.CENTER, p.textTop, p.textBottom, 1.3f * u,
            listOf(
                LogoItem(3f * u, dark),
                Gap(0.6f * u),
                Txt(join("   |   ", date, params, location), sans(400, 1.35f)),
                Txt(by("© "), sans(400, 1.2f, fg2)),
            ),
            maxWidth = W * 0.92f,
        )
        return p.bmp
    }
}

/** Model beside the logo, then each setting in its own outlined chip. */
object PosterChips : Template("poster_chips", "Chips", Family.POSTER) {
    override fun Ctx.draw(): Bitmap {
        val side = 4f * u * k
        val p = poster(side, side + 14f * u)

        val mp = sans(700, 1.7f)
        val lh = 2.6f * u
        val lw = logoWidth(lh)
        val mw = mp.width(model)
        val gapX = if (lw > 0f && mw > 0f) 1.4f * u else 0f
        val head = Block(mw + gapX + lw, max(lh, capH(mp))) { cv, left, top ->
            val cy = top + max(lh, capH(mp)) / 2
            mp.textAlign = Align.LEFT
            if (mw > 0f) cv.drawText(model, left, cy + capH(mp) / 2, mp)
            drawLogo(cv, left + mw + gapX, cy, lh, Align.LEFT, dark)
        }

        val chips = listOf(
            info.shutter.trim().removeSuffix("s") to "s",
            info.iso.trim().removePrefix("ISO").trim() to "ISO",
            info.focal.trim().removeSuffix("mm") to "mm",
            info.aperture.trim().removePrefix("f/") to "f",
        ).filter { it.first.isNotBlank() }
        val value = sans(500, 1.35f)
        val label = sans(400, 1.05f, fg2)
        val chipH = 3f * u
        val chipGap = 1.4f * u
        val widths = chips.map { max(value.width(it.first) + 2.4f * u, 5.6f * u) }
        val rowW = widths.sum() + chipGap * (chips.size - 1).coerceAtLeast(0)
        val row = Block(rowW, if (chips.isEmpty()) 0f else chipH + 2.6f * u) { cv, left, top ->
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 0.12f * u
                color = withAlpha(fg, 0.35f)
            }
            value.textAlign = Align.CENTER
            label.textAlign = Align.CENTER
            var x = left
            chips.forEachIndexed { i, (v, l) ->
                val w = widths[i]
                cv.drawRoundRect(RectF(x, top, x + w, top + chipH), 0.6f * u, 0.6f * u, stroke)
                cv.drawText(v, x + w / 2, top + chipH / 2 + capH(value) / 2, value)
                cv.drawText(l, x + w / 2, top + chipH + 2.0f * u, label)
                x += w + chipGap
            }
        }
        stack(p.cv, p.outW / 2, Align.CENTER, p.textTop, p.textBottom, 1.6f * u, listOf(head, row))
        return p.bmp
    }
}
