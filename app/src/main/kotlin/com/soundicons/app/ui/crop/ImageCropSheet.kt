package com.soundicons.app.ui.crop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundicons.app.util.CropShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

private const val TAG = "ImageCropSheet"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropSheet(
    imageUri:      String,
    selectedShape: CropShape,
    onShapeChange: (CropShape) -> Unit,
    onConfirm:     (String) -> Unit,
    onDismiss:     () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scale        by remember { mutableFloatStateOf(1f) }
    var offset       by remember { mutableStateOf(Offset.Zero) }
    var viewportPx   by remember { mutableFloatStateOf(0f) }
    val cropWindowPx = viewportPx * 0.8f

    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(imageUri) {
        val bmp = withContext(Dispatchers.IO) {
            try {
                val uri    = Uri.parse(imageUri)
                val file   = File(imageUri)
                val stream = if (file.exists()) file.inputStream()
                else context.contentResolver.openInputStream(uri)
                stream?.use { BitmapFactory.decodeStream(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load bitmap", e)
                null
            }
        }
        sourceBitmap = bmp
        bmp?.let {
            val vp = viewportPx.takeIf { v -> v > 0f } ?: 900f
            val cw = vp * 0.8f
            scale  = max(cw / it.width, cw / it.height)
            val imgW = it.width  * scale
            val imgH = it.height * scale
            offset = Offset((vp - imgW) / 2f, (vp - imgH) / 2f)
        }
    }

    fun resetTransform(bmp: Bitmap, vp: Float) {
        val cw   = vp * 0.8f
        scale    = max(cw / bmp.width, cw / bmp.height)
        val imgW = bmp.width  * scale
        val imgH = bmp.height * scale
        offset   = Offset((vp - imgW) / 2f, (vp - imgH) / 2f)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Crop Icon", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Pinch to zoom  ·  Drag to reposition",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            // ── Interactive crop viewport ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF111111))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val bmp = sourceBitmap ?: return@detectTransformGestures
                                val vp  = size.width.toFloat()
                                val cw  = vp * 0.8f

                                val minScale = max(cw / bmp.width, cw / bmp.height)
                                val newScale = (scale * zoom).coerceIn(minScale, minScale * 8f)

                                val scaleChange = newScale / scale
                                val newOffX = (offset.x - vp / 2f) * scaleChange + vp / 2f + pan.x
                                val newOffY = (offset.y - vp / 2f) * scaleChange + vp / 2f + pan.y

                                scale = newScale

                                val imgW     = bmp.width  * scale
                                val imgH     = bmp.height * scale
                                val cropLeft = (vp - cw) / 2f
                                val cropTop  = (vp - cw) / 2f

                                offset = Offset(
                                    newOffX.coerceIn(cropLeft + cw - imgW, cropLeft),
                                    newOffY.coerceIn(cropTop  + cw - imgH, cropTop)
                                )
                            }
                        }
                ) {
                    val vp = size.width
                    if (viewportPx != vp) {
                        viewportPx = vp
                        sourceBitmap?.let { resetTransform(it, vp) }
                    }
                    val cw       = vp * 0.8f
                    val cropLeft = (vp - cw) / 2f
                    val cropTop  = (vp - cw) / 2f

                    drawIntoCanvas { canvas ->

                        // ── 1. Draw the image ─────────────────────────────────
                        sourceBitmap?.let { bmp ->
                            val matrix = Matrix().apply {
                                postScale(scale, scale)
                                postTranslate(offset.x, offset.y)
                            }
                            canvas.nativeCanvas.drawBitmap(bmp, matrix, null)
                        }

                        // ── 2. Draw shape-aware overlay ───────────────────────
                        //
                        // Strategy:
                        //   a) Fill the entire viewport with the dark overlay colour.
                        //   b) "Punch out" the crop shape using PorterDuff.CLEAR so
                        //      the image shows through in exactly the right shape.
                        //   c) Draw the white border matching the crop shape.
                        //   d) Draw corner handles (square/rounded only) or none (circle).
                        //
                        // Using CLEAR xfer-mode means we never have to manually
                        // compute the complement of a circle — we just erase the
                        // shape from the overlay layer.

                        // Save layer so CLEAR only affects this overlay layer,
                        // not the image drawn beneath it.
                        val overlayBounds = android.graphics.RectF(
                            0f, 0f, vp, vp
                        )
                        canvas.nativeCanvas.saveLayer(overlayBounds, null)

                        // a) Full dark fill
                        val overPaint = android.graphics.Paint().apply {
                            color = 0xAA000000.toInt()
                            style = android.graphics.Paint.Style.FILL
                        }
                        canvas.nativeCanvas.drawRect(0f, 0f, vp, vp, overPaint)

                        // b) Punch out the crop shape with CLEAR
                        val clearPaint = android.graphics.Paint().apply {
                            xfermode = android.graphics.PorterDuffXfermode(
                                android.graphics.PorterDuff.Mode.CLEAR
                            )
                            style = android.graphics.Paint.Style.FILL
                            isAntiAlias = true
                        }
                        val cropRect = android.graphics.RectF(
                            cropLeft, cropTop, cropLeft + cw, cropTop + cw
                        )
                        when (selectedShape) {
                            CropShape.CIRCLE ->
                                canvas.nativeCanvas.drawOval(cropRect, clearPaint)

                            CropShape.ROUNDED_SQUARE -> {
                                val r = cw * 0.18f
                                canvas.nativeCanvas.drawRoundRect(cropRect, r, r, clearPaint)
                            }

                            CropShape.SQUARE ->
                                canvas.nativeCanvas.drawRect(cropRect, clearPaint)
                        }

                        // Restore — overlay with punched-out shape is now composited
                        canvas.nativeCanvas.restore()

                        // c) White border matching the crop shape
                        val borderPaint = android.graphics.Paint().apply {
                            color       = 0xFFFFFFFF.toInt()
                            style       = android.graphics.Paint.Style.STROKE
                            strokeWidth = 2.dp.toPx()
                            isAntiAlias = true
                        }
                        when (selectedShape) {
                            CropShape.CIRCLE ->
                                canvas.nativeCanvas.drawOval(cropRect, borderPaint)

                            CropShape.ROUNDED_SQUARE -> {
                                val r = cw * 0.18f
                                canvas.nativeCanvas.drawRoundRect(cropRect, r, r, borderPaint)
                            }

                            CropShape.SQUARE ->
                                canvas.nativeCanvas.drawRect(cropRect, borderPaint)
                        }

                        // d) Corner handles — shown for Square and Rounded Square,
                        //    skipped for Circle (no corners to indicate).
                        if (selectedShape != CropShape.CIRCLE) {
                            val handleLen   = 16.dp.toPx()
                            val handlePaint = android.graphics.Paint().apply {
                                color       = 0xFFFFFFFF.toInt()
                                style       = android.graphics.Paint.Style.STROKE
                                strokeWidth = 3.dp.toPx()
                                strokeCap   = android.graphics.Paint.Cap.ROUND
                                isAntiAlias = true
                            }
                            // Top-left
                            canvas.nativeCanvas.drawLine(cropLeft, cropTop, cropLeft + handleLen, cropTop, handlePaint)
                            canvas.nativeCanvas.drawLine(cropLeft, cropTop, cropLeft, cropTop + handleLen, handlePaint)
                            // Top-right
                            canvas.nativeCanvas.drawLine(cropLeft + cw, cropTop, cropLeft + cw - handleLen, cropTop, handlePaint)
                            canvas.nativeCanvas.drawLine(cropLeft + cw, cropTop, cropLeft + cw, cropTop + handleLen, handlePaint)
                            // Bottom-left
                            canvas.nativeCanvas.drawLine(cropLeft, cropTop + cw, cropLeft + handleLen, cropTop + cw, handlePaint)
                            canvas.nativeCanvas.drawLine(cropLeft, cropTop + cw, cropLeft, cropTop + cw - handleLen, handlePaint)
                            // Bottom-right
                            canvas.nativeCanvas.drawLine(cropLeft + cw, cropTop + cw, cropLeft + cw - handleLen, cropTop + cw, handlePaint)
                            canvas.nativeCanvas.drawLine(cropLeft + cw, cropTop + cw, cropLeft + cw, cropTop + cw - handleLen, handlePaint)
                        }
                    }
                }
            }

            // ── Shape picker ──────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.align(Alignment.CenterHorizontally)
            ) {
                ShapeChip("Circle",  CropShape.CIRCLE,         selectedShape, CircleShape,               onShapeChange)
                ShapeChip("Rounded", CropShape.ROUNDED_SQUARE, selectedShape, RoundedCornerShape(20.dp), onShapeChange)
                ShapeChip("Square",  CropShape.SQUARE,         selectedShape, RoundedCornerShape(4.dp),  onShapeChange)
            }

            // ── Reset button ──────────────────────────────────────────────────
            OutlinedButton(
                onClick  = { sourceBitmap?.let { resetTransform(it, viewportPx) } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Reset Position")
            }

            errorMsg?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
            }

            // ── Confirm ───────────────────────────────────────────────────────
            Button(
                onClick  = {
                    sourceBitmap ?: return@Button
                    isSaving = true
                    errorMsg = null
                },
                enabled  = sourceBitmap != null && !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Apply Crop", fontWeight = FontWeight.SemiBold)
                }
            }

            // Trigger save when isSaving flips true
            LaunchedEffect(isSaving) {
                if (!isSaving) return@LaunchedEffect
                val bmp = sourceBitmap
                if (bmp == null) { isSaving = false; return@LaunchedEffect }
                val path = withContext(Dispatchers.IO) {
                    renderAndSave(context, bmp, offset, scale, viewportPx, viewportPx * 0.8f, selectedShape)
                }
                isSaving = false
                if (path != null) onConfirm(path)
                else errorMsg = "Crop failed — please try again"
            }
        }
    }
}

