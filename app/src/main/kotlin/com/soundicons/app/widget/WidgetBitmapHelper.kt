package com.soundicons.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.util.Log
import com.soundicons.app.data.model.SoundIcon
import java.io.File
import java.io.InputStream

private const val TAG = "WidgetBitmapHelper"

object WidgetBitmapHelper {

    fun load(context: Context, icon: SoundIcon, widthPx: Int, heightPx: Int): Bitmap {
        val w = widthPx.coerceAtLeast(32)
        val h = heightPx.coerceAtLeast(32)

        val imageUri = icon.croppedImageUri ?: icon.imageUri
        if (!imageUri.isNullOrBlank()) {
            try {
                val file = File(imageUri)
                val stream: InputStream? =
                    if (file.exists()) file.inputStream()
                    else context.contentResolver.openInputStream(Uri.parse(imageUri))

                stream?.use { s ->
                    val raw = BitmapFactory.decodeStream(s) ?: return@use

                    val scaled = if (w == h) {
                        val minEdge = minOf(raw.width, raw.height)
                        val srcX    = (raw.width  - minEdge) / 2
                        val srcY    = (raw.height - minEdge) / 2
                        val cropped = Bitmap.createBitmap(raw, srcX, srcY, minEdge, minEdge)
                        Bitmap.createScaledBitmap(cropped, w, h, true)
                    } else {
                        Bitmap.createScaledBitmap(raw, w, h, true)
                    }

                    return roundedClip(scaled, w, h)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not load image $imageUri: ${e.message}")
            }
        }
        return buildFallback(icon.name, w, h)
    }

    fun load(context: Context, icon: SoundIcon, sizePx: Int): Bitmap =
        load(context, icon, sizePx, sizePx)

    fun dpToPx(context: Context, dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt().coerceAtLeast(32)

    private fun roundedClip(src: Bitmap, widthPx: Int, heightPx: Int): Bitmap {
        val out    = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val radius = minOf(widthPx, heightPx) * 0.22f
        val rect   = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
        val path   = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
        canvas.clipPath(path)
        canvas.drawBitmap(src, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        return out
    }

    private fun buildFallback(name: String?, widthPx: Int, heightPx: Int): Bitmap {
        val letter    = name?.firstOrNull()?.uppercaseChar()?.toString() ?: "♪"
        val out       = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas    = Canvas(out)
        val shortSide = minOf(widthPx, heightPx).toFloat()
        val radius    = shortSide * 0.22f
        val rect      = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())

        val shader = LinearGradient(
            0f, 0f, widthPx.toFloat(), heightPx.toFloat(),
            0xFF6650A4.toInt(), 0xFF8B5CF6.toInt(),
            Shader.TileMode.CLAMP
        )
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
        canvas.drawRoundRect(rect, radius, radius, bgPaint)

        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color     = 0xFFFFFFFF.toInt()
            textSize  = shortSide * 0.44f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            letter,
            widthPx  / 2f,
            heightPx / 2f - (tp.descent() + tp.ascent()) / 2f,
            tp
        )
        return out
    }
}