package com.jrla.edgeprint.render

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.jrla.edgeprint.model.PhotoInfo
import com.jrla.edgeprint.model.Ratio
import com.jrla.edgeprint.model.Style
import com.jrla.edgeprint.model.Texts
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The photo must land in the output pixel-for-pixel. A 1 px checkerboard turns grey
 * the moment it's drawn at a fractional offset and resampled.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class SharpnessTest {
    private val info = PhotoInfo(brand = "Sony", model = "A7 IV", focal = "35mm", aperture = "f/2", shutter = "1/250s", iso = "ISO100")

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<android.content.Context>()
        Fonts.init(app)
        BrandLogos.init(app)
    }

    private fun checker(w: Int, h: Int): Bitmap {
        val px = IntArray(w * h) { i -> if ((i % w + i / w) % 2 == 0) BLACK else WHITE }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }

    /** Counts pixels in a patch that are neither pure black nor pure white. */
    private fun greys(bmp: Bitmap, cx: Int, cy: Int, r: Int = 12): Int {
        var n = 0
        for (y in cy - r until cy + r) for (x in cx - r until cx + r) {
            val c = bmp.getPixel(x, y)
            if (c != BLACK && c != WHITE) n++
        }
        return n
    }

    // Photo-centred templates whose middle is never covered by text or effects.
    private val ids = listOf(
        "bar_classic", "poster_gallery", "poster_line", "poster_shadow", "poster_card", "poster_chips",
        "fr_border", "fr_instant", "fr_film35", "fr_film120", "fr_mat",
    )

    @Test
    fun photoIsNotResampled() {
        // Odd sizes give fractional margins (side = 2.6 * u etc.).
        val src = checker(1501, 997)
        for (id in ids) {
            val out = Renderer.render(src, info, Texts(author = "User"), Style(templateId = id), BrandLogos.resolve("auto", info.brand, null))
            val ox = (out.width - src.width) / 2
            assertEquals("$id softened the photo", 0, greys(out, ox + src.width / 2, src.height / 2 + (out.height - src.height) / 4))
        }
    }

    @Test
    fun ratioPaddingKeepsPixels() {
        val src = checker(1501, 997)
        for (ratio in Ratio.entries) {
            val out = Renderer.render(src, info, Texts(), Style(templateId = "ov_strip", ratio = ratio), null)
            assertEquals("$ratio softened the photo", 0, greys(out, out.width / 2, out.height / 2 - src.height / 4))
        }
    }
}
