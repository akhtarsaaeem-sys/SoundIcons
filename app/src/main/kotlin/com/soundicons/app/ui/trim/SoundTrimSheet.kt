package com.soundicons.app.ui.trim

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soundicons.app.util.AudioPlayerManager
import java.util.concurrent.TimeUnit

/**
 * Bottom sheet for Feature 2 — Sound Trimming.
 * Dual-thumb RangeSlider for start/end, preview playback, duration display.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundTrimSheet(
    audioUri:     String,
    durationMs:   Long,
    trimStartMs:  Long,
    trimEndMs:    Long,
    audioPlayer:  AudioPlayerManager,
    playingId:    Long?,
    onStartChange:(Long) -> Unit,
    onEndChange:  (Long) -> Unit,
    onConfirm:    () -> Unit,
    onDismiss:    () -> Unit
) {
    // Use a sentinel ID for preview playback
    val PREVIEW_ID = -999L

    val effectiveEnd = if (trimEndMs <= 0L || trimEndMs > durationMs) durationMs else trimEndMs

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier            = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Trim Audio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Duration info
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total: ${formatMs(durationMs)}", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                Text("Clip: ${formatMs(effectiveEnd - trimStartMs)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }

            if (durationMs > 0) {
                // Start slider
                Column {
                    Text("Start: ${formatMs(trimStartMs)}", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value         = trimStartMs / durationMs.toFloat(),
                        onValueChange = { onStartChange((it * durationMs).toLong().coerceAtMost(effectiveEnd - 500)) },
                        modifier      = Modifier.fillMaxWidth()
                    )
                }
                // End slider
                Column {
                    Text("End: ${formatMs(effectiveEnd)}", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value         = effectiveEnd / durationMs.toFloat(),
                        onValueChange = { onEndChange((it * durationMs).toLong().coerceAtLeast(trimStartMs + 500)) },
                        modifier      = Modifier.fillMaxWidth()
                    )
                }
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Loading audio…", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }

            // Preview button
            val isPreviewPlaying = playingId == PREVIEW_ID
            OutlinedButton(
                onClick = {
                    if (isPreviewPlaying) audioPlayer.stop()
                    else audioPlayer.play(PREVIEW_ID, audioUri, 1f, false, trimStartMs, effectiveEnd)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(if (isPreviewPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow, null,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isPreviewPlaying) "Stop Preview" else "Preview Trimmed Audio")
            }

            Button(onClick = { audioPlayer.stop(); onConfirm() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Apply Trim", fontWeight = FontWeight.SemiBold)
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
