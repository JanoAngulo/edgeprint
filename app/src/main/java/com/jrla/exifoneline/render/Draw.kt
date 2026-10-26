package com.jrla.exifoneline.render

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.jrla.exifoneline.model.LogoTint
import com.jrla.exifoneline.model.PhotoInfo
import com.jrla.exifoneline.model.Style
import com.jrla.exifoneline.model.Texts
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

const val WHITE = 0xFFFFFFFF.toInt()
const val BLACK = 0xFF000000.toInt()
private const val INK = 0xFF161616.toInt()
private const val PAPER_TEXT = 0xFFF3F3F1.toInt()

fun withAlpha(color: Int, alpha: Float): Int =
    (color and 0x00FFFFFF) or ((alpha * 255).roundToInt().coerceIn(0, 255) shl 24)

fun isDark(color: Int): Boolean =
    0.2126f * Color.red(color) + 0.7152f * Color.green(color) + 0.0722f * Color.blue(color) < 140f

fun textPaint(
    typeface: Typeface,
    size: Float,
    color: Int,
    align: Align = Align.LEFT,
    spacing: Float = 0f,
) = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
    this.typeface = typeface
    textSize = size
    this.color = color
    textAlign = align
    letterSpacing = spacing
}

fun capH(p: Paint): Float {
    val r = Rect()
    p.getTextBounds("H", 0, 1, r)
    return r.height().toFloat()
}

fun Paint.width(s: String) = if (s.isBlank()) 0f else measureText(s)

/** Shrinks every paint by the same factor when [needed] exceeds [available]; returns the factor. */
fun shrinkToFit(available: Float, needed: Float, vararg paints: Paint): Float {
    if (needed <= available || needed <= 0f) return 1f
    val f = available / needed
    paints.forEach { it.textSize *= f }
    return f
}

/** Draws [s], temporarily shrinking the paint so the text never runs past [maxW]. */
fun Canvas.textFit(s: String, x: Float, y: Float, p: Paint, maxW: Float) {
    if (s.isBlank()) return
    val size = p.textSize
    val w = p.measureText(s)
    if (w > maxW && maxW > 0f) p.textSize = size * maxW / w
    drawText(s, x, y, p)
    p.textSize = size
}

fun Canvas.vline(x: Float, cy: Float, h: Float, color: Int, width: Float) {
    drawRect(x - width / 2, cy - h / 2, x + width / 2, cy + h / 2, Paint().apply { this.color = color })
}

fun Canvas.bottomShade(w: Float, h: Float, height: Float, alpha: Float) {
    val p = Paint().apply {
        shader = LinearGradient(
            0f, h - height, 0f, h,
            intArrayOf(0, withAlpha(BLACK, alpha * 0.45f), withAlpha(BLACK, alpha)),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    drawRect(0f, h - height, w, h, p)
}

/** Draws the photo into a rect, optionally with rounded corners and a soft drop shadow. */
fun Canvas.photo(
    src: Bitmap,
    left: Float,
    top: Float,
    width: Float = src.width.toFloat(),
    height: Float = src.height.toFloat(),
    radius: Float = 0f,
    shadow: Float = 0f,
    shadowColor: Int = 0x59000000,
) {
    val dst = RectF(left, top, left + width, top + height)
    if (shadow > 0f) {
        val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BLACK
            setShadowLayer(shadow, 0f, shadow * 0.4f, shadowColor)
        }
        drawRoundRect(dst, radius, radius, sp)
    }
    val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    if (radius <= 0f) {
        drawBitmap(src, null, dst, p)
        return
    }
    p.shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
        setLocalMatrix(Matrix().apply {
            setRectToRect(RectF(0f, 0f, src.width.toFloat(), src.height.toFloat()), dst, Matrix.ScaleToFit.FILL)
        })
    }
    drawRoundRect(dst, radius, radius, p)
}

