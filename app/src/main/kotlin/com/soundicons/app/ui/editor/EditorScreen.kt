package com.soundicons.app.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.soundicons.app.ui.crop.ImageCropSheet
import com.soundicons.app.ui.trim.SoundTrimSheet
import com.soundicons.app.viewmodel.EditorViewModel
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    iconId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val uiState     by viewModel.uiState.collectAsStateWithLifecycle()
    val playingId   by viewModel.audioPlayer.playingIconId.collectAsStateWithLifecycle()
    var showTrimSheet    by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onNavigateBack()
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let { uri -> viewModel.setAudioUri(uri) }
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        viewModel.setImageUri(it)
        // Auto-open crop sheet after picking image
        if (it != null) viewModel.showCropSheet(true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit Sound" else "New Sound", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    // Favorite toggle
                    IconButton(onClick = { viewModel.setFavorite(!uiState.isFavorite) }) {
                        Icon(if (uiState.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            "Favorite", tint = if (uiState.isFavorite) Color(0xFFFFD700) else LocalContentColor.current)
                    }
                    Button(onClick = viewModel::save, enabled = !uiState.isSaving, modifier = Modifier.padding(end = 8.dp)) {
                        if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else { Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Save", fontWeight = FontWeight.SemiBold) }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            // ── Icon Preview Banner ───────────────────────────────────────
            IconPreviewBanner(
                name       = uiState.name.ifBlank { "Icon Name" },
                displayUri = uiState.croppedImageUri ?: uiState.imageUri,
                onPickImage = { imagePickerLauncher.launch(arrayOf("image/*")) }
            )

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name
                FormSection("Icon Name") {
                    OutlinedTextField(value = uiState.name, onValueChange = viewModel::setName,
                        placeholder = { Text("e.g. Air Horn, Applause…") },
                        isError = uiState.nameError != null,
                        supportingText = uiState.nameError?.let { { Text(it) } },
                        singleLine = true, leadingIcon = { Icon(Icons.Rounded.Label, null) },
                        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                }

                // Audio file + Trim
                FormSection("Audio File") {
                    AudioPickerRow(
                        audioUri      = uiState.audioUri,
                        hasError      = uiState.audioError != null,
                        errorMessage  = uiState.audioError,
                        durationMs    = uiState.audioDurationMs,
                        trimStartMs   = uiState.trimStartMs,
                        trimEndMs     = uiState.trimEndMs,
                        onPickFile    = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                        onOpenTrim    = { showTrimSheet = true }
                    )
                }

                // Crop controls (only if image selected)
                AnimatedVisibility(!uiState.imageUri.isNullOrBlank()) {
                    FormSection("Image Crop") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.showCropSheet(true) }) {
                                Icon(Icons.Filled.Crop, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Edit Crop Shape")
                            }
                            if (!uiState.croppedImageUri.isNullOrBlank()) {
                                AssistChip(onClick = { viewModel.removeImage() },
                                    label = { Text("Remove Image") },
                                    leadingIcon = { Icon(Icons.Filled.Close, null, modifier = Modifier.size(14.dp)) })
                            }
                        }
                    }
                }

                // Category
                FormSection("Category (optional)") {
                    CategoryField(category = uiState.category, existingCategories = uiState.existingCategories, onCategoryChange = viewModel::setCategory)
                }

                // Volume
                FormSection("Volume  •  ${(uiState.volume * 100).toInt()}%") {
                    Slider(value = uiState.volume, onValueChange = viewModel::setVolume, modifier = Modifier.fillMaxWidth())
                }

                // Loop
                ToggleRow("Loop Audio", "Repeat until stopped", uiState.loopAudio, viewModel::setLoop)

                // Vibration — Feature 11
                ToggleRow("Vibration on Tap", "Vibrate when widget is tapped", uiState.vibrationEnabled, viewModel::setVibration)
                AnimatedVisibility(uiState.vibrationEnabled) {
                    Column {
                        Text("Vibration Duration: ${uiState.vibrationDurationMs}ms", style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = uiState.vibrationDurationMs / 1000f,
                            onValueChange = { viewModel.setVibrationDuration((it * 1000).toLong().coerceIn(50, 1000)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.height(60.dp))
            }
        }
    }

    // ── Sheets ────────────────────────────────────────────────────────────────


    if (uiState.showCropSheet && !uiState.imageUri.isNullOrBlank()) {
        ImageCropSheet(
            imageUri      = uiState.imageUri!!,
            selectedShape = uiState.cropShape,
            onShapeChange = viewModel::setCropShape,
            onConfirm     = { croppedPath -> viewModel.setCroppedImagePath(croppedPath) },
            onDismiss     = { viewModel.showCropSheet(false) }
        )
    }

    if (showTrimSheet && uiState.audioUri.isNotBlank()) {
        SoundTrimSheet(
            audioUri    = uiState.audioUri,
            durationMs  = uiState.audioDurationMs,
            trimStartMs = uiState.trimStartMs,
            trimEndMs   = uiState.trimEndMs,
            audioPlayer = viewModel.audioPlayer,
            playingId   = playingId,
            onStartChange = viewModel::setTrimStart,
            onEndChange   = viewModel::setTrimEnd,
            onConfirm     = { showTrimSheet = false },
            onDismiss     = { showTrimSheet = false }
        )
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

/**
 * Live preview banner shown at the top of the editor.
 * Mirrors the exact rendering logic of SoundIconCard:
 *   - Image present → image only, rounded clip, no colour background
 *   - No image      → fixed purple gradient + music note icon
 */
@Composable
private fun IconPreviewBanner(name: String, displayUri: String?, onPickImage: () -> Unit) {
    val hasImage       = !displayUri.isNullOrBlank()
    val placeholderBrush = Brush.linearGradient(
        listOf(Color(0xFF6650A4), Color(0xFF8B5CF6))
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D1B2A), Color(0xFF1B2838))))
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(88.dp)
                    .shadow(
                        elevation    = 16.dp,
                        shape        = RoundedCornerShape(22.dp),
                        ambientColor = Color.Black.copy(alpha = 0.4f),
                        spotColor    = Color.Black.copy(alpha = 0.5f)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    // Placeholder gradient only when no image — same as SoundIconCard
                    .then(
                        if (!hasImage) Modifier.background(placeholderBrush)
                        else Modifier
                    )
                    .clickable(onClick = onPickImage)
            ) {
                if (hasImage) {
                    // Image only — no colour background
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(displayUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Rounded.GraphicEq, null,
                        tint     = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(42.dp)
                    )
                }

                // Camera badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CameraAlt, null,
                        tint     = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                name,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    color    = Color.White
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Tap icon to change image",
                style    = MaterialTheme.typography.labelSmall,
                color    = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun FormSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun AudioPickerRow(audioUri: String, hasError: Boolean, errorMessage: String?,
                           durationMs: Long, trimStartMs: Long, trimEndMs: Long,
                           onPickFile: () -> Unit, onOpenTrim: () -> Unit) {
    val hasFile   = audioUri.isNotBlank()
    val displayName = if (hasFile) audioUri.substringAfterLast("/").let { if (it.length > 36) it.take(33) + "…" else it } else ""
    val effectiveEnd = if (trimEndMs <= 0 || trimEndMs > durationMs) durationMs else trimEndMs
    val isTrimmed = trimStartMs > 0 || (trimEndMs > 0 && trimEndMs < durationMs)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedCard(onClick = onPickFile,
            border = BorderStroke(1.5.dp, if (hasError) MaterialTheme.colorScheme.error
                else if (hasFile) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
            shape = MaterialTheme.shapes.medium) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                Icon(if (hasFile) Icons.Filled.AudioFile else Icons.Rounded.FolderOpen, null,
                    tint = if (hasFile) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (hasFile) "Audio file selected" else "Tap to choose audio file",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (hasFile) {
                        Text(displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                        if (durationMs > 0) {
                            Text(buildString {
                                append(formatMs(durationMs))
                                if (isTrimmed) append("  ·  Clip: ${formatMs(effectiveEnd - trimStartMs)}")
                            }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                        }
                    }
                }
                if (hasFile) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        if (hasFile) {
            TextButton(onClick = onOpenTrim, modifier = Modifier.align(Alignment.Start)) {
                Icon(Icons.Filled.ContentCut, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (isTrimmed) "Edit Trim  (${formatMs(effectiveEnd - trimStartMs)})" else "Trim Audio")
            }
        }
        if (hasError && errorMessage != null) {
            Text(errorMessage, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 16.dp))
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
        shape = MaterialTheme.shapes.medium) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
            }
            Switch(checked = checked, onCheckedChange = onToggle)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryField(category: String, existingCategories: List<String>, onCategoryChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && existingCategories.isNotEmpty(), onExpandedChange = { expanded = it }) {
        OutlinedTextField(value = category, onValueChange = onCategoryChange,
            placeholder = { Text("e.g. Memes, Animals, Music…") },
            leadingIcon = { Icon(Icons.Rounded.Category, null) },
            trailingIcon = { if (existingCategories.isNotEmpty()) ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            singleLine = true, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().menuAnchor())
        if (existingCategories.isNotEmpty()) {
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                existingCategories.forEach { cat ->
                    DropdownMenuItem(text = { Text(cat) }, onClick = { onCategoryChange(cat); expanded = false })
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val min = TimeUnit.MILLISECONDS.toMinutes(ms)
    val sec = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "%d:%02d".format(min, sec)
}
