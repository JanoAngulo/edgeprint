package com.jrla.exifoneline.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jrla.exifoneline.io.ExifReader
import com.jrla.exifoneline.io.Exporter
import com.jrla.exifoneline.io.Geo
import com.jrla.exifoneline.io.ImageIO
import com.jrla.exifoneline.io.LogoStore
import com.jrla.exifoneline.io.Prefs
import com.jrla.exifoneline.model.PhotoInfo
import com.jrla.exifoneline.model.Ratio
import com.jrla.exifoneline.model.Style
import com.jrla.exifoneline.model.Texts
import com.jrla.exifoneline.render.BrandLogos
import com.jrla.exifoneline.render.Fonts
import com.jrla.exifoneline.render.Renderer
import com.jrla.exifoneline.render.Templates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PhotoItem(
    val uri: Uri,
    val name: String,
    val exifUri: Uri,
    val rawModel: String,
    val info: PhotoInfo,
)

sealed interface UiEvent {
    data class Saved(val count: Int, val failed: Int, val uri: Uri?, val reduced: Boolean) : UiEvent
    data class Error(val message: String) : UiEvent
}

private const val PREVIEW_PX = 1600
private const val THUMB_PX = 480

class EditorViewModel(app: Application) : AndroidViewModel(app) {
    private val context get() = getApplication<Application>()
    private val prefs = Prefs(app)

    var photos by mutableStateOf<List<PhotoItem>>(emptyList())
        private set
    var index by mutableIntStateOf(0)
        private set
    var style by mutableStateOf(prefs.style)
        private set
    var texts by mutableStateOf(prefs.texts)
        private set
    var logo by mutableStateOf<Bitmap?>(null)
        private set
    var preview by mutableStateOf<Bitmap?>(null)
        private set
    var thumbs by mutableStateOf<Map<String, Bitmap>>(emptyMap())
        private set
    var small by mutableStateOf<Map<Uri, Bitmap>>(emptyMap())
        private set
    var busy by mutableStateOf<String?>(null)
        private set
    var exportMax by mutableIntStateOf(prefs.exportMax)
        private set
    var quality by mutableIntStateOf(prefs.quality)
        private set
    /** Bumped after each export so the home screen refreshes its recent saves. */
    var savedVersion by mutableIntStateOf(0)
        private set
    var keepGps by mutableStateOf(prefs.keepGps)
        private set

