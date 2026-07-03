package com.soundicons.app.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soundicons.app.data.model.SoundIcon
import com.soundicons.app.ui.components.DeleteConfirmDialog
import com.soundicons.app.ui.components.SoundIconCard
import com.soundicons.app.ui.components.WithMediaPermissions
import com.soundicons.app.viewmodel.FilterMode
import com.soundicons.app.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToEditor:   (Long?) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    val playingIconId by viewModel.audioPlayer.playingIconId.collectAsStateWithLifecycle()
    val snackbarHost  = remember { SnackbarHostState() }
    val haptic        = LocalHapticFeedback.current

    var iconToDelete by remember { mutableStateOf<SoundIcon?>(null) }
    var showVolume   by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }

    // ── Drag-and-drop state ───────────────────────────────────────────────────
    var draggingId        by remember { mutableStateOf<Long?>(null) }
    var dragTargetIndex   by remember { mutableIntStateOf(-1) }
    var dragOffset        by remember { mutableStateOf(Offset.Zero) }

    // Map of icon id → its grid cell position on screen (centre point)
    val cellPositions = remember { mutableStateMapOf<Long, Offset>() }

    val widgetIconIds = remember(uiState.widgetMappings) {
        uiState.widgetMappings.map { it.soundIconId }.toSet()
    }
    val playingIcon = remember(playingIconId, uiState.icons) {
        uiState.icons.find { it.id == playingIconId }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    val gridState = rememberLazyGridState()

    WithMediaPermissions {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF0D1B2A),
                            0.45f to Color(0xFF1B2838),
                            0.85f to Color(0xFF0A1628),
                            1.0f to Color(0xFF050D18)
                        )
                    )
                )
        ) {
            // Accent glow
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .offset(x = 160.dp, y = (-40).dp)
                    .graphicsLayer { alpha = 0.99f }
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF6650A4).copy(alpha = 0.2f), Color.Transparent)
                        )
                    )
            )

            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost   = { SnackbarHost(snackbarHost) },
                topBar = {
                    Column {
                        if (searchActive) {
                            // ── M3 SearchBar ──────────────────────────────────
                            SearchBar(
                                query          = uiState.searchQuery,
                                onQueryChange  = viewModel::setSearchQuery,
                                onSearch       = { /* real-time — no action needed */ },
                                active         = true,
                                onActiveChange = { active ->
                                    searchActive = active
                                    if (!active) viewModel.setSearchQuery("")
                                },
                                placeholder    = {
                                    Text(
                                        "Search by name or category…",
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                },
                                leadingIcon  = {
                                    Icon(Icons.Filled.Search, null, tint = Color.White.copy(0.7f))
                                },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        searchActive = false
                                        viewModel.setSearchQuery("")
                                    }) {
                                        Icon(
                                            Icons.Filled.Close, "Close search",
                                            tint = Color.White.copy(0.7f)
                                        )
                                    }
                                },
                                colors = SearchBarDefaults.colors(
                                    containerColor = Color(0xFF1B2838),
                                    inputFieldColors = TextFieldDefaults.colors(
                                        focusedTextColor   = Color.White,
                                        unfocusedTextColor = Color.White,
                                        cursorColor        = Color.White
                                    )
                                ),
                                modifier       = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                tonalElevation = 0.dp
                            ) {
                                // Suggestions slot — show result count
                                if (uiState.searchQuery.isNotBlank()) {
                                    Text(
                                        text     = "${uiState.icons.size} result${if (uiState.icons.size != 1) "s" else ""} for \"${uiState.searchQuery}\"",
                                        style    = MaterialTheme.typography.labelMedium,
                                        color    = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            // ── Normal TopAppBar ──────────────────────────────
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(
                                            "Sound Icons",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color      = Color.White
                                            )
                                        )
                                        Text(
                                            "${uiState.icons.size} sound${if (uiState.icons.size != 1) "s" else ""}  ·  " +
                                            "${uiState.widgetMappings.size} widget${if (uiState.widgetMappings.size != 1) "s" else ""}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color    = Color.White.copy(alpha = 0.5f),
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.White.copy(alpha = 0.07f)
                                ),
                                actions = {
                                    IconButton(onClick = { searchActive = true }) {
                                        Icon(Icons.Filled.Search, "Search", tint = Color.White)
                                    }
                                    IconButton(onClick = { showVolume = !showVolume }) {
                                        Icon(Icons.Filled.VolumeUp, "Volume", tint = Color.White)
                                    }
                                    IconButton(onClick = onNavigateToSettings) {
                                        Icon(Icons.Filled.Settings, "Settings", tint = Color.White)
                                    }
                                }
                            )
                        }

                        // ── Filter chips (hidden during search) ───────────────
                        if (!searchActive) {
                            FilterChipsRow(
                                filterMode       = uiState.filterMode,
                                categories       = uiState.categories,
                                selectedCategory = uiState.selectedCategory,
                                onFilterMode     = viewModel::setFilterMode,
                                onCategory       = viewModel::setCategory
                            )
                        }
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Volume panel
                    AnimatedVisibility(
                        visible = showVolume,
                        enter   = expandVertically() + fadeIn(),
                        exit    = shrinkVertically() + fadeOut()
                    ) {
                        VolumeRow(
                            volume        = uiState.masterVolume,
                            onVolumeChange = viewModel::setMasterVolume
                        )
                    }

                    // ── Icon grid ─────────────────────────────────────────────
                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            uiState.isLoading       -> CircularProgressIndicator(
                                color    = Color.White,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            uiState.icons.isEmpty() -> EmptyState(
                                filterMode  = uiState.filterMode,
                                searchQuery = uiState.searchQuery
                            )
                            else -> LazyVerticalGrid(
                                columns               = GridCells.Fixed(4),
                                state                 = gridState,
                                contentPadding        = PaddingValues(
                                    horizontal = 20.dp, vertical = 24.dp
                                ),
                                verticalArrangement   = Arrangement.spacedBy(28.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier              = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(
                                    items = uiState.icons,
                                    key   = { _, icon -> icon.id }
                                ) { index, icon ->
                                    val isDraggingThis = draggingId == icon.id
                                    val isDropTarget   = dragTargetIndex == index && draggingId != null

                                    SoundIconCard(
                                        icon        = icon,
                                        isPlaying   = icon.id == playingIconId,
                                        hasWidget   = icon.id in widgetIconIds,
                                        isDragging  = isDraggingThis,
                                        onTap       = { viewModel.playSound(icon) },
                                        onEdit      = { onNavigateToEditor(icon.id) },
                                        onDelete    = { iconToDelete = icon },
                                        onFavorite  = { viewModel.toggleFavorite(icon) },
                                        modifier    = Modifier
                                            .animateItem()
                                            // Record cell centre position for drag target detection
                                            .onGloballyPositioned { coords ->
                                                val pos = coords.positionInWindow()
                                                val size = coords.size
                                                cellPositions[icon.id] = Offset(
                                                    pos.x + size.width / 2f,
                                                    pos.y + size.height / 2f
                                                )
                                            }
                                            // Drag gesture — owns the long-press.
                                            // SoundIconCard's icon body only has onTap
                                            // so this handler wins the long-press uncontested.
                                            .pointerInput(icon.id, uiState.icons.size) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { startOffset ->
                                                        draggingId = icon.id
                                                        haptic.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        )
                                                        // Record the delta from cell centre to
                                                        // finger contact point for accurate tracking
                                                        val cellCentre = cellPositions[icon.id]
                                                            ?: Offset.Zero
                                                        dragOffset = cellCentre - startOffset
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffset += dragAmount
                                                        // dragOffset now holds the finger's
                                                        // window-absolute position
                                                        val fingerWindow = dragOffset
                                                        var closestIndex = index
                                                        var closestDist  = Float.MAX_VALUE
                                                        uiState.icons.forEachIndexed { i, other ->
                                                            val centre = cellPositions[other.id]
                                                                ?: return@forEachIndexed
                                                            val dist = (centre - fingerWindow).getDistance()
                                                            if (dist < closestDist) {
                                                                closestDist  = dist
                                                                closestIndex = i
                                                            }
                                                        }
                                                        dragTargetIndex = closestIndex
                                                    },
                                                    onDragEnd = {
                                                        val fromIdx = uiState.icons.indexOfFirst {
                                                            it.id == draggingId
                                                        }
                                                        if (fromIdx >= 0 &&
                                                            dragTargetIndex >= 0 &&
                                                            fromIdx != dragTargetIndex) {
                                                            viewModel.reorderIcons(
                                                                fromIdx, dragTargetIndex
                                                            )
                                                        }
                                                        draggingId      = null
                                                        dragTargetIndex = -1
                                                        dragOffset      = Offset.Zero
                                                    },
                                                    onDragCancel = {
                                                        draggingId      = null
                                                        dragTargetIndex = -1
                                                        dragOffset      = Offset.Zero
                                                    }
                                                )
                                            }
                                    )
                                }
                            }
                        }
                    }

                    // Dock
                    Dock(
                        playingIcon    = playingIcon,
                        masterVolume   = uiState.masterVolume,
                        onVolumeChange = viewModel::setMasterVolume,
                        onStop         = viewModel::stopSound,
                        onAddNew       = { onNavigateToEditor(null) }
                    )
                }
            }
        }
    }

    iconToDelete?.let { icon ->
        DeleteConfirmDialog(
            icon      = icon,
            onConfirm = { viewModel.deleteIcon(icon); iconToDelete = null },
            onDismiss = { iconToDelete = null }
        )
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun FilterChipsRow(
    filterMode:       FilterMode,
    categories:       List<String>,
    selectedCategory: String?,
    onFilterMode:     (FilterMode) -> Unit,
    onCategory:       (String?) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f))
    ) {
        item {
            FilterChip(
                selected = filterMode == FilterMode.ALL && selectedCategory == null,
                onClick  = { onFilterMode(FilterMode.ALL); onCategory(null) },
                label    = { Text("All") },
                colors   = frostedChipColors()
            )
        }
        item {
            FilterChip(
                selected = filterMode == FilterMode.FAVORITES,
                onClick  = { onFilterMode(FilterMode.FAVORITES) },
                label    = { Text("⭐ Favorites") },
                colors   = FilterChipDefaults.filterChipColors(
                    containerColor         = Color.White.copy(alpha = 0.08f),
                    labelColor             = Color.White,
                    selectedContainerColor = Color(0xFFFFD700),
                    selectedLabelColor     = Color.Black
                )
            )
        }
        items(categories.size) { i ->
            val cat = categories[i]
            FilterChip(
                selected = selectedCategory == cat,
                onClick  = { onCategory(if (selectedCategory == cat) null else cat) },
                label    = { Text(cat) },
                colors   = frostedChipColors(selectedColor = MaterialTheme.colorScheme.tertiary)
            )
        }
    }
}

