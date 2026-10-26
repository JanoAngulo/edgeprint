package com.jrla.exifoneline.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorScreen(vm: EditorViewModel) {
    BackHandler { vm.close() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    val count = vm.photos.size

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg)
            .safeDrawingPadding(),
    ) {
        val sideWidth = (maxWidth * 0.42f).coerceIn(340.dp, 440.dp)
        if (maxWidth > maxHeight && maxWidth >= 600.dp) {
            // Landscape: height is scarce, so the photo gets all of it and the actions
            // sit above the tools on the side, like other photo editors.
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Preview(vm, Modifier.weight(1f).fillMaxWidth())
                    if (count > 1) PhotoStrip(vm)
                }
                Column(Modifier.width(sideWidth).fillMaxHeight().background(Palette.Surface)) {
                    TopBar(vm, count) { showSettings = true }
                    Tabs(selected = tab, onSelect = { tab = it })
                    Panel(vm, tab, Modifier.weight(1f).fillMaxWidth())
                }
            }
        } else {
            // Tall screens like tablets give the panel more room; phones keep 272dp.
            val panelHeight = (maxHeight * 0.3f).coerceAtLeast(272.dp)
            Column(Modifier.fillMaxSize()) {
                TopBar(vm, count) { showSettings = true }
                Preview(vm, Modifier.weight(1f).fillMaxWidth())
                if (count > 1) PhotoStrip(vm)
                Tabs(selected = tab, onSelect = { tab = it })
                Panel(vm, tab, Modifier.fillMaxWidth().height(panelHeight).background(Palette.Surface))
            }
        }
    }

    if (showSettings) SettingsSheet(vm) { showSettings = false }
}

@Composable
private fun TopBar(vm: EditorViewModel, count: Int, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = vm::close) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Palette.Text)
        }
        Text(
            if (count > 1) "${vm.index + 1} of $count" else "Edit",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Palette.Text,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Export settings", tint = Palette.Text2)
        }
        if (count > 1) {
            TextButton(onClick = { vm.save(all = true) }) {
                Text("Save all", color = Palette.Text, fontWeight = FontWeight.Medium)
            }
        }
        Button(
            onClick = { vm.save(all = false) },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Text, contentColor = Palette.Bg),
            contentPadding = PaddingValues(horizontal = 20.dp),
            modifier = Modifier.padding(end = 8.dp).height(40.dp),
        ) {
            Text("Save", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Preview(vm: EditorViewModel, modifier: Modifier) {
    Box(modifier.padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        val bmp = vm.preview
        if (bmp != null) {
            val image = remember(bmp) { bmp.asImageBitmap() }
            Image(image, contentDescription = "Preview", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        } else {
            CircularProgressIndicator(color = Palette.Text2, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun Panel(vm: EditorViewModel, tab: Int, modifier: Modifier) {
    Box(modifier) {
        when (tab) {
            0 -> StylePanel(vm)
            1 -> DetailsPanel(vm)
            else -> LookPanel(vm)
        }
    }
}

@Composable
private fun PhotoStrip(vm: EditorViewModel) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(vm.photos, key = { _, p -> p.uri.toString() }) { i, photo ->
            val selected = i == vm.index
            val thumb = vm.small[photo.uri]
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.dp, if (selected) Palette.Text else Palette.Bg, RoundedCornerShape(10.dp))
                    .background(Palette.Raised)
                    .clickable { vm.select(i) },
            ) {
                if (thumb != null) {
                    val image = remember(thumb) { thumb.asImageBitmap() }
                    Image(image, contentDescription = photo.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun Tabs(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Palette.Surface)
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        listOf("Style", "Details", "Look").forEachIndexed { i, label ->
            val active = i == selected
            Column(
                Modifier
                    .selectable(selected = active, role = Role.Tab, onClick = { onSelect(i) })
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    label,
                    fontSize = 15.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) Palette.Text else Palette.Text2,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .width(18.dp)
                        .height(2.dp)
                        .background(if (active) Palette.Accent else Palette.Surface, RoundedCornerShape(1.dp)),
                )
            }
        }
    }
}
