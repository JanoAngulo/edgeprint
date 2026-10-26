package com.jrla.exifoneline.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Align

// Text drawn on the photo itself; output keeps the photo's size.

abstract class OverlayTemplate(id: String, name: String) : Template(id, name, Family.OVERLAY) {
    override fun padColor(c: Ctx): Int = c.style.background ?: WHITE

    protected fun Ctx.base(): Pair<Bitmap, Canvas> {
        val (bmp, cv) = frame(W, H, BLACK)
        cv.photo(src, 0f, 0f)
        return bmp to cv
    }
}

/** Dark fade along the bottom with logo, divider, credit and settings. */
object OverlayStrip : OverlayTemplate("ov_strip", "Strip") {
    override fun Ctx.draw(): Bitmap {
        val (bmp, cv) = base()
        cv.bottomShade(W, H, 22f * u, 0.62f)
        val pad = 4f * u
        val cy = H - 6f * u
        val lw = drawLogo(cv, pad, cy, 4f * u, Align.LEFT, true)
        var tx = pad
        if (lw > 0f) {
            val dx = pad + lw + 2.2f * u
            cv.vline(dx, cy, 4.4f * u, withAlpha(WHITE, 0.55f), 0.15f * u)
            tx = dx + 2.2f * u
        }
        stack(
            cv, tx, Align.LEFT, cy - 4f * u, cy + 4f * u, 1.1f * u,
            listOf(Txt(by("Photo by "), light(600, 1.8f)), Txt(join("   ", model, params), light(400, 1.4f, 0.85f))),
            maxWidth = W * 0.62f,
        )
        stack(cv, W - pad, Align.RIGHT, cy - 4f * u, cy + 4f * u, 0f, listOf(Txt(date, light(400, 1.4f, 0.75f))))
        return bmp
    }
}

/** Three-column caption: model, credit with settings, date. */
object OverlayCaption : OverlayTemplate("ov_caption", "Caption") {
    override fun Ctx.draw(): Bitmap {
        val (bmp, cv) = base()
        cv.bottomShade(W, H, 17f * u, 0.55f)
        val pad = 3f * u
        val top = H - 10f * u
        val bottom = H - 2.5f * u
        stack(cv, pad, Align.LEFT, top, bottom, 0f, listOf(Txt(model, light(500, 1.3f, 0.8f))), maxWidth = W * 0.26f)
        stack(
            cv, W / 2, Align.CENTER, top, bottom, 1.1f * u,
            listOf(Txt(by("PHOTO BY ").uppercase(), light(700, 1.4f, spacing = 0.08f)), Txt(params, light(400, 1.25f, 0.85f))),
            maxWidth = W * 0.44f,
        )
        stack(cv, W - pad, Align.RIGHT, top, bottom, 0f, listOf(Txt(date, light(400, 1.3f, 0.8f))), maxWidth = W * 0.26f)
        return bmp
    }
}

/** Logo and "Shot on" credit in the middle of the frame. */
object OverlayCenter : OverlayTemplate("ov_center", "Center") {
    override fun Ctx.draw(): Bitmap {
        val (bmp, cv) = base()
        stack(
            cv, W / 2, Align.CENTER, H / 2 - 9f * u, H / 2 + 9f * u, 1.1f * u,
            listOf(
                LogoItem(6f * u, true),
                Gap(0.6f * u),
                Txt(if (model.isBlank()) "" else "Shot on $model", light(500, 1.6f)),
                Txt(author, light(400, 1.25f, 0.75f)),
            ),
            maxWidth = W * 0.8f,
        )
        return bmp
    }
}

/** Logo at the top, copyright line at the bottom. */
object OverlaySign : OverlayTemplate("ov_sign", "Signature") {
    override fun Ctx.draw(): Bitmap {
        val (bmp, cv) = base()
        stack(cv, W / 2, Align.CENTER, 3f * u, 10f * u, 0f, listOf(LogoItem(4f * u, true)))
        stack(
            cv, W / 2, Align.CENTER, H - 10f * u, H - 3f * u, 1.1f * u,
            listOf(Txt(by("© "), light(600, 1.5f)), Txt(join("  ·  ", location, date), light(400, 1.15f, 0.75f))),
            maxWidth = W * 0.9f,
        )
        return bmp
    }
}

/** Settings bottom-left, credit bottom-right. */
object OverlayCorner : OverlayTemplate("ov_corner", "Corners") {
    override fun Ctx.draw(): Bitmap {
        val (bmp, cv) = base()
        val pad = 3.5f * u
        stack(
            cv, pad, Align.LEFT, H - 12f * u, H - 3f * u, 1.1f * u,
            listOf(LogoItem(2.6f * u, true), Gap(0.3f * u), Txt(params, light(600, 1.45f)), Txt(model, light(400, 1.15f, 0.75f))),
            maxWidth = W * 0.46f,
        )
        stack(
            cv, W - pad, Align.RIGHT, H - 9f * u, H - 3f * u, 1.1f * u,
            listOf(Txt(by("Photo by "), light(600, 1.45f)), Txt(location, light(400, 1.15f, 0.75f))),
            maxWidth = W * 0.46f,
        )
        return bmp
    }
}

/** Thin inset rule, logo at the top, italic settings at the bottom. */
object OverlayInset : OverlayTemplate("ov_inset", "Inset") {
    override fun Ctx.draw(): Bitmap {
        val (bmp, cv) = base()
        val inset = 2.6f * u * k
        val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.28f * u
            color = withAlpha(WHITE, 0.9f)
        }
        cv.drawRect(inset, inset, W - inset, H - inset, rule)
        stack(cv, W / 2, Align.CENTER, inset + 2f * u, inset + 8f * u, 0f, listOf(LogoItem(3.4f * u, true)))
        stack(
            cv, W / 2, Align.CENTER, H - inset - 10f * u, H - inset - 2f * u, 1.2f * u,
            listOf(Txt(location, light(400, 1.15f, 0.8f, italic = true)), Txt(params, light(700, 1.75f, italic = true))),
            maxWidth = W - 2 * inset - 4f * u,
        )
        return bmp
    }
}
