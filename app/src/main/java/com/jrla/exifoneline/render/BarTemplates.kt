package com.jrla.exifoneline.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint.Align

// White strip under the photo: logo, model and settings on one line.

private fun Ctx.barCanvas(barH: Float): Pair<Bitmap, Canvas> {
    val (bmp, cv) = frame(W, H + barH)
    cv.photo(src, 0f, 0f)
    return bmp to cv
}

/** Model and date on the left; logo, divider, settings and place on the right. */
object BarClassic : Template("bar_classic", "Classic", Family.BAR) {
    override fun Ctx.draw(): Bitmap {
        val barH = 11f * u
        val (bmp, cv) = barCanvas(barH)
        val top = H
        val bottom = H + barH
        val pad = 4f * u
        val title = sans(700, 2.2f)
        val sub = sans(400, 1.6f, fg2)
        val strong = sans(600, 2.0f)
        val subR = sans(400, 1.6f, fg2)
        var logoH = 5f * u

        val left = listOf(Txt(model, title), Txt(date, sub))
        val right = listOf(Txt(params, strong), Txt(location, subR))
        val lw = logoWidth(logoH)
        val needed = stackWidth(left) + stackWidth(right) + (if (lw > 0f) lw + 4.4f * u else 0f) + 4f * u
        logoH *= shrinkToFit(W - 2 * pad, needed, title, sub, strong, subR)

        stack(cv, pad, Align.LEFT, top, bottom, 1.1f * u, left)
        val textX = W - pad - stackWidth(right)
        stack(cv, textX, Align.LEFT, top, bottom, 1.1f * u, right)
        if (logoWidth(logoH) > 0f) {
            val divX = textX - 2.2f * u
            cv.vline(divX, top + barH / 2, 4.6f * u, hair, 0.16f * u)
            drawLogo(cv, divX - 2.2f * u, top + barH / 2, logoH, Align.RIGHT, dark)
        }
        return bmp
    }
}

/** Author and place on the left; logo, divider, model and bold settings on the right. */
object BarSplit : Template("bar_split", "Split", Family.BAR) {
    override fun Ctx.draw(): Bitmap {
        val barH = 11f * u
        val (bmp, cv) = barCanvas(barH)
        val top = H
        val bottom = H + barH
        val pad = 4f * u
        val a = sans(600, 2.0f)
        val b = sans(400, 1.5f, fg2)
        val c = sans(500, 1.6f)
        val d = sans(700, 2.0f)
        var logoH = 5.5f * u

        val left = listOf(Txt(by("by "), a), Txt(join("   ", location, date), b))
        val right = listOf(Txt(model, c), Txt(params, d))
        val lw = logoWidth(logoH)
        val needed = stackWidth(left) + stackWidth(right) + (if (lw > 0f) lw + 4.4f * u else 0f) + 4f * u
        logoH *= shrinkToFit(W - 2 * pad, needed, a, b, c, d)

        stack(cv, pad, Align.LEFT, top, bottom, 1.1f * u, left)
        stack(cv, W - pad, Align.RIGHT, top, bottom, 1.1f * u, right)
        if (logoWidth(logoH) > 0f) {
            val divX = W - pad - stackWidth(right) - 2.2f * u
            cv.vline(divX, top + barH / 2, 4.6f * u, hair, 0.16f * u)
            drawLogo(cv, divX - 2.2f * u, top + barH / 2, logoH, Align.RIGHT, dark)
        }
        return bmp
    }
}

/** Logo left; model and exposure, a large focal length, then author and date. */
object BarFocal : Template("bar_focal", "Focal", Family.BAR) {
    override fun Ctx.draw(): Bitmap {
        val barH = 11f * u
        val (bmp, cv) = barCanvas(barH)
        val top = H
        val bottom = H + barH
        val pad = 4f * u
        val gap = 3f * u
        val a = sans(600, 1.9f)
        val b = sans(400, 1.5f, fg2)
        val big = sans(300, 4.4f)
        val m = sans(700, 1.9f)
        val e = sans(400, 1.5f, fg2)
        var logoH = 6.5f * u

        val authorBlock = listOf(Txt(by("by "), a), Txt(date, b))
        val focal = listOf(Txt(info.focal.trim(), big))
        val mp = listOf(Txt(model, m), Txt(exposure, e))
        fun widths() = Triple(stackWidth(authorBlock), stackWidth(focal), stackWidth(mp))
        val (aw0, fw0, mw0) = widths()
        val lw = logoWidth(logoH)
        val needed = lw + 4f * u + mw0 + gap + fw0 + gap + aw0
        logoH *= shrinkToFit(W - 2 * pad, needed, a, b, big, m, e)

        drawLogo(cv, pad, top + barH / 2, logoH, Align.LEFT, dark)
        val (aw, fw, _) = widths()
        var x = W - pad
        stack(cv, x, Align.RIGHT, top, bottom, 1.1f * u, authorBlock)
        if (aw > 0f) x -= aw + gap
        stack(cv, x, Align.RIGHT, top, bottom, 0f, focal)
        if (fw > 0f) x -= fw + gap
        stack(cv, x, Align.RIGHT, top, bottom, 1.1f * u, mp)
        return bmp
    }
}

/** Logo first with model and settings beside it; place and date on the right. */
object BarLogo : Template("bar_logo", "Logo first", Family.BAR) {
    override fun Ctx.draw(): Bitmap {
        val barH = 11f * u
        val (bmp, cv) = barCanvas(barH)
        val top = H
        val bottom = H + barH
        val pad = 4f * u
        val a = sans(700, 2.0f)
        val b = sans(400, 1.6f, fg2)
        val c = sans(700, 1.9f)
        val d = sans(400, 1.4f, fg2)
        var logoH = 5.5f * u

        val mid = listOf(Txt(model, a), Txt(params, b))
        val right = listOf(Txt(location, c), Txt(date, d))
        val lw = logoWidth(logoH)
        val needed = (if (lw > 0f) lw + 2.4f * u else 0f) + stackWidth(mid) + stackWidth(right) + 4f * u
        logoH *= shrinkToFit(W - 2 * pad, needed, a, b, c, d)

        val drawn = drawLogo(cv, pad, top + barH / 2, logoH, Align.LEFT, dark)
        val tx = if (drawn > 0f) pad + drawn + 2.4f * u else pad
        stack(cv, tx, Align.LEFT, top, bottom, 1.1f * u, mid)
        stack(cv, W - pad, Align.RIGHT, top, bottom, 1.1f * u, right)
        return bmp
    }
}
