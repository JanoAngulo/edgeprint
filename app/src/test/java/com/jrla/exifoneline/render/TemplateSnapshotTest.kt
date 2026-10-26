package com.jrla.exifoneline.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.test.core.app.ApplicationProvider
import com.jrla.exifoneline.model.PhotoInfo
import com.jrla.exifoneline.model.Style
import com.jrla.exifoneline.model.Texts
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDateTime

/** Renders every template on landscape and portrait sample images into build/snapshots for review. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class TemplateSnapshotTest {
    private val info = PhotoInfo(
        brand = "Samsung",
        model = "Galaxy S26 Ultra",
        focal = "24mm",
        aperture = "f/1.7",
        shutter = "1/120s",
        iso = "ISO50",
        date = "2026.10.25 17:42",
        location = "Manila, Philippines",
        dateTime = LocalDateTime.of(2026, 10, 25, 17, 42, 9),
    )
    private val texts = Texts(author = "User", quote = "Light is the only story worth telling")

    private fun sample(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawPaint(Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h.toFloat(), intArrayOf(0xFF26324A.toInt(), 0xFFB4654A.toInt(), 0xFFE9B44C.toInt()), null, Shader.TileMode.CLAMP)
        })
        val hill = Path().apply {
            moveTo(0f, h * 0.75f); lineTo(w * 0.3f, h * 0.52f); lineTo(w * 0.5f, h * 0.66f)
            lineTo(w * 0.66f, h * 0.58f); lineTo(w.toFloat(), h * 0.76f); lineTo(w.toFloat(), h.toFloat()); lineTo(0f, h.toFloat()); close()
        }
        c.drawPath(hill, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2A1A1E.toInt() })
        return bmp
    }

    @Test
    fun renderAll() {
        val app = ApplicationProvider.getApplicationContext<android.content.Context>()
        Fonts.init(app)
        BrandLogos.init(app)
        val out = File("build/snapshots").apply { mkdirs() }
        val shots = mapOf("land" to sample(1500, 1000), "port" to sample(1000, 1500))
        for ((tag, src) in shots) {
            for (t in Templates.all) {
                val bmp = Renderer.render(src, info, texts, Style(templateId = t.id), BrandLogos.resolve("auto", info.brand, null))
                File(out, "${tag}_${t.id}.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
        val src = shots.getValue("land")
        for ((logo, font) in listOf("leica" to "manrope", "fujifilm" to "oswald", "nikon" to "playfair", "sony" to "spacegrotesk")) {
            for (id in listOf("bar_classic", "poster_card", "ov_strip")) {
                val bmp = Renderer.render(src, info, texts, Style(templateId = id, logoId = logo, font = font), BrandLogos.resolve(logo, info.brand, null))
                File(out, "mix_${logo}_${font}_$id.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
        assertTrue(out.listFiles()!!.size >= Templates.all.size * 2)
    }
}
