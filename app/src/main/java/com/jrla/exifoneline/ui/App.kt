package com.jrla.exifoneline.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jrla.exifoneline.io.Exporter

@Composable
fun App(vm: EditorViewModel) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is UiEvent.Saved -> {
                    val noun = if (event.count == 1) "photo" else "photos"
                    val message = buildString {
                        append("Saved ${event.count} $noun to Pictures/${Exporter.FOLDER}")
                        if (event.failed > 0) append(" · ${event.failed} failed")
                        if (event.reduced) append(" · some were reduced to fit memory")
                    }
                    val uri = event.uri
                    val shareable = uri != null && event.count == 1
                    val result = snackbar.showSnackbar(
                        message,
                        actionLabel = if (shareable) "Share" else null,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed && uri != null) share(context, uri)
                }
                is UiEvent.Error -> snackbar.showSnackbar(event.message)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Palette.Bg)) {
        if (vm.photos.isEmpty()) HomeScreen(vm) else EditorScreen(vm)

        SnackbarHost(
            snackbar,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 60.dp),
        ) { data ->
            Snackbar(
                data,
                containerColor = Palette.Raised,
                contentColor = Palette.Text,
                actionColor = Palette.Accent,
                shape = RoundedCornerShape(14.dp),
            )
        }
    }

    vm.busy?.let { message ->
        Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
            Surface(shape = RoundedCornerShape(20.dp), color = Palette.Raised) {
                Row(Modifier.padding(horizontal = 24.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Palette.Text, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(message, fontSize = 15.sp, color = Palette.Text)
                }
            }
        }
    }
}

private fun share(context: Context, uri: Uri) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Share photo"))
}
