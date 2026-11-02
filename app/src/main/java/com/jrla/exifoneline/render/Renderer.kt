package com.jrla.exifoneline.render

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Gainmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import com.jrla.exifoneline.model.PhotoInfo
import com.jrla.exifoneline.model.Ratio
import com.jrla.exifoneline.model.Style
import com.jrla.exifoneline.model.Texts
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
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
    /** With [hdr], an Ultra HDR photo's gain map is carried onto the result (Android 14+). */
    fun render(src: Bitmap, info: PhotoInfo, texts: Texts, style: Style, logo: LogoSet?, hdr: Boolean = false): Bitmap {
        val template = Templates.byId(style.templateId)
        val ctx = Ctx(src, info, texts, style, logo, template.defaultBg)
        val drawn = with(template) { ctx.draw() }
        val (out, dx, dy) = pad(drawn, style.ratio, template.padColor(ctx))
        val photo = ctx.photoRect
        if (hdr && photo != null && Build.VERSION.SDK_INT >= 34 && src.hasGainmap()) {
            photo.offset(dx.toFloat(), dy.toFloat())
            out.gainmap = HdrGainmap.place(src, out, photo, ctx.photoRadius)
        }
        return out
    }

    /** Returns the padded bitmap and how far the original moved. */
    private fun pad(b: Bitmap, ratio: Ratio, color: Int): Triple<Bitmap, Int, Int> {
        if (ratio == Ratio.ORIGINAL) return Triple(b, 0, 0)
        val target = ratio.w.toFloat() / ratio.h
        val current = b.width.toFloat() / b.height
        val (w, h) = if (current > target) {
            b.width to (b.width / target).roundToInt()
        } else {
            (b.height * target).roundToInt() to b.height
        }
        if (w == b.width && h == b.height) return Triple(b, 0, 0)
        // Whole-pixel offsets, so the photo isn't resampled.
        val dx = (w - b.width) / 2
        val dy = (h - b.height) / 2
        val out = blankLike(b, w, h)
        Canvas(out).apply {
            drawColor(color)
            drawBitmap(b, dx.toFloat(), dy.toFloat(), null)
        }
        b.recycle()
        return Triple(out, dx, dy)
    }
}

/**
 * Ultra HDR photos carry a gain map that brightens highlights on HDR screens. A frame
 * changes the canvas, so the map is rebuilt to match: the photo's map where the photo
 * sits, and "no boost" everywhere else so borders stay paper-white.
 */
@RequiresApi(34)
private object HdrGainmap {
    fun place(src: Bitmap, out: Bitmap, photo: RectF, radius: Float): Gainmap {
        val from = src.gainmap!!
        val contents = from.gainmapContents
        val sx = contents.width.toFloat() / src.width
        val sy = contents.height.toFloat() / src.height
        val w = max(1, (out.width * sx).roundToInt())
        val h = max(1, (out.height * sy).roundToInt())
        val config = contents.config ?: Bitmap.Config.ARGB_8888
        // Gain values are data, not colours: keep the source's colour space so nothing converts them.
        val map = contents.colorSpace?.let { Bitmap.createBitmap(w, h, config, true, it) } ?: Bitmap.createBitmap(w, h, config)
        val cv = Canvas(map)
        cv.drawColor(noBoost(from, config == Bitmap.Config.ALPHA_8))
        val dst = RectF(photo.left * sx, photo.top * sy, photo.right * sx, photo.bottom * sy)
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        if (radius <= 0f) {
            cv.drawBitmap(contents, null, dst, p)
        } else {
            p.shader = BitmapShader(contents, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(Matrix().apply {
                    setRectToRect(RectF(0f, 0f, contents.width.toFloat(), contents.height.toFloat()), dst, Matrix.ScaleToFit.FILL)
                })
            }
            cv.drawRoundRect(dst, radius * sx, radius * sy, p)
        }
        if (Build.VERSION.SDK_INT >= 35) return Gainmap(from, map)
        return Gainmap(map).apply {
            from.ratioMin.let { setRatioMin(it[0], it[1], it[2]) }
            from.ratioMax.let { setRatioMax(it[0], it[1], it[2]) }
            from.gamma.let { setGamma(it[0], it[1], it[2]) }
            from.epsilonSdr.let { setEpsilonSdr(it[0], it[1], it[2]) }
            from.epsilonHdr.let { setEpsilonHdr(it[0], it[1], it[2]) }
            displayRatioForFullHdr = from.displayRatioForFullHdr
            minDisplayRatioForHdrTransition = from.minDisplayRatioForHdrTransition
        }
    }

    /** The stored value that decodes to a gain of exactly 1 in each channel. */
    private fun noBoost(g: Gainmap, alphaOnly: Boolean): Int {
        val v = IntArray(3) { c ->
            val lo = ln(g.ratioMin[c])
            val hi = ln(g.ratioMax[c])
            val t = if (hi > lo) (-lo / (hi - lo)).coerceIn(0f, 1f) else 0f
            (t.pow(g.gamma[c]) * 255).roundToInt()
        }
        return Color.argb(if (alphaOnly) v[0] else 255, v[0], v[1], v[2])
    }
}
