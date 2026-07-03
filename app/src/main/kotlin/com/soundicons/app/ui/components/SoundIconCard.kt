package com.soundicons.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.soundicons.app.data.model.SoundIcon
import java.io.File

/**
 * Launcher-style icon card.
 *
 * ── Gesture ownership ────────────────────────────────────────────────────────
 *
 *  • Single tap  → play sound  (via clickable — does NOT hold the gesture stream)
 *  • Long-press  → drag-and-drop  (detectDragGesturesAfterLongPress in HomeScreen
 *                  wins uncontested because clickable releases the stream)
 *  • ⋮ button   → Favourite / Edit / Delete  (independent tap target)
 *
 * CRITICAL: We use .clickable{} NOT pointerInput/detectTapGestures.
 * detectTapGestures holds the gesture until it can classify the touch, which
 * blocks detectDragGesturesAfterLongPress from ever receiving the long-press.
 * clickable() uses a different internal mechanism that does not hold the stream,
 * so the outer drag recogniser can claim the long-press freely.
 *
 * ── Image rendering ──────────────────────────────────────────────────────────
 *
 *  • Image present → render image only, no colour background, rounded clip only
 *  • No image      → fixed purple gradient placeholder + music note icon
 */
@Composable
fun SoundIconCard(
    icon:       SoundIcon,
    isPlaying:  Boolean,
    hasWidget:  Boolean,
    isDragging: Boolean  = false,
    onTap:      () -> Unit,
    onEdit:     () -> Unit,
    onDelete:   () -> Unit,
    onFavorite: () -> Unit,
    modifier:   Modifier = Modifier
) {
    val context  = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue   = when {
            isDragging -> 1.10f
            isPlaying  -> 0.95f
            else       -> 1f
        },
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "scale"
    )

    val inf = rememberInfiniteTransition(label = "glow")
    val glowAlpha by inf.animateFloat(
        initialValue  = 0f,
        targetValue   = if (isPlaying) 0.55f else 0f,
        animationSpec = infiniteRepeatable(
            tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ),
        label = "glow"
    )

    val displayUri = icon.croppedImageUri ?: icon.imageUri
    val hasImage   = !displayUri.isNullOrBlank()

    // Fixed placeholder colours — used only when no image is set
    val placeholderBrush = Brush.linearGradient(
        listOf(Color(0xFF6650A4), Color(0xFF8B5CF6))
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(76.dp).scale(scale)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {

            // ── Icon body ─────────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .shadow(
                        elevation    = if (isDragging) 24.dp else if (isPlaying) 20.dp else 6.dp,
                        shape        = RoundedCornerShape(18.dp),
                        ambientColor = Color.Black.copy(alpha = 0.35f),
                        spotColor    = Color.Black.copy(alpha = 0.45f)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    // Placeholder gradient only when no image
                    .then(
                        if (!hasImage) Modifier.background(placeholderBrush)
                        else Modifier
                    )
                    // clickable — does NOT hold the gesture stream, so long-press
                    // is immediately available to detectDragGesturesAfterLongPress
                    .clickable(onClick = onTap)
            ) {
                if (hasImage) {
                    // Image only — no colour background, rounded clip from parent
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(
                                if (File(displayUri!!).exists()) displayUri
                                else displayUri
                            )
                            .crossfade(true)
                            .build(),
                        contentDescription = icon.name,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize()
                    )
                } else {
                    // Placeholder: music note on purple gradient
                    Icon(
                        Icons.Rounded.GraphicEq, null,
                        tint     = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Playing pulse overlay
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                color = Color.White.copy(alpha = glowAlpha * 0.22f),
                                shape = RoundedCornerShape(18.dp)
                            )
                    )
                }

                // Drag highlight ring
                if (isDragging) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(
                                width = 2.dp,
                                color = Color.White.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(18.dp)
                            )
                    )
                }
            }

            // ── ⋮ options button ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .offset(x = 4.dp, y = (-4).dp)
            ) {
                IconButton(
                    onClick  = { showMenu = true },
                    modifier = Modifier
                        .size(22.dp)
                        .background(Color.Black.copy(alpha = 0.50f), CircleShape)
                ) {
                    Icon(
                        Icons.Filled.MoreVert, "Options",
                        tint     = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }

                DropdownMenu(
                    expanded         = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (icon.isFavorite) "Remove from Favourites"
                                else "Add to Favourites"
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (icon.isFavorite) Icons.Filled.StarBorder
                                else Icons.Filled.Star,
                                null,
                                tint = Color(0xFFFFD700)
                            )
                        },
                        onClick = { showMenu = false; onFavorite() }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text        = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Filled.Edit, null) },
                        onClick     = { showMenu = false; onEdit() }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Filled.Delete, null,
                                tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }

            // ── Status badges ─────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
            ) {
                if (hasWidget) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(14.dp)
                            .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                            .border(1.dp, Color(0xFF0D1B2A), CircleShape)
                    ) {
                        Icon(Icons.Filled.Widgets, "Widget",
                            tint = Color.White, modifier = Modifier.size(8.dp))
                    }
                    Spacer(Modifier.height(2.dp))
                }
                if (icon.isFavorite) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(14.dp)
                            .background(Color(0xFFFFD700), CircleShape)
                            .border(1.dp, Color(0xFF0D1B2A), CircleShape)
                    ) {
                        Icon(Icons.Filled.Star, "Favourite",
                            tint = Color.Black, modifier = Modifier.size(8.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(7.dp))

        Text(
            text  = icon.name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize   = 11.sp,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                lineHeight = 13.sp
            ),
            color     = Color.White,
            maxLines  = 2,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier  = Modifier.widthIn(max = 76.dp)
        )
    }
}