// ── Shape picker chip ─────────────────────────────────────────────────────────

@Composable
private fun ShapeChip(
    label:    String,
    shape:    CropShape,
    selected: CropShape,
    clip:     androidx.compose.ui.graphics.Shape,
    onSelect: (CropShape) -> Unit
) {
    val isSelected = shape == selected
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.clickable { onSelect(shape) }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(clip)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .then(
                    if (!isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, clip)
                    else Modifier
                )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style    = MaterialTheme.typography.labelSmall,
            color    = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
    }
}

// ── Crop render + save (IO thread) ────────────────────────────────────────────
// NOT modified — saving logic was already correct.

private fun renderAndSave(
    context:      Context,
    src:          Bitmap,
    offset:       Offset,
    scale:        Float,
    viewportPx:   Float,
    cropWindowPx: Float,
    shape:        CropShape,
    outputSizePx: Int = 512
): String? {
    return try {
        val cropLeft  = (viewportPx - cropWindowPx) / 2f
        val cropTop   = (viewportPx - cropWindowPx) / 2f

        val srcLeft   = ((cropLeft - offset.x) / scale).coerceAtLeast(0f)
        val srcTop    = ((cropTop  - offset.y) / scale).coerceAtLeast(0f)
        val srcRight  = ((cropLeft + cropWindowPx - offset.x) / scale).coerceAtMost(src.width.toFloat())
        val srcBottom = ((cropTop  + cropWindowPx - offset.y) / scale).coerceAtMost(src.height.toFloat())

        val srcW = (srcRight  - srcLeft).toInt().coerceAtLeast(1)
        val srcH = (srcBottom - srcTop ).toInt().coerceAtLeast(1)

        val cropped = Bitmap.createBitmap(src, srcLeft.toInt(), srcTop.toInt(), srcW, srcH)
        val scaled  = Bitmap.createScaledBitmap(cropped, outputSizePx, outputSizePx, true)

        val out    = Bitmap.createBitmap(outputSizePx, outputSizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect   = RectF(0f, 0f, outputSizePx.toFloat(), outputSizePx.toFloat())

        when (shape) {
            CropShape.CIRCLE ->
                canvas.drawCircle(outputSizePx / 2f, outputSizePx / 2f, outputSizePx / 2f, paint)
            CropShape.ROUNDED_SQUARE -> {
                val r = outputSizePx * 0.22f
                canvas.drawRoundRect(rect, r, r, paint)
            }
            CropShape.SQUARE ->
                canvas.drawRect(rect, paint)
        }
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, null, android.graphics.Rect(0, 0, outputSizePx, outputSizePx), paint)

        val dir  = File(context.filesDir, "cropped_icons")
        dir.mkdirs()
        val file = File(dir, "crop_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { fos -> out.compress(Bitmap.CompressFormat.PNG, 100, fos) }
        file.absolutePath
    } catch (e: Exception) {
        Log.e(TAG, "renderAndSave failed", e)
        null
    }
}