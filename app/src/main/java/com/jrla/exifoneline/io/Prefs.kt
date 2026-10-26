package com.jrla.exifoneline.io

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.edit
import com.jrla.exifoneline.model.LogoTint
import com.jrla.exifoneline.model.Ratio
import com.jrla.exifoneline.model.Style
import com.jrla.exifoneline.model.Texts
import java.io.File

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var style: Style
        get() = Style(
            templateId = sp.getString("template", null) ?: Style().templateId,
            background = if (sp.contains("bg")) sp.getInt("bg", 0) else null,
            logoTint = enumOr(sp.getString("tint", null), LogoTint.AUTO),
            ratio = enumOr(sp.getString("ratio", null), Ratio.ORIGINAL),
            showLogo = sp.getBoolean("showLogo", true),
            showDate = sp.getBoolean("showDate", true),
            showLocation = sp.getBoolean("showLocation", true),
            showAuthor = sp.getBoolean("showAuthor", true),
            borderScale = sp.getFloat("border", 1f),
            logoId = sp.getString("logoId", null) ?: Style().logoId,
            font = sp.getString("font", null) ?: Style().font,
        )
        set(v) = sp.edit {
            putString("template", v.templateId)
            if (v.background == null) remove("bg") else putInt("bg", v.background)
            putString("tint", v.logoTint.name)
            putString("ratio", v.ratio.name)
            putBoolean("showLogo", v.showLogo)
            putBoolean("showDate", v.showDate)
            putBoolean("showLocation", v.showLocation)
            putBoolean("showAuthor", v.showAuthor)
            putFloat("border", v.borderScale)
            putString("logoId", v.logoId)
            putString("font", v.font)
        }

    var texts: Texts
        get() = Texts(
            author = sp.getString("author", null) ?: Texts().author,
            title = sp.getString("title", "").orEmpty(),
            quote = sp.getString("quote", "").orEmpty(),
        )
        set(v) = sp.edit {
            putString("author", v.author)
            putString("title", v.title)
            putString("quote", v.quote)
        }

    /** Longest side of exported images, in pixels. */
    var exportMax: Int
        get() = sp.getInt("exportMax", EXPORT_FULL)
        set(v) = sp.edit { putInt("exportMax", v) }

    var quality: Int
        get() = sp.getInt("quality", 95)
        set(v) = sp.edit { putInt("quality", v) }

    var keepGps: Boolean
        get() = sp.getBoolean("keepGps", true)
        set(v) = sp.edit { putBoolean("keepGps", v) }

    fun modelOverride(raw: String): String? = if (raw.isEmpty()) null else sp.getString("model:$raw", null)

    fun setModelOverride(raw: String, name: String) {
        if (raw.isEmpty()) return
        sp.edit { putString("model:$raw", name) }
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, fallback: T): T =
        name?.let { n -> enumValues<T>().firstOrNull { it.name == n } } ?: fallback

    companion object {
        /** Full resolution up to ~50 MP; 200 MP shots are scaled to this. */
        const val EXPORT_FULL = 8192
    }
}

/** The user's logo, normalised to a PNG in app storage. */
object LogoStore {
    private fun file(context: Context) = File(context.filesDir, "logo.png")

    fun load(context: Context): Bitmap? = file(context).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }

    fun save(context: Context, uri: Uri): Bitmap {
        val bmp = ImageIO.decode(context, uri, 1024)
        file(context).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bmp
    }

    fun clear(context: Context) {
        file(context).delete()
    }
}
