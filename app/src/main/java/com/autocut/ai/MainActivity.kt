package com.autocut.ai

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

private val AppBackground = Color(0xFF0D1117)
private val Panel = Color(0xFF161D27)
private val Accent = Color(0xFF9B8CFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = AppBackground)) {
                AutoCutHome()
            }
        }
    }
}

@Composable
private fun AutoCutHome() {
    val context = LocalContext.current
    var selectedVideo by remember { mutableStateOf<Uri?>(null) }
    var videoName by remember { mutableStateOf("") }
    var player by remember { mutableStateOf<VideoView?>(null) }
    var durationMs by remember { mutableIntStateOf(0) }
    var positionMs by remember { mutableIntStateOf(0) }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedVideo = uri
        player = null
        durationMs = 0
        positionMs = 0
        videoName = uri?.let { getDisplayName(context, it) } ?: ""
    }

    LaunchedEffect(player, selectedVideo) {
        while (selectedVideo != null) {
            player?.let { view ->
                if (view.isPlaying) positionMs = view.currentPosition
                if (durationMs <= 0 && view.duration > 0) durationMs = view.duration
            }
            delay(300)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column {
                Text("AutoCut", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Editor video di perangkat Anda", color = Color(0xFF9AA6B5), fontSize = 14.sp)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Panel)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("PRATINJAU", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (selectedVideo == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(Color(0xFF090C11), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("▶", color = Accent, fontSize = 32.sp)
                                Spacer(Modifier.size(8.dp))
                                Text("Pilih video untuk mulai", color = Color(0xFF9AA6B5))
                            }
                        }
                    } else {
                        keyVideo(selectedVideo!!) {
                            AndroidView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f),
                                factory = { viewContext ->
                                    VideoView(viewContext).apply {
                                        setVideoURI(selectedVideo)
                                        val controls = MediaController(viewContext)
                                        controls.setAnchorView(this)
                                        setMediaController(controls)
                                        setOnPreparedListener { mediaPlayer ->
                                            durationMs = mediaPlayer.duration
                                            start()
                                        }
                                        player = this
                                    }
                                },
                                update = { player = it }
                            )
                        }
                    }
                    Text(
                        text = if (videoName.isBlank()) "Belum ada video dipilih" else videoName,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Panel)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TIMELINE", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${formatTime(positionMs)} / ${formatTime(durationMs)}", color = Color(0xFF9AA6B5), fontSize = 12.sp)
                    }
                    Slider(
                        value = positionMs.toFloat().coerceIn(0f, durationMs.coerceAtLeast(1).toFloat()),
                        onValueChange = { value ->
                            positionMs = value.toInt()
                            player?.seekTo(positionMs)
                        },
                        valueRange = 0f..durationMs.coerceAtLeast(1).toFloat(),
                        enabled = selectedVideo != null && durationMs > 0
                    )
                    Text("Geser penanda untuk mencari posisi video.", color = Color(0xFF9AA6B5), fontSize = 12.sp)
                }
            }

            Button(
                onClick = { videoPicker.launch("video/*") },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF11111A))
            ) {
                Text(if (selectedVideo == null) "Pilih video" else "Ganti video", fontWeight = FontWeight.Bold)
            }
            Text(
                "Video dipilih melalui pemilih file Android; aplikasi tidak meminta akses ke seluruh galeri.",
                color = Color(0xFF768293),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun keyVideo(uri: Uri, content: @Composable () -> Unit) {
    androidx.compose.runtime.key(uri) { content() }
}

private fun getDisplayName(context: android.content.Context, uri: Uri): String {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: "Video dipilih"
}

private fun formatTime(milliseconds: Int): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
