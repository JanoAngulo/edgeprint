package com.jrla.exifoneline.render

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache

class Brand(val id: String, val name: String)

/** Artwork for light and dark backgrounds. A custom logo uses the same image for both. */
class LogoSet(val light: Bitmap, val dark: Bitmap, val custom: Boolean)

/** Camera and phone brand logos bundled in assets/logos/{light,dark}. */
object BrandLogos {
    val all = listOf(
        Brand("samsung", "Samsung"), Brand("apple", "Apple"), Brand("apple_classic", "Apple (classic)"),
        Brand("google", "Google"), Brand("xiaomi", "Xiaomi"), Brand("huawei", "Huawei"),
        Brand("oppo", "OPPO"), Brand("vivo", "vivo"), Brand("oneplus", "OnePlus"),
        Brand("honor", "HONOR"), Brand("realme", "realme"), Brand("iqoo", "iQOO"),
        Brand("nubia", "nubia"), Brand("motorola", "Motorola"), Brand("infinix", "Infinix"),
        Brand("tecno", "TECNO"), Brand("itel", "itel"), Brand("nothing", "Nothing"),
        Brand("leica", "Leica"), Brand("canon", "Canon"), Brand("sony", "Sony"),
        Brand("sony_alpha", "Sony α"), Brand("sony_g", "Sony G Master"), Brand("nikon", "Nikon"),
        Brand("fujifilm", "Fujifilm"), Brand("lumix", "Lumix"), Brand("panasonic", "Panasonic"),
        Brand("olympus", "Olympus"), Brand("ricoh", "Ricoh"), Brand("ricoh_gr", "Ricoh GR"),
        Brand("pentax", "Pentax"), Brand("hasselblad", "Hasselblad"), Brand("hasselblad_h", "Hasselblad H"),
        Brand("dji", "DJI"), Brand("sigma", "Sigma"), Brand("tamron", "Tamron"),
        Brand("zeiss", "ZEISS"), Brand("zeiss_ikon", "Zeiss Ikon"), Brand("voigtlander", "Voigtländer"),
        Brand("kodak", "Kodak"), Brand("polaroid", "Polaroid"), Brand("agfa", "Agfa"),
        Brand("mamiya", "Mamiya"), Brand("phase_one", "Phase One"), Brand("contax", "Contax"),
        Brand("yashica", "Yashica"), Brand("rollei", "Rollei"), Brand("linhof", "Linhof"),
        Brand("sinar", "Sinar"), Brand("alpa", "Alpa"), Brand("casio", "Casio"), Brand("epson", "Epson"),
    )

    // First word of the EXIF make, lowercased -> logo id.
    private val makes = mapOf(
        "samsung" to "samsung", "apple" to "apple", "google" to "google", "xiaomi" to "xiaomi",
        "redmi" to "xiaomi", "huawei" to "huawei", "oppo" to "oppo", "vivo" to "vivo",
        "oneplus" to "oneplus", "honor" to "honor", "realme" to "realme", "nubia" to "nubia",
        "motorola" to "motorola", "infinix" to "infinix", "tecno" to "tecno", "itel" to "itel",
        "nothing" to "nothing",
        "leica" to "leica", "canon" to "canon", "sony" to "sony", "nikon" to "nikon",
        "fujifilm" to "fujifilm", "panasonic" to "lumix", "olympus" to "olympus", "om" to "olympus",
        "ricoh" to "ricoh", "pentax" to "pentax", "hasselblad" to "hasselblad", "dji" to "dji",
        "sigma" to "sigma", "kodak" to "kodak", "eastman" to "kodak", "polaroid" to "polaroid",
        "mamiya" to "mamiya", "phase" to "phase_one", "casio" to "casio", "epson" to "epson",
    )

    private lateinit var assets: AssetManager
    private val cache = object : LruCache<String, Bitmap>(32 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun init(context: Context) {
        if (!::assets.isInitialized) assets = context.applicationContext.assets
    }

    fun forBrand(brand: String): String? = makes[brand.trim().split(" ").firstOrNull()?.lowercase()]

    /** Decoded at no less than ~480 px tall, enough for a sharp logo on a 50 MP export. */
    fun load(id: String, dark: Boolean): Bitmap? {
        val key = (if (dark) "dark/" else "light/") + id
        cache.get(key)?.let { return it }
        val path = "logos/$key.png"
        val bmp = runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outHeight / (sample * 2) >= 480) sample *= 2
            assets.open(path).use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
        }.getOrNull() ?: return null
        cache.put(key, bmp)
        return bmp
    }

    /** What to draw for a photo; null means the brand name as text. */
    fun resolve(logoId: String, brand: String, custom: Bitmap?): LogoSet? {
        val id = when (logoId) {
            "custom" -> return custom?.let { LogoSet(it, it, custom = true) }
            "text" -> return null
            "auto" -> forBrand(brand) ?: return null
            else -> logoId
        }
        val light = load(id, dark = false) ?: return null
        return LogoSet(light, load(id, dark = true) ?: light, custom = false)
    }
}
