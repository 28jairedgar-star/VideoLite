package com.videolite

import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private val Background = Color(0xFF101114)
private val Surface = Color(0xFF1B1D22)
private val Accent = Color(0xFF8B7CFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VideoLiteApp() }
    }
}

@Composable
fun VideoLiteApp() {
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var videoName by remember { mutableStateOf("Ningún video seleccionado") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            videoUri = uri
            videoName = uri.lastPathSegment?.substringAfterLast('/') ?: "Video seleccionado"
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Accent, background = Background, surface = Surface,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Column(
            modifier = Modifier.fillMaxSize().background(Background).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Video", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                Text("Lite", color = Accent, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.weight(1f))
                Text("0.1.0", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.fillMaxWidth().weight(1f).background(Color(0xFF08090B), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                val context = LocalContext.current
                if (videoUri != null) {
                    AndroidView(
                        factory = { ctx -> VideoView(ctx).apply { setVideoURI(videoUri); setOnPreparedListener { it.isLooping = true; start() } } },
                        update = { it.setVideoURI(videoUri) },
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Add, null, tint = Accent, modifier = Modifier.size(52.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Tu próximo video comienza aquí", color = Color.White)
                        Text("Selecciona un archivo para empezar", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(videoName, color = Color.LightGray, maxLines = 1)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { picker.launch(arrayOf("video/*")) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Seleccionar video")
            }
            Spacer(Modifier.height(18.dp))
            Text("HERRAMIENTAS", color = Color.Gray, style = MaterialTheme.typography.labelMedium, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolTile("Recortar", Icons.Default.ContentCut, Modifier.weight(1f))
                ToolTile("Música", Icons.Default.MusicNote, Modifier.weight(1f))
                ToolTile("Velocidad", Icons.Default.Speed, Modifier.weight(1f))
                ToolTile("Ajustes", Icons.Default.Tune, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Text("Editor ligero · Interfaz optimizada para móvil", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ToolTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.height(82.dp), color = Surface, shape = RoundedCornerShape(14.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Accent)
            Spacer(Modifier.height(6.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}
