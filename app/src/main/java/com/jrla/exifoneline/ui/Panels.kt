package com.jrla.exifoneline.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jrla.exifoneline.model.LogoTint
import com.jrla.exifoneline.model.Ratio
import com.jrla.exifoneline.render.Family
import com.jrla.exifoneline.render.Templates
import java.util.Locale
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.produceState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.jrla.exifoneline.render.BrandLogos
import com.jrla.exifoneline.render.Fonts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---- Style: pick a template, filtered by family.

@Composable
fun StylePanel(vm: EditorViewModel) {
    var family by rememberSaveable { mutableStateOf<String?>(null) }
    val shown = Templates.all.filter { family == null || it.family.name == family }
    // Thumbnails grow into taller panels (tablets, landscape) instead of leaving them empty.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val thumbHeight = minOf(maxHeight - 120.dp, maxWidth * 0.4f).coerceIn(150.dp, 360.dp)
        Column(Modifier.fillMaxSize()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { Pill("All", family == null, { family = null }) }
                items(Family.entries) { f -> Pill(f.label, family == f.name, { family = f.name }) }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                items(shown, key = { it.id }) { t ->
                    val selected = vm.style.templateId == t.id
                    val thumb = vm.thumbs[t.id]
                    Column(
                        Modifier.clickable { vm.updateStyle { it.copy(templateId = t.id) } },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val shape = RoundedCornerShape(10.dp)
                        val frame = Modifier
                            .height(thumbHeight)
                            .clip(shape)
                            .border(2.dp, if (selected) Palette.Text else Color.Transparent, shape)
                            .background(Palette.Raised)
                        if (thumb != null) {
                            val image = remember(thumb) { thumb.asImageBitmap() }
                            Image(
                                image,
                                contentDescription = t.name,
                                contentScale = ContentScale.Fit,
                                modifier = frame.aspectRatio(thumb.width.toFloat() / thumb.height).padding(4.dp),
                            )
                        } else {
                            Box(frame.width(thumbHeight * 0.73f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            t.name,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) Palette.Text else Palette.Text2,
                        )
                    }
                }
            }
        }
    }
}

// ---- Details: the editable text drawn on the watermark.

@Composable
fun DetailsPanel(vm: EditorViewModel) {
    val info = vm.current?.info ?: return
    val texts = vm.texts
    val style = vm.style
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SectionLabel("This photo") }
        item { Field("Camera", info.model, vm::setModel) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Focal length", info.focal, { v -> vm.updateInfo { it.copy(focal = v) } }, Modifier.weight(1f))
                Field("Aperture", info.aperture, { v -> vm.updateInfo { it.copy(aperture = v) } }, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Shutter", info.shutter, { v -> vm.updateInfo { it.copy(shutter = v) } }, Modifier.weight(1f))
                Field("ISO", info.iso, { v -> vm.updateInfo { it.copy(iso = v) } }, Modifier.weight(1f))
            }
        }
        item { Field("Date", info.date, { v -> vm.updateInfo { it.copy(date = v) } }) }
        item { Field("Location", info.location, { v -> vm.updateInfo { it.copy(location = v) } }, placeholder = "City, Country") }
        item { Field("Brand (picks the Auto logo)", info.brand, { v -> vm.updateInfo { it.copy(brand = v) } }) }

        item { SectionLabel("All photos") }
        item { Field("Photo by", texts.author, { v -> vm.updateTexts { it.copy(author = v) } }) }
        item { Field("Title", texts.title, { v -> vm.updateTexts { it.copy(title = v) } }, placeholder = "Magazine headline (defaults to the month)") }
        item { Field("Quote", texts.quote, { v -> vm.updateTexts { it.copy(quote = v) } }, placeholder = "Shown on Dark panel") }

        item { SectionLabel("Show on watermark") }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Logo", style.showLogo, { vm.updateStyle { it.copy(showLogo = !it.showLogo) } })
                Pill("Date", style.showDate, { vm.updateStyle { it.copy(showDate = !it.showDate) } })
                Pill("Location", style.showLocation, { vm.updateStyle { it.copy(showLocation = !it.showLocation) } })
                Pill("Name", style.showAuthor, { vm.updateStyle { it.copy(showAuthor = !it.showAuthor) } })
            }
        }
    }
}

// ---- Look: colours, ratio, logo and border.

private val swatches = listOf(
    0xFFFFFFFF, 0xFFF7F5F0, 0xFFE9E9E6, 0xFFE8DCC8, 0xFF000000,
    0xFF1E1E1E, 0xFF1F3A2E, 0xFFA1261E, 0xFF4A3A2C, 0xFF20263A,
).map { it.toInt() }

@Composable
fun LookPanel(vm: EditorViewModel) {
    val style = vm.style
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.setLogo(it) }
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
    ) {
        SectionLabel("Logo")
        LogoPicker(vm, onPickImage = { logoPicker.launch("image/*") })
        if (style.showLogo && style.logoId == "custom" && vm.logo != null) {
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { logoPicker.launch("image/*") },
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Palette.Line),
                ) { Text("Replace image", color = Palette.Text) }
                TextButton(onClick = vm::clearLogo) { Text("Remove", color = Palette.Text2) }
            }
        }

        SectionLabel("Font")
        val context = LocalContext.current
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Fonts.all.forEach { f ->
                val family = remember(f.id) { previewFamily(context.assets, f.file) }
                Pill(f.label, style.font == f.id, { vm.updateStyle { it.copy(font = f.id) } }, fontFamily = family)
            }
        }

        SectionLabel("Background")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Swatch(null, style.background == null) { vm.updateStyle { it.copy(background = null) } }
            swatches.forEach { c -> Swatch(c, style.background == c) { vm.updateStyle { it.copy(background = c) } } }
        }

        SectionLabel("Aspect ratio")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Ratio.entries.forEach { r -> Pill(r.label, style.ratio == r, { vm.updateStyle { it.copy(ratio = r) } }) }
        }

        SectionLabel("Logo colour")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LogoTint.entries.forEach { t -> Pill(t.label, style.logoTint == t, { vm.updateStyle { it.copy(logoTint = t) } }) }
        }

        SectionLabel("Border size  ×" + String.format(Locale.US, "%.1f", style.borderScale))
        Slider(
            value = style.borderScale,
            onValueChange = { v -> vm.updateStyle { it.copy(borderScale = v) } },
            valueRange = 0.5f..2f,
            colors = SliderDefaults.colors(
                thumbColor = Palette.Text,
                activeTrackColor = Palette.Text,
                inactiveTrackColor = Palette.Line,
            ),
        )
    }
}

@Composable
private fun Swatch(color: Int?, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .border(2.dp, if (selected) Palette.Text else Color.Transparent, CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(if (color == null) Palette.Raised else Color(color))
            .border(1.dp, Palette.Line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (color == null) Text("Auto", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)
    }
}

@Composable
private fun LogoPicker(vm: EditorViewModel, onPickImage: () -> Unit) {
    val style = vm.style
    val brand = vm.current?.info?.brand.orEmpty()
    val autoId = BrandLogos.forBrand(brand)
    fun on(id: String) = style.showLogo && style.logoId == id
    fun choose(id: String) = vm.updateStyle { it.copy(showLogo = true, logoId = id) }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            LogoTile("No logo", !style.showLogo, { vm.updateStyle { it.copy(showLogo = false) } }) {
                Canvas(Modifier.size(22.dp)) {
                    drawCircle(Palette.Text2, style = Stroke(2.dp.toPx()))
                    drawLine(Palette.Text2, Offset(size.width * 0.2f, size.height * 0.8f), Offset(size.width * 0.8f, size.height * 0.2f), 2.dp.toPx())
                }
            }
        }
        item {
            LogoTile("Auto", on("auto"), { choose("auto") }) {
                if (autoId != null) BrandImage(autoId) else WordmarkPreview(brand)
            }
        }
        item { LogoTile("Text", on("text"), { choose("text") }) { WordmarkPreview(brand) } }
        item {
            LogoTile("Your image", on("custom"), { if (vm.logo == null) onPickImage() else choose("custom") }) {
                val logo = vm.logo
                if (logo != null) {
                    val image = remember(logo) { logo.asImageBitmap() }
                    Image(image, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(8.dp))
                } else {
                    Text("+", fontSize = 22.sp, color = Palette.Bg)
                }
            }
        }
        items(BrandLogos.all, key = { it.id }) { b ->
            LogoTile(b.name, on(b.id), { choose(b.id) }) { BrandImage(b.id) }
        }
    }
}

@Composable
private fun LogoTile(label: String, selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(Modifier.width(84.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(width = 84.dp, height = 52.dp)
                .clip(shape)
                .background(Color(0xFFF2F2F0))
                .border(2.dp, if (selected) Palette.Accent else Color.Transparent, shape),
            contentAlignment = Alignment.Center,
        ) { content() }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontSize = 11.sp,
            color = if (selected) Palette.Text else Palette.Text2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BrandImage(id: String) {
    @Suppress("ProduceStateDoesNotAssignValue") // False positive on nullable state; value is assigned below.
    val bmp by produceState<Bitmap?>(null, id) {
        value = withContext(Dispatchers.IO) { BrandLogos.load(id, dark = false) }
    }
    bmp?.let {
        val image = remember(it) { it.asImageBitmap() }
        Image(image, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 9.dp))
    }
}

@Composable
private fun WordmarkPreview(brand: String) {
    Text(
        brand.uppercase().ifEmpty { "BRAND" },
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.sp,
        color = Color(0xFF161616),
        maxLines = 1,
    )
}
