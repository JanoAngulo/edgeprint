package com.jrla.exifoneline.render

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Typeface

/** A bundled Google Font (all OFL). Variable fonts, so any weight can be instanced. */
class FontSpec(val id: String, val label: String, val file: String, val italicFile: String? = null)

/** Variable fonts bundled in assets, instanced per weight and cached. */
object Fonts {
    val all = listOf(
        FontSpec("inter", "Inter", "fonts/inter.ttf", "fonts/inter_italic.ttf"),
        FontSpec("manrope", "Manrope", "fonts/manrope.ttf"),
        FontSpec("dmsans", "DM Sans", "fonts/dmsans.ttf"),
        FontSpec("plex", "IBM Plex Sans", "fonts/plexsans.ttf"),
        FontSpec("spacegrotesk", "Space Grotesk", "fonts/spacegrotesk.ttf"),
        FontSpec("outfit", "Outfit", "fonts/outfit.ttf"),
        FontSpec("sora", "Sora", "fonts/sora.ttf"),
        FontSpec("oswald", "Oswald", "fonts/oswald.ttf"),
        FontSpec("playfair", "Playfair Display", "fonts/playfair.ttf", "fonts/playfair_italic.ttf"),
        FontSpec("cormorant", "Cormorant", "fonts/cormorant.ttf"),
    )

    private lateinit var assets: AssetManager
    private val cache = HashMap<String, Typeface>()

    fun init(context: Context) {
        if (!::assets.isInitialized) assets = context.applicationContext.assets
    }

    fun spec(id: String) = all.firstOrNull { it.id == id } ?: all.first()

    private fun load(path: String, weight: Int): Typeface = synchronized(cache) {
        cache.getOrPut("$path@$weight") {
            runCatching {
                Typeface.Builder(assets, path).setFontVariationSettings("'wght' $weight").build()
            }.getOrNull() ?: Typeface.DEFAULT
        }
    }

    /** The chosen family at [weight]; fonts without an italic file get a synthetic slant. */
    fun face(id: String, weight: Int, italic: Boolean = false): Typeface {
        val s = spec(id)
        if (!italic) return load(s.file, weight)
        s.italicFile?.let { return load(it, weight) }
        return synchronized(cache) {
            cache.getOrPut("${s.file}@$weight/i") { Typeface.create(load(s.file, weight), Typeface.ITALIC) }
        }
    }

    fun sans(weight: Int = 400) = face("inter", weight)
    fun serifItalic(weight: Int = 400) = load("fonts/playfair_italic.ttf", weight)
    val mono: Typeface by lazy { load("fonts/jetbrains_mono.ttf", 700) }
}