/** Halves repeatedly before the final resize so big downscales stay smooth. */
fun downscale(src: Bitmap, targetW: Int): Bitmap {
    var cur = src
    while (cur.width / 2 >= targetW) {
        val next = Bitmap.createScaledBitmap(cur, cur.width / 2, max(1, cur.height / 2), true)
        if (cur !== src) cur.recycle()
        cur = next
    }
    if (cur.width == targetW) return if (cur === src) cur.copy(Bitmap.Config.ARGB_8888, false) else cur
    val h = max(1, (targetW.toFloat() * cur.height / cur.width).roundToInt())
    val out = Bitmap.createScaledBitmap(cur, targetW, h, true)
    if (cur !== src && cur !== out) cur.recycle()
    return out
}

/** Items laid out top-to-bottom by [Ctx.stack]. */
sealed interface Item
class Txt(val s: String, val p: Paint) : Item
class LogoItem(val h: Float, val onDark: Boolean) : Item
class Gap(val h: Float) : Item
class Block(val w: Float, val h: Float, val draw: (Canvas, Float, Float) -> Unit) : Item

/**
 * Everything a template needs: the photo, its info and the style, with sizes in [u] —
 * 1% of the photo's geometric-mean side — so layouts scale identically from preview to export.
 */
class Ctx(
    val src: Bitmap,
    val info: PhotoInfo,
    val texts: Texts,
    val style: Style,
    private val logo: LogoSet?,
    defaultBg: Int,
) {
    val W = src.width.toFloat()
    val H = src.height.toFloat()
    val u = sqrt(W * H) / 100f
    val k = style.borderScale
    val bg = style.background ?: defaultBg
    val dark = isDark(bg)
    val fg = if (dark) PAPER_TEXT else INK
    val fg2 = withAlpha(fg, 0.56f)
    val hair = withAlpha(fg, 0.22f)

    val model get() = info.model.trim()
    val params get() = join("  ", info.focal, info.aperture, info.shutter, info.iso)
    val exposure get() = join("  ", info.aperture, info.shutter, info.iso)
    val date get() = if (style.showDate) info.date.trim() else ""
    val location get() = if (style.showLocation) info.location.trim() else ""
    val author get() = if (style.showAuthor) texts.author.trim() else ""
    val quote get() = texts.quote.trim().let { if (it.isBlank()) "" else "“$it”" }

    fun by(prefix: String) = if (author.isBlank()) "" else prefix + author
    fun join(sep: String, vararg parts: String) = parts.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(sep)

    /** The user's chosen watermark font. */
    fun face(weight: Int, italic: Boolean = false) = Fonts.face(style.font, weight, italic)

    /** Text paint on the frame background; [size] is in u. */
    fun sans(weight: Int, size: Float, color: Int = fg, spacing: Float = 0f) =
        textPaint(face(weight), size * u, color, spacing = spacing)

    /** White text with a soft shadow, for drawing over the photo. */
    fun light(weight: Int, size: Float, alpha: Float = 1f, italic: Boolean = false, spacing: Float = 0f) =
        textPaint(face(weight, italic), size * u, withAlpha(WHITE, alpha), spacing = spacing)
            .apply { setShadowLayer(u * 0.45f, 0f, u * 0.1f, 0x66000000) }

    fun frame(width: Float, height: Float, color: Int = bg): Pair<Bitmap, Canvas> {
        val bmp = Bitmap.createBitmap(width.roundToInt(), height.roundToInt(), Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        cv.drawColor(color)
        return bmp to cv
    }

    // ---- Logo: brand artwork or the user's image, else the camera brand as a wordmark.

    private val brandText get() = info.brand.trim().uppercase()
    private fun brandPaint(h: Float, color: Int) = textPaint(face(800), h * 0.62f, color, spacing = 0.14f)
    private val scaledLogos = HashMap<String, Bitmap>()

    /**
     * Drawn size of a logo inside a box [h] tall. Wide wordmarks are drawn shorter than
     * square emblems so both carry similar visual weight.
     */
    private fun logoSize(h: Float, bmp: Bitmap): Pair<Float, Float> {
        val aspect = bmp.width.toFloat() / bmp.height
        val drawH = min(h * (1.1f - 0.14f * aspect).coerceIn(0.5f, 1f), h * 5f / aspect)
        return drawH * aspect to drawH
    }

    fun logoWidth(h: Float): Float {
        if (!style.showLogo || h <= 0f) return 0f
        logo?.let { return logoSize(h, it.light).first }
        if (brandText.isEmpty()) return 0f
        return brandPaint(h, fg).measureText(brandText)
    }

    fun drawLogo(cv: Canvas, x: Float, cy: Float, h: Float, align: Align, onDark: Boolean): Float {
        val w = logoWidth(h)
        if (w <= 0f) return 0f
        val left = when (align) {
            Align.LEFT -> x
            Align.CENTER -> x - w / 2
            Align.RIGHT -> x - w
        }
        val set = logo
        // Brand logos ship light/dark artwork, so Auto only recolours custom images.
        val tint = when (style.logoTint) {
            LogoTint.AUTO -> if (onDark && (set == null || set.custom)) WHITE else null
            LogoTint.ORIGINAL -> null
            LogoTint.WHITE -> WHITE
            LogoTint.BLACK -> INK
        }
        if (set != null) {
            val bmp = if (onDark) set.dark else set.light
            val drawH = logoSize(h, bmp).second
            val targetW = max(1, w.roundToInt())
            val scaled = scaledLogos.getOrPut("$targetW/$onDark") {
                if (bmp.width > targetW * 2) downscale(bmp, targetW) else bmp
            }
            val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            tint?.let { p.colorFilter = PorterDuffColorFilter(it, PorterDuff.Mode.SRC_IN) }
            cv.drawBitmap(scaled, null, RectF(left, cy - drawH / 2, left + w, cy + drawH / 2), p)
        } else {
            val p = brandPaint(h, tint ?: if (onDark) WHITE else INK)
            cv.drawText(brandText, left, cy + capH(p) / 2, p)
        }
        return w
    }

    // ---- Vertical stacks of text/logo, centred inside a band.

    private fun visible(items: List<Item>) = items.filter {
        when (it) {
            is Txt -> it.s.isNotBlank()
            is LogoItem -> logoWidth(it.h) > 0f
            is Block -> it.h > 0f && it.w > 0f
            is Gap -> true
        }
    }.dropWhile { it is Gap }.dropLastWhile { it is Gap }

    private fun itemH(it: Item) = when (it) {
        is Txt -> capH(it.p)
        is LogoItem -> it.h
        is Gap -> it.h
        is Block -> it.h
    }

    fun stackWidth(items: List<Item>): Float = visible(items).maxOfOrNull {
        when (it) {
            is Txt -> it.p.width(it.s)
            is LogoItem -> logoWidth(it.h)
            is Block -> it.w
            is Gap -> 0f
        }
    } ?: 0f

    fun stack(
        cv: Canvas,
        x: Float,
        align: Align,
        top: Float,
        bottom: Float,
        gap: Float,
        items: List<Item>,
        maxWidth: Float = Float.MAX_VALUE,
    ) {
        val live = visible(items)
        if (live.isEmpty()) return
        val content = live.count { it !is Gap }
        val total = live.sumOf { itemH(it).toDouble() }.toFloat() + gap * (content - 1)
        var y = top + (bottom - top - total) / 2
        var first = true
        for (it in live) {
            if (it !is Gap) {
                if (!first) y += gap
                first = false
            }
            when (it) {
                is Txt -> {
                    it.p.textAlign = align
                    cv.textFit(it.s, x, y + capH(it.p), it.p, maxWidth)
                }
                is LogoItem -> drawLogo(cv, x, y + it.h / 2, it.h, align, it.onDark)
                is Block -> {
                    val left = when (align) {
                        Align.LEFT -> x
                        Align.CENTER -> x - it.w / 2
                        Align.RIGHT -> x - it.w
                    }
                    it.draw(cv, left, y)
                }
                is Gap -> Unit
            }
            y += itemH(it)
        }
    }
}
