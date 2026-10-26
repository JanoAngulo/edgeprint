package com.jrla.exifoneline.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.text.format.DateUtils
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jrla.exifoneline.io.ExifReader
import com.jrla.exifoneline.io.Exporter
import com.jrla.exifoneline.io.Models
import com.jrla.exifoneline.model.PhotoInfo
import com.jrla.exifoneline.model.Ratio
import com.jrla.exifoneline.render.BrandLogos
import com.jrla.exifoneline.render.Renderer
import com.jrla.exifoneline.render.Templates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.Locale

private const val MAX_PICK = 50
private val showcaseIds = listOf("bar_classic", "fr_film35", "poster_card", "ov_strip", "ed_magazine", "sp_blur")

fun mediaPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.ACCESS_MEDIA_LOCATION)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_MEDIA_LOCATION)
    }

private fun missing(context: Context) = mediaPermissions().filter {
    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
}

@Composable
fun HomeScreen(vm: EditorViewModel) {
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK)) {
        vm.open(it)
    }
    val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    // Photo access is only for reading GPS from the original files; picking works without it.
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        picker.launch(request)
    }

    // Null while the gallery query runs, so the showcase doesn't flash before the user's own photos.
    @Suppress("ProduceStateDoesNotAssignValue") // False positive on nullable state; value is assigned below.
    val saves by produceState<List<Saved>?>(null, vm.savedVersion) {
        value = withContext(Dispatchers.IO) { recentSaves(context) }
    }
    val hero = @Composable { modifier: Modifier, fill: Boolean ->
        val current = saves
        when {
            current == null -> Carousel(emptyList(), modifier, fill) {}
            current.isEmpty() -> Showcase(vm, modifier, fill)
            else -> LatestSaves(current, modifier, fill)
        }
    }
    val chooser = {
        val need = missing(context)
        if (need.isEmpty()) picker.launch(request) else permissions.launch(need.toTypedArray())
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.Bg).safeDrawingPadding()) {
        if (maxWidth > maxHeight && maxWidth >= 600.dp) {
            // Landscape: the picture can't take the full width, so it sits beside the intro.
            Column(Modifier.fillMaxSize()) {
                Header { showSettings = true }
                Row(Modifier.fillMaxSize().padding(bottom = 12.dp)) {
                    hero(Modifier.weight(1f).fillMaxHeight().padding(top = 4.dp), true)
                    Column(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Intro(chooser)
                    }
                }
            }
        } else {
            // Capped so tablets get a readable column instead of a stretched phone layout.
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Header { showSettings = true }
                hero(Modifier.padding(top = 12.dp), false)
                Spacer(Modifier.height(28.dp))
                Intro(chooser)
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showSettings) SettingsSheet(vm) { showSettings = false }
}

@Composable
private fun Header(onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Edgeprint", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Palette.Text2)
        }
    }
}

@Composable
private fun Intro(onChoose: () -> Unit) {
    Column(Modifier.padding(horizontal = 24.dp)) {
        Text(
            "Put your camera settings on your photos.",
            fontSize = 32.sp,
            lineHeight = 37.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.6).sp,
            color = Palette.Text,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Pick photos, choose a style, and save at full resolution. Your originals stay untouched.",
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = Palette.Text2,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onChoose,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Text, contentColor = Palette.Bg),
            modifier = Modifier.fillMaxWidth().height(58.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Choose photos", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "You can also share photos from Gallery to Edgeprint.",
            fontSize = 13.sp,
            color = Palette.Text2,
        )
    }
}

private class Slide(val label: String, val description: String, val bitmap: Bitmap)

/** An export in Pictures/Edgeprint (or the pre-rename folder); [added] is in milliseconds. */
private class Saved(val uri: Uri, val added: Long, val bitmap: Bitmap)

/** Real renders of a few templates, using the user's current font and logo. Shown until the first save. */
@Composable
private fun Showcase(vm: EditorViewModel, modifier: Modifier = Modifier, fill: Boolean = false) {
    val style = vm.style
    val texts = vm.texts
    val logo = vm.logo
    val slides by produceState<List<Slide>>(emptyList(), style.font, style.logoId, style.showLogo, style.logoTint, logo, texts) {
        value = withContext(Dispatchers.Default) {
            val src = sampleScene(1200, 900)
            val info = demoInfo()
            showcaseIds.map { id ->
                val s = style.copy(templateId = id, ratio = Ratio.ORIGINAL, background = null, borderScale = 1f)
                val template = Templates.byId(id)
                Slide(template.name, "Example: ${template.name} style", Renderer.render(src, info, texts, s, BrandLogos.resolve(s.logoId, info.brand, logo)))
            }
        }
    }
    Carousel(slides, modifier, fill) {
        Text("${Templates.all.size} styles", fontSize = 13.sp, color = Palette.Text2)
    }
}

/** The user's latest exports, in the showcase's place once there are any. */
@Composable
private fun LatestSaves(saves: List<Saved>, modifier: Modifier = Modifier, fill: Boolean = false) {
    val context = LocalContext.current
    val slides = remember(saves) {
        saves.map {
            val ago = DateUtils.getRelativeTimeSpanString(it.added, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
            Slide("Saved · $ago", "Your saved photo, $ago", it.bitmap)
        }
    }
    Carousel(slides, modifier, fill) { index ->
        TextButton(
            onClick = { openInGallery(context, saves[index].uri) },
            contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier.height(32.dp),
        ) {
            Text("Open", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Palette.Accent)
        }
    }
}

/**
 * Crossfades through [slides] slowly; tap to advance. Captioned like film-edge markings.
 * With [fill], the picture takes the height it's given instead of a fixed shape.
 */
@Composable
private fun Carousel(
    slides: List<Slide>,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
    trailing: @Composable (index: Int) -> Unit,
) {
    val context = LocalContext.current
    var index by remember(slides) { mutableIntStateOf(0) }
    val still = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    LaunchedEffect(slides, index, still) {
        if (slides.size < 2 || still) return@LaunchedEffect
        delay(3200)
        index = (index + 1) % slides.size
    }

    Column(modifier.fillMaxWidth()) {
        Box(
            (if (fill) Modifier.weight(1f).fillMaxWidth() else Modifier.fillMaxWidth().aspectRatio(1.24f))
                .padding(horizontal = 24.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    if (slides.isNotEmpty()) index = (index + 1) % slides.size
                },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = slides.getOrNull(index), animationSpec = tween(700), label = "carousel") { slide ->
                if (slide != null) {
                    val image = remember(slide.bitmap) { slide.bitmap.asImageBitmap() }
                    Image(
                        image,
                        contentDescription = slide.description,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 6.dp).height(32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (slides.isNotEmpty()) {
                Text(
                    "▶ " + String.format(Locale.US, "%02d", index + 1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Palette.Accent,
                )
                Spacer(Modifier.width(10.dp))
                Text(slides[index].label, fontSize = 13.sp, color = Palette.Text2)
                Spacer(Modifier.weight(1f))
                trailing(index)
            }
        }
    }
}

private fun recentSaves(context: Context, limit: Int = 6): List<Saved> = runCatching {
    val cr = context.contentResolver
    val base = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val rows = mutableListOf<Pair<Uri, Long>>()
    cr.query(
        base,
        arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED),
        "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?",
        arrayOf("Pictures/${Exporter.FOLDER}%", "Pictures/${Exporter.LEGACY_FOLDER}%"),
        "${MediaStore.Images.Media.DATE_ADDED} DESC",
    )?.use { c ->
        while (c.moveToNext() && rows.size < limit) {
            rows += ContentUris.withAppendedId(base, c.getLong(0)) to c.getLong(1) * 1000
        }
    }
    // Hero-sized rather than grid thumbnails: these fill the screen width.
    rows.mapNotNull { (uri, added) ->
        runCatching { Saved(uri, added, cr.loadThumbnail(uri, Size(1200, 1200), null)) }.getOrNull()
    }
}.getOrDefault(emptyList())

private fun openInGallery(context: Context, uri: Uri) {
    val view = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "image/jpeg")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(view) }
}

/** Sample values, using this phone's own name so the preview feels like the user's camera. */
private fun demoInfo(): PhotoInfo {
    val now = LocalDateTime.now().withSecond(9)
    return PhotoInfo(
        brand = Models.brand(Build.MANUFACTURER),
        model = Models.friendly(Build.MODEL),
        focal = "24mm",
        aperture = "f/1.7",
        shutter = "1/120s",
        iso = "ISO50",
        date = now.format(ExifReader.displayDate),
        location = "Manila, Philippines",
        dateTime = now,
    )
}

/** A dusk landscape drawn in code, so the showcase needs no bundled photo. */
private fun sampleScene(w: Int, h: Int): Bitmap {
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.drawPaint(Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(0xFF1F2A44.toInt(), 0xFF6B4A5E.toInt(), 0xFFC9694A.toInt(), 0xFFF0B45A.toInt()),
            floatArrayOf(0f, 0.38f, 0.68f, 0.86f),
            Shader.TileMode.CLAMP,
        )
    })
    val aa = Paint(Paint.ANTI_ALIAS_FLAG)
    aa.color = 0xFFFFF0D2.toInt()
    c.drawCircle(w * 0.68f, h * 0.6f, h * 0.06f, aa)
    fun ridge(color: Int, vararg ys: Float) {
        val p = Path().apply {
            moveTo(0f, h * ys[0])
            ys.forEachIndexed { i, y -> lineTo(w * i / (ys.size - 1f), h * y) }
            lineTo(w.toFloat(), h.toFloat())
            lineTo(0f, h.toFloat())
            close()
        }
        aa.color = color
        c.drawPath(p, aa)
    }
    ridge(0xFF8A4A3C.toInt(), 0.7f, 0.56f, 0.64f, 0.52f, 0.66f, 0.62f)
    ridge(0xFF4A2A2C.toInt(), 0.82f, 0.72f, 0.78f, 0.7f, 0.8f, 0.76f)
    ridge(0xFF1C1416.toInt(), 0.92f, 0.86f, 0.9f, 0.88f, 0.84f, 0.9f)
    return bmp
}
