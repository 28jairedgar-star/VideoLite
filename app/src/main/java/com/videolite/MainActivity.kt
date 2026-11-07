package com.videolite

import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private val Background = Color(0xFF101114)
private val Panel = Color(0xFF1B1D22)
private val Accent = Color(0xFF8B7CFF)
private data class MediaItem(val id: Int, val type: String, val name: String, val uri: Uri? = null)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VideoLiteApp() }
    }
}

@Composable
fun VideoLiteApp() {
    val context = LocalContext.current
    val items = remember { mutableStateListOf<MediaItem>() }
    var nextId by remember { mutableIntStateOf(1) }
    var editorOpen by remember { mutableStateOf(false) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }
    var selectedId by remember { mutableIntStateOf(-1) }
    var textDialog by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf("") }
    var infoDialog by remember { mutableStateOf<String?>(null) }

    fun addPicked(type: String, uris: List<Uri>) {
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { }
            val name = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
                ?.takeIf { it.isNotBlank() } ?: type
            items.add(MediaItem(nextId++, type, name, uri))
            if (type == "Video" && previewUri == null) previewUri = uri
        }
        if (uris.isNotEmpty()) {
            editorOpen = true
            if (selectedId == -1) selectedId = items.last().id
        }
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { addPicked("Video", it) }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { addPicked("Audio", it) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { addPicked("Foto", it) }

    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Background, surface = Panel, onBackground = Color.White, onSurface = Color.White)) {
        if (!editorOpen) {
            Column(Modifier.fillMaxSize().background(Background).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Video", color = Color.White, style = MaterialTheme.typography.headlineLarge)
                    Text("Lite", color = Accent, style = MaterialTheme.typography.headlineLarge)
                }
                Spacer(Modifier.height(14.dp))
                Text("Editor de video multipista", color = Color.LightGray)
                Spacer(Modifier.height(30.dp))
                Button(onClick = { videoPicker.launch(arrayOf("video/*")) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Icons.Default.VideoLibrary, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Crear proyecto / Añadir videos")
                }
                Spacer(Modifier.height(12.dp))
                Text("Selecciona uno o varios videos para abrir el editor.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            EditorScreen(
                items = items, previewUri = previewUri, selectedId = selectedId,
                onSelect = { selectedId = it },
                onBack = { editorOpen = false },
                onAddVideo = { videoPicker.launch(arrayOf("video/*")) },
                onAddAudio = { audioPicker.launch(arrayOf("audio/*")) },
                onAddPhoto = { photoPicker.launch(arrayOf("image/*")) },
                onAddText = { textDialog = true },
                onAddExtra = {
                    items.add(MediaItem(nextId++, "Extra", "Elemento adicional"))
                    selectedId = items.last().id
                },
                onDelete = {
                    val target = items.firstOrNull { it.id == selectedId }
                    if (target != null) {
                        items.remove(target)
                        if (previewUri == target.uri) previewUri = items.firstOrNull { it.type == "Video" }?.uri
                        selectedId = items.lastOrNull()?.id ?: -1
                    }
                },
                onTool = { infoDialog = it }
            )
        }

        if (textDialog) {
            AlertDialog(
                onDismissRequest = { textDialog = false },
                title = { Text("Añadir texto") },
                text = { OutlinedTextField(value = textValue, onValueChange = { textValue = it }, label = { Text("Contenido") }) },
                confirmButton = {
                    TextButton(onClick = {
                        if (textValue.isNotBlank()) {
                            items.add(MediaItem(nextId++, "Texto", textValue.trim()))
                            selectedId = items.last().id
                            editorOpen = true
                        }
                        textValue = ""
                        textDialog = false
                    }) { Text("Añadir a pista") }
                },
                dismissButton = { TextButton(onClick = { textDialog = false }) { Text("Cancelar") } }
            )
        }
        infoDialog?.let { title ->
            AlertDialog(
                onDismissRequest = { infoDialog = null },
                title = { Text(title) },
                text = { Text("El botón ya responde. Esta función todavía no procesa ni exporta el video; primero estamos construyendo el editor multipista y la gestión de medios.") },
                confirmButton = { TextButton(onClick = { infoDialog = null }) { Text("Entendido") } }
            )
        }
    }
}

