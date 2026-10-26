package com.jrla.exifoneline.ui

import android.content.res.AssetManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Palette {
    val Bg = Color(0xFF000000)
    val Surface = Color(0xFF141414)
    val Raised = Color(0xFF1F1F1F)
    val Line = Color(0xFF2E2E2E)
    val Text = Color(0xFFF2F2F0)
    val Text2 = Color(0xFF9A9AA0)
    val Accent = Color(0xFFF0A040)
}

@OptIn(ExperimentalTextApi::class)
private fun inter(assets: AssetManager) = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { w ->
        Font(
            "fonts/inter.ttf",
            assets,
            weight = w,
            variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)),
        )
    },
)

private fun typography(f: FontFamily): Typography {
    val t = Typography()
    // Material's default tracking reads loose with Inter; tighten it.
    fun TextStyle.inter() = copy(fontFamily = f, letterSpacing = (letterSpacing.value.coerceAtMost(0.1f)).sp)
    return Typography(
        displayLarge = t.displayLarge.inter(),
        displayMedium = t.displayMedium.inter(),
        displaySmall = t.displaySmall.inter(),
        headlineLarge = t.headlineLarge.inter(),
        headlineMedium = t.headlineMedium.inter(),
        headlineSmall = t.headlineSmall.inter(),
        titleLarge = t.titleLarge.inter(),
        titleMedium = t.titleMedium.inter(),
        titleSmall = t.titleSmall.inter(),
        bodyLarge = t.bodyLarge.inter(),
        bodyMedium = t.bodyMedium.inter(),
        bodySmall = t.bodySmall.inter(),
        labelLarge = t.labelLarge.inter(),
        labelMedium = t.labelMedium.inter(),
        labelSmall = t.labelSmall.inter(),
    )
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val type = remember { typography(inter(context.assets)) }
    val scheme = darkColorScheme(
        primary = Palette.Text,
        onPrimary = Palette.Bg,
        secondary = Palette.Accent,
        onSecondary = Palette.Bg,
        background = Palette.Bg,
        onBackground = Palette.Text,
        surface = Palette.Surface,
        onSurface = Palette.Text,
        surfaceVariant = Palette.Raised,
        onSurfaceVariant = Palette.Text2,
        surfaceContainer = Palette.Surface,
        surfaceContainerLow = Palette.Surface,
        surfaceContainerHigh = Palette.Raised,
        surfaceContainerHighest = Palette.Raised,
        primaryContainer = Palette.Raised,
        onPrimaryContainer = Palette.Text,
        secondaryContainer = Palette.Raised,
        onSecondaryContainer = Palette.Text,
        outline = Palette.Line,
        outlineVariant = Palette.Line,
    )
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}

/** A single-weight family for previewing a bundled font in its own picker label. */
@OptIn(ExperimentalTextApi::class)
fun previewFamily(assets: AssetManager, file: String) = FontFamily(
    Font(file, assets, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
)