    val events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)

    val current: PhotoItem? get() = photos.getOrNull(index)

    private val previews = object : LruCache<Uri, Bitmap>(96 * 1024 * 1024) {
        override fun sizeOf(key: Uri, value: Bitmap) = value.byteCount
    }

    init {
        Fonts.init(app)
        BrandLogos.init(app)
        viewModelScope.launch {
            logo = withContext(Dispatchers.IO) { LogoStore.load(context) }
        }
        watchPreview()
        watchThumbs()
    }

    // ---- Live rendering

    private data class PreviewKey(val item: PhotoItem?, val style: Style, val texts: Texts, val logo: Bitmap?)

    private fun watchPreview() = viewModelScope.launch {
        snapshotFlow { PreviewKey(current, style, texts, logo) }.collectLatest { key ->
            val item = key.item
            if (item == null) {
                preview = null
                return@collectLatest
            }
            delay(30)
            val out = withContext(Dispatchers.Default) {
                runCatching {
                    val src = previews.get(item.uri)
                        ?: ImageIO.decode(context, item.uri, PREVIEW_PX).also { previews.put(item.uri, it) }
                    Renderer.render(src, item.info, key.texts, key.style, BrandLogos.resolve(key.style.logoId, item.info.brand, key.logo))
                }.getOrNull()
            }
            preview = out
        }
    }

    private data class ThumbKey(val src: Bitmap?, val info: PhotoInfo?, val style: Style, val texts: Texts, val logo: Bitmap?)

    private fun watchThumbs() = viewModelScope.launch {
        snapshotFlow {
            val item = current
            ThumbKey(item?.let { small[it.uri] }, item?.info, style.copy(templateId = "", ratio = Ratio.ORIGINAL), texts, logo)
        }.collectLatest { key ->
            val src = key.src ?: return@collectLatest
            delay(150)
            thumbs = withContext(Dispatchers.Default) {
                Templates.all.associate { t ->
                    val info = key.info ?: PhotoInfo()
                    t.id to Renderer.render(src, info, key.texts, key.style.copy(templateId = t.id), BrandLogos.resolve(key.style.logoId, info.brand, key.logo))
                }
            }
        }
    }

    // ---- Opening photos

    fun open(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            busy = "Reading photos…"
            val loaded = withContext(Dispatchers.IO) {
                uris.distinct().mapNotNull { uri -> runCatching { load(uri) }.getOrNull() }
            }
            busy = null
            if (loaded.isEmpty()) {
                events.emit(UiEvent.Error("Couldn't open those photos"))
                return@launch
            }
            previews.evictAll()
            photos = loaded.map { it.item }
            small = loaded.associate { it.item.uri to it.thumb }
            index = 0
            loaded.forEach { l -> l.gps?.let { geocode(l.item.uri, it.first, it.second) } }
        }
    }

    private class Loaded(val item: PhotoItem, val thumb: Bitmap, val gps: Pair<Double, Double>?)

    private fun load(uri: Uri): Loaded {
        val exif = ExifReader.read(context, uri) { prefs.modelOverride(it) }
        val thumb = ImageIO.decode(context, uri, THUMB_PX)
        val item = PhotoItem(uri, ImageIO.displayName(context, uri), exif.exifUri, exif.rawModel, exif.info)
        val gps = if (exif.lat != null && exif.lon != null) exif.lat to exif.lon else null
        return Loaded(item, thumb, gps)
    }

    private fun geocode(uri: Uri, lat: Double, lon: Double) = viewModelScope.launch {
        val place = Geo.placeName(context, lat, lon) ?: Geo.coords(lat, lon)
        photos = photos.map {
            if (it.uri == uri && it.info.location.isBlank()) it.copy(info = it.info.copy(location = place)) else it
        }
    }

    fun select(i: Int) {
        if (i in photos.indices) index = i
    }

    fun close() {
        photos = emptyList()
        small = emptyMap()
        thumbs = emptyMap()
        index = 0
        previews.evictAll()
    }

    // ---- Editing

    fun updateInfo(transform: (PhotoInfo) -> PhotoInfo) {
        val uri = current?.uri ?: return
        photos = photos.map { if (it.uri == uri) it.copy(info = transform(it.info)) else it }
    }

    /** Renaming a camera applies to every photo from it and is remembered for next time. */
    fun setModel(name: String) {
        val item = current ?: return
        prefs.setModelOverride(item.rawModel, name)
        photos = photos.map {
            val same = it.uri == item.uri || (item.rawModel.isNotEmpty() && it.rawModel == item.rawModel)
            if (same) it.copy(info = it.info.copy(model = name)) else it
        }
    }

    fun updateStyle(transform: (Style) -> Style) {
        style = transform(style)
        prefs.style = style
    }

    fun updateTexts(transform: (Texts) -> Texts) {
        texts = transform(texts)
        prefs.texts = texts
    }

    fun setLogo(uri: Uri) = viewModelScope.launch {
        busy = "Loading logo…"
        val bmp = withContext(Dispatchers.IO) { runCatching { LogoStore.save(context, uri) }.getOrNull() }
        busy = null
        if (bmp == null) {
            events.emit(UiEvent.Error("Couldn't read that image"))
            return@launch
        }
        logo = bmp
        updateStyle { it.copy(showLogo = true, logoId = "custom") }
    }

    fun clearLogo() {
        LogoStore.clear(context)
        logo = null
        if (style.logoId == "custom") updateStyle { it.copy(logoId = "auto") }
    }

    fun changeExportMax(px: Int) {
        exportMax = px
        prefs.exportMax = px
    }

    fun changeQuality(q: Int) {
        quality = q
        prefs.quality = q
    }

    fun changeKeepGps(keep: Boolean) {
        keepGps = keep
        prefs.keepGps = keep
    }

    // ---- Export

    fun save(all: Boolean) {
        val items = if (all) photos else listOfNotNull(current)
        if (items.isEmpty() || busy != null) return
        val style = style
        val texts = texts
        val logo = logo
        viewModelScope.launch {
            var failed = 0
            var last: Uri? = null
            var reduced = false
            items.forEachIndexed { i, item ->
                busy = if (items.size == 1) "Saving…" else "Saving ${i + 1} of ${items.size}…"
                val result = withContext(Dispatchers.Default) { exportOne(item, style, texts, logo) }
                if (result == null) {
                    failed++
                } else {
                    last = result.first
                    reduced = reduced || result.second
                }
            }
            busy = null
            savedVersion++
            events.emit(UiEvent.Saved(items.size - failed, failed, last, reduced))
        }
    }

    /** Renders at full size, stepping down if the phone runs out of memory. Second value: was it reduced. */
    private fun exportOne(item: PhotoItem, style: Style, texts: Texts, logo: Bitmap?): Pair<Uri, Boolean>? {
        val sizes = listOf(exportMax, 4096, 2560).filter { it <= exportMax }.distinct()
        for ((attempt, maxLong) in sizes.withIndex()) {
            try {
                val src = ImageIO.decode(context, item.uri, maxLong)
                val out = try {
                    Renderer.render(src, item.info, texts, style, BrandLogos.resolve(style.logoId, item.info.brand, logo))
                } finally {
                    src.recycle()
                }
                try {
                    return Exporter.save(context, out, item.name, item.exifUri, quality, keepGps) to (attempt > 0)
                } finally {
                    out.recycle()
                }
            } catch (e: OutOfMemoryError) {
                System.gc()
            } catch (e: Exception) {
                return null
            }
        }
        return null
    }
}