@Composable
private fun EditorScreen(
    items: SnapshotStateList<MediaItem>, previewUri: Uri?, selectedId: Int,
    onSelect: (Int) -> Unit, onBack: () -> Unit,
    onAddVideo: () -> Unit, onAddAudio: () -> Unit, onAddPhoto: () -> Unit,
    onAddText: () -> Unit, onAddExtra: () -> Unit, onDelete: () -> Unit, onTool: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().background(Background)) {
        Row(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text("VideoLite", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text("Proyecto · ${items.size} elementos", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = { onTool("Exportar") }) { Text("Exportar") }
        }

        Box(Modifier.fillMaxWidth().weight(1f).padding(10.dp).background(Color(0xFF08090B), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            if (previewUri != null) {
                key(previewUri) {
                    AndroidView(
                        factory = { ctx -> VideoView(ctx).apply {
                            setVideoURI(previewUri)
                            setOnPreparedListener { player -> player.isLooping = true; start() }
                        } },
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.VideoLibrary, null, tint = Accent, modifier = Modifier.size(42.dp))
                    Text("Vista previa", color = Color.White)
                    Text("Añade un video para visualizarlo", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ToolButton("Cortar", Icons.Default.ContentCut, Modifier.weight(1f)) { onTool("Cortar") }
            ToolButton("Audio", Icons.Default.MusicNote, Modifier.weight(1f)) { onTool("Mezcla de audio") }
            ToolButton("Velocidad", Icons.Default.Speed, Modifier.weight(1f)) { onTool("Velocidad") }
            ToolButton("Ajustes", Icons.Default.Tune, Modifier.weight(1f)) { onTool("Ajustes") }
        }

        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("LÍNEA DE TIEMPO", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDelete, enabled = selectedId != -1) {
                Icon(Icons.Default.Delete, "Eliminar elemento", tint = if (selectedId != -1) Color(0xFFFF7777) else Color.Gray)
            }
        }

        Column(Modifier.fillMaxWidth().heightIn(min = 230.dp, max = 300.dp).background(Color(0xFF15161A)).verticalScroll(rememberScrollState()).padding(vertical = 5.dp)) {
            TrackRow("VIDEO", Icons.Default.VideoLibrary, items.filter { it.type == "Video" }, selectedId, Color(0xFF5546A8), onSelect, onAddVideo)
            TrackRow("AUDIO", Icons.Default.MusicNote, items.filter { it.type == "Audio" }, selectedId, Color(0xFF176B59), onSelect, onAddAudio)
            TrackRow("FOTO", Icons.Default.Image, items.filter { it.type == "Foto" }, selectedId, Color(0xFF9A6530), onSelect, onAddPhoto)
            TrackRow("TEXTO", Icons.Default.TextFields, items.filter { it.type == "Texto" }, selectedId, Color(0xFF8A477D), onSelect, onAddText)
            TrackRow("ADICIONAL", Icons.Default.Add, items.filter { it.type == "Extra" }, selectedId, Color(0xFF536A83), onSelect, onAddExtra)
        }

        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AddChip("Video", Icons.Default.VideoLibrary, onAddVideo)
            AddChip("Audio", Icons.Default.MusicNote, onAddAudio)
            AddChip("Foto", Icons.Default.Image, onAddPhoto)
            AddChip("Texto", Icons.Default.TextFields, onAddText)
            AddChip("Extra", Icons.Default.Add, onAddExtra)
        }
    }
}

@Composable
private fun TrackRow(title: String, icon: ImageVector, clips: List<MediaItem>, selectedId: Int, tint: Color, onSelect: (Int) -> Unit, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 9.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(78.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp))
                Text(title, color = Color.LightGray, fontSize = 10.sp)
            }
            Text("${clips.size} clip(s)", color = Color.Gray, fontSize = 9.sp)
        }
        Row(Modifier.weight(1f).height(48.dp).horizontalScroll(rememberScrollState()).background(Color(0xFF202127), RoundedCornerShape(8.dp)).padding(3.dp), verticalAlignment = Alignment.CenterVertically) {
            clips.forEach { clip ->
                Surface(onClick = { onSelect(clip.id) }, color = if (selectedId == clip.id) tint else tint.copy(alpha = 0.55f), shape = RoundedCornerShape(6.dp), modifier = Modifier.padding(end = 4.dp).width(112.dp).fillMaxHeight()) {
                    Box(Modifier.padding(horizontal = 7.dp), contentAlignment = Alignment.CenterStart) {
                        Text(clip.name, color = Color.White, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (clips.isEmpty()) Text("Añade elementos con +", color = Color.DarkGray, fontSize = 10.sp)
        }
        IconButton(onClick = onAdd, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Add, "Añadir a $title", tint = Accent) }
    }
}

@Composable
private fun ToolButton(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier.height(48.dp), contentPadding = PaddingValues(horizontal = 3.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(16.dp))
            Text(label, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun AddChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)) {
        Icon(icon, null, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(3.dp))
        Text(label, fontSize = 11.sp)
    }
}
