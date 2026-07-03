package com.soundicons.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.soundicons.app.data.model.SoundIcon
import com.soundicons.app.data.model.WidgetMapping
import com.soundicons.app.ui.theme.SoundIconsTheme
import com.soundicons.app.viewmodel.WidgetConfigViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

private const val TAG = "WidgetConfigureActivity"

/**
 * Widget configuration Activity shown when the user places a widget.
 *
 * Flow:
 *   1. User picks a sound icon from the (scrollable) list
 *   2. User picks display style: Icon Only  OR  Icon + Name
 *   3. (Optional) User adjusts per-widget volume
 *   4. Tap "Add Widget" → done
 *
 * No widget size selection — the launcher controls all sizing.
 *
 * Layout structure (per request):
 *   Column {
 *       LazyColumn(Modifier.weight(1f)) { ... sound list ... }
 *       BottomBar { Button(...) }   // sticky, always visible
 *   }
 *
 * The bottom bar uses .navigationBarsPadding() so the "Add Widget" button
 * is never hidden behind 3-button nav or gesture nav on any device
 * (notably Samsung's larger nav bar).
 */
@AndroidEntryPoint
class WidgetConfigureActivity : ComponentActivity() {

    private val viewModel: WidgetConfigViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)   // default; changed to OK on confirm

        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "Invalid widget ID"); finish(); return
        }

        enableEdgeToEdge()
        setContent {
            SoundIconsTheme {
                WidgetConfigScreen(
                    viewModel  = viewModel,
                    onConfirm  = { soundIcon, displayMode, volumePct ->
                        viewModel.saveWidget(this, appWidgetId, soundIcon, volumePct, displayMode)
                        val result = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        setResult(RESULT_OK, result)
                        finish()
                    },
                    onCancel = { setResult(RESULT_CANCELED); finish() }
                )
            }
        }
    }
}

// ── Compose UI ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(
    viewModel: WidgetConfigViewModel,
    onConfirm: (SoundIcon, String, Int) -> Unit,
    onCancel:  () -> Unit
) {
    val icons       by viewModel.allIcons.collectAsStateWithLifecycle()
    var selected    by remember { mutableStateOf<SoundIcon?>(null) }
    var displayMode by remember { mutableStateOf(WidgetMapping.DisplayMode.ICON_AND_NAME) }
    var volumePct   by remember { mutableIntStateOf(100) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Add Sound Widget", fontWeight = FontWeight.Bold)
                        Text(
                            "Choose a sound and display style",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, "Cancel")
                    }
                },
                // Top bar should also respect the status bar inset
                windowInsets = TopAppBarDefaults.windowInsets
            )
        }
        // No bottomBar slot here — the sticky CTA is built manually inside the
        // content Column below so we have full control over insets padding.
    ) { padding ->

        // ── Outer Column: list takes remaining space, CTA is pinned below ────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {

            // ── Scrollable sound picker ───────────────────────────────────
            if (icons.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier.weight(1f).fillMaxWidth().padding(32.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.MusicNote, null,
                            tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            "No sounds yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Open Sound Icons and create some sounds first.",
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier            = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Style + volume controls scroll with the list when a sound
                    // is selected — keeps the picker compact when nothing chosen.
                    if (selected != null) {
                        item {
                            StyleSelector(
                                displayMode  = displayMode,
                                onModeChange = { displayMode = it }
                            )
                        }
                        item {
                            VolumeRow(
                                volumePct      = volumePct,
                                onVolumeChange = { volumePct = it }
                            )
                        }
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                "Select Sound",
                                style      = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.primary,
                                modifier   = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                        }
                    }

                    items(icons, key = { it.id }) { icon ->
                        SoundRow(
                            icon       = icon,
                            isSelected = icon.id == selected?.id,
                            onSelect   = { selected = icon }
                        )
                    }

                    // Extra bottom spacer so the last item never sits flush
                    // against the sticky CTA bar above the nav-bar padding.
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }

            // ── Sticky bottom CTA — always fully visible ─────────────────────
            // Surface gives it a solid background + elevation so it visually
            // separates from the scrolling list above.
            //
            // CRITICAL: .navigationBarsPadding() is applied to the OUTERMOST
            // modifier chain of this container so the button's hit area and
            // visible bounds are pushed entirely above the system nav bar —
            // covers 3-button nav, gesture nav, and Samsung's taller nav bar.
            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = {
                            selected?.let { onConfirm(it, displayMode, volumePct) }
                        },
                        enabled  = selected != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (selected != null) "Add \"${selected!!.name}\" Widget"
                            else "Select a sound above",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ── Style selector — two chips only: Icon Only / Icon + Name ─────────────────

@Composable
private fun StyleSelector(
    displayMode:  String,
    onModeChange: (String) -> Unit
) {
    Column {
        Text(
            "Display Style",
            style      = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                WidgetMapping.DisplayMode.ICON_ONLY     to "Icon Only",
                WidgetMapping.DisplayMode.ICON_AND_NAME to "Icon + Name"
            ).forEach { (mode, label) ->
                FilterChip(
                    selected = displayMode == mode,
                    onClick  = { onModeChange(mode) },
                    label    = { Text(label) },
                    leadingIcon = if (displayMode == mode) ({
                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(14.dp))
                    }) else null
                )
            }
        }
        Text(
            when (displayMode) {
                WidgetMapping.DisplayMode.ICON_ONLY ->
                    "Icon fills the widget. Resize freely in any direction."
                else ->
                    "Name label always visible below icon. Resize freely."
            },
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// ── Volume slider row ──────────────────────────────────────────────────────────

@Composable
private fun VolumeRow(
    volumePct:      Int,
    onVolumeChange: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Volume: $volumePct%",
                style    = MaterialTheme.typography.labelLarge,
                modifier = Modifier.width(100.dp)
            )
            Slider(
                value         = volumePct / 100f,
                onValueChange = { onVolumeChange((it * 100).toInt()) },
                modifier      = Modifier.weight(1f)
            )
        }
    }
}

// ── Sound picker row ──────────────────────────────────────────────────────────

@Composable
private fun SoundRow(
    icon:       SoundIcon,
    isSelected: Boolean,
    onSelect:   () -> Unit
) {
    val displayUri = icon.croppedImageUri ?: icon.imageUri
    val placeholderBrush = Brush.linearGradient(
        listOf(Color(0xFF6650A4), Color(0xFF8B5CF6))
    )

    Card(
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(
                    2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)
                ) else Modifier
            )
            .clickable(onClick = onSelect)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier              = Modifier.padding(12.dp)
        ) {
            // Thumbnail
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .then(
                        if (displayUri.isNullOrBlank())
                            Modifier.background(placeholderBrush)
                        else Modifier
                    )
            ) {
                if (!displayUri.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(
                                if (File(displayUri).exists()) displayUri else displayUri
                            )
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(13.dp))
                    )
                } else {
                    Text(
                        text  = icon.name.firstOrNull()?.uppercaseChar()?.toString() ?: "♪",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(icon.name, fontWeight = FontWeight.SemiBold)
                Text(
                    buildString {
                        append("Vol ${(icon.volume * 100).toInt()}%")
                        if (icon.loopAudio) append(" · Loop")
                        if (icon.trimEndMs > 0) append(" · Trimmed")
                        if (!icon.category.isNullOrBlank()) append(" · ${icon.category}")
                    },
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Filled.Check, null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}