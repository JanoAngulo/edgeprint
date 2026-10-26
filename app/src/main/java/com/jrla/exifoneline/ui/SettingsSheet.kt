package com.jrla.exifoneline.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jrla.exifoneline.io.Exporter
import com.jrla.exifoneline.io.Prefs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(vm: EditorViewModel, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Palette.Surface,
    ) {
        // Scrolls so every setting stays reachable when the sheet is short (landscape).
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text("Export", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)

            SectionLabel("Name on watermarks")
            Field("Photo by", vm.texts.author, { v -> vm.updateTexts { it.copy(author = v) } })

            SectionLabel("Size")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Prefs.EXPORT_FULL to "Full", 4096 to "4096 px", 2560 to "2560 px").forEach { (px, label) ->
                    Pill(label, vm.exportMax == px, { vm.changeExportMax(px) })
                }
            }
            Text(
                "Full keeps the original resolution (200 MP shots are scaled to 50 MP).",
                fontSize = 12.sp,
                color = Palette.Text2,
                modifier = Modifier.padding(top = 8.dp),
            )

            SectionLabel("JPEG quality  ${vm.quality}")
            Slider(
                value = vm.quality.toFloat(),
                onValueChange = { vm.changeQuality(it.roundToInt()) },
                valueRange = 80f..100f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = Palette.Text,
                    activeTrackColor = Palette.Text,
                    inactiveTrackColor = Palette.Line,
                    activeTickColor = Palette.Bg,
                    inactiveTickColor = Palette.Text2,
                ),
            )

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Keep GPS in saved files", fontSize = 15.sp, color = Palette.Text)
                    Text(
                        "Turn off before posting if you don't want to share where you were.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = Palette.Text2,
                    )
                }
                Switch(
                    checked = vm.keepGps,
                    onCheckedChange = vm::changeKeepGps,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Palette.Bg,
                        checkedTrackColor = Palette.Text,
                        uncheckedThumbColor = Palette.Text2,
                        uncheckedTrackColor = Palette.Raised,
                        uncheckedBorderColor = Palette.Line,
                    ),
                )
            }

            Text(
                "Saved to Pictures/${Exporter.FOLDER}. Your originals are never changed.",
                fontSize = 12.sp,
                color = Palette.Text2,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}