@Composable
private fun frostedChipColors(
    selectedColor: Color = MaterialTheme.colorScheme.primary
) = FilterChipDefaults.filterChipColors(
    containerColor         = Color.White.copy(alpha = 0.08f),
    labelColor             = Color.White,
    selectedContainerColor = selectedColor,
    selectedLabelColor     = Color.White
)

@Composable
private fun VolumeRow(volume: Float, onVolumeChange: (Float) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Icon(Icons.Filled.VolumeDown, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Slider(
            value         = volume,
            onValueChange = onVolumeChange,
            modifier      = Modifier.weight(1f).padding(horizontal = 8.dp),
            colors        = SliderDefaults.colors(
                thumbColor         = Color.White,
                activeTrackColor   = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
        Icon(Icons.Filled.VolumeUp, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            "${(volume * 100).toInt()}%",
            color    = Color.White,
            style    = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(36.dp)
        )
    }
}

@Composable
private fun Dock(
    playingIcon:    SoundIcon?,
    masterVolume:   Float,
    onVolumeChange: (Float) -> Unit,
    onStop:         () -> Unit,
    onAddNew:       () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0xBB050D18)))
            )
    ) {
        AnimatedVisibility(
            visible = playingIcon != null,
            enter   = slideInVertically { it } + fadeIn(),
            exit    = slideOutVertically { it } + fadeOut()
        ) {
            playingIcon?.let { icon ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                color = Color(0xFF6650A4),   // fixed purple — color picker removed
                                shape = RoundedCornerShape(9.dp)
                            )
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Now Playing",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            icon.name,
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color.White,
                            maxLines   = 1
                        )
                    }
                    Slider(
                        value         = masterVolume,
                        onValueChange = onVolumeChange,
                        modifier      = Modifier.width(80.dp),
                        colors        = SliderDefaults.colors(
                            thumbColor         = Color.White,
                            activeTrackColor   = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    IconButton(onClick = onStop) {
                        Icon(Icons.Filled.Stop, "Stop", tint = Color.White)
                    }
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 60.dp, vertical = 14.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(Color.White.copy(alpha = 0.11f))
                .padding(vertical = 12.dp)
        ) {
            FloatingActionButton(
                onClick        = onAddNew,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = Color.White,
                modifier       = Modifier.size(56.dp)
            ) {
                Icon(Icons.Rounded.Add, "New Sound Icon", modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun EmptyState(filterMode: FilterMode, searchQuery: String) {
    val inf = rememberInfiniteTransition(label = "empty")
    val alpha by inf.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 0.9f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label         = "alpha"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(40.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = alpha * 0.12f))
                .graphicsLayer { this.alpha = alpha }
        ) {
            Icon(
                imageVector = when {
                    searchQuery.isNotBlank()           -> Icons.Filled.SearchOff
                    filterMode == FilterMode.FAVORITES -> Icons.Filled.Star
                    else                               -> Icons.Filled.Add
                },
                contentDescription = null,
                tint     = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = when {
                searchQuery.isNotBlank()           -> "No results for \"$searchQuery\""
                filterMode == FilterMode.FAVORITES -> "No favourites yet"
                else                               -> "No Sound Icons Yet"
            },
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when {
                searchQuery.isNotBlank()           -> "Try a different name or category"
                filterMode == FilterMode.FAVORITES -> "Star a sound to add it to favourites"
                else                               -> "Tap + to create sounds, then add them as home screen widgets"
            },
            style     = MaterialTheme.typography.bodyMedium,
            color     = Color.White.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )
    }
}
