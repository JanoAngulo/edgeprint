package com.jrla.exifoneline.render

import android.graphics.Bitmap
import android.graphics.Canvas
import com.jrla.exifoneline.model.PhotoInfo
import com.jrla.exifoneline.model.Ratio
import com.jrla.exifoneline.model.Style
import com.jrla.exifoneline.model.Texts
import kotlin.math.roundToInt

enum class Family(val label: String) {
    BAR("Bar"),
    POSTER("Poster"),
    OVERLAY("Overlay"),
    EDITORIAL("Editorial"),
    SPECIAL("Special"),
    FRAME("Frame"),
}

abstract class Template(
    val id: String,
    val name: String,
    val family: Family,
    val defaultBg: Int = WHITE,
) {
    /** Returns a new bitmap; never returns or recycles [Ctx.src]. */
    abstract fun Ctx.draw(): Bitmap

    /** Fill used when the result is padded out to a fixed aspect ratio. */
    open fun padColor(c: Ctx): Int = c.bg
}

object Templates {
    val all: List<Template> = listOf(
        BarClassic, BarSplit, BarFocal, BarLogo,
        PosterGallery, PosterLine, PosterShadow, PosterCard, PosterChips,
        OverlayStrip, OverlayCaption, OverlayCenter, OverlaySign, OverlayCorner, OverlayInset,
        EditorialPanel, EditorialMagazine,
        SpecialDark, SpecialBlur, SpecialHeader,
        FrameBorder, FrameInstant, FrameFilm35, FrameFilm120, FrameMat,
    )

    fun byId(id: String): Template = all.firstOrNull { it.id == id } ?: BarClassic
}

object Renderer {
    fun render(src: Bitmap, info: PhotoInfo, texts: Texts, style: Style, logo: LogoSet?): Bitmap {
        val template = Templates.byId(style.templateId)
        val ctx = Ctx(src, info, texts, style, logo, template.defaultBg)
        val out = with(template) { ctx.draw() }
        return pad(out, style.ratio, template.padColor(ctx))
    }

    private fun pad(b: Bitmap, ratio: Ratio, color: Int): Bitmap {
        if (ratio == Ratio.ORIGINAL) return b
        val target = ratio.w.toFloat() / ratio.h
        val current = b.width.toFloat() / b.height
        val (w, h) = if (current > target) {
            b.width to (b.width / target).roundToInt()
        } else {
            (b.height * target).roundToInt() to b.height
        }
        if (w == b.width && h == b.height) return b
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(out).apply {
            drawColor(color)
            drawBitmap(b, (w - b.width) / 2f, (h - b.height) / 2f, null)
        }
        b.recycle()
        return out
    }
}
