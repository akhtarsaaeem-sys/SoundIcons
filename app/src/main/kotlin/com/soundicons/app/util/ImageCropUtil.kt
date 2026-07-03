package com.soundicons.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

private const val TAG = "ImageCropUtil"

/** Crop shape options for Feature 1 */
enum class CropShape { CIRCLE, ROUNDED_SQUARE, SQUARE }

object ImageCropUtil {

    /**
     * Loads [uri], centres-crops it to a square, applies [shape] clipping,
     * saves the result to the app's internal files dir, and returns the path.
     */
    fun cropAndSave(
        context: Context,
        uri: Uri,
        shape: CropShape = CropShape.ROUNDED_SQUARE,
        outputSizePx: Int = 256
    ): String? {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            val raw    = BitmapFactory.decodeStream(stream)
            stream.close()
            raw ?: return null

            val cropped = applyCrop(raw, shape, outputSizePx)

            val dir  = File(context.filesDir, "cropped_icons")
            dir.mkdirs()
            val file = File(dir, "icon_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { fos ->
                cropped.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "cropAndSave failed", e)
            null
        }
    }

    /**
     * Apply crop shape to a Bitmap (in-memory, no file I/O).
     * Used for live preview in the editor.
     */
    fun applyCrop(src: Bitmap, shape: CropShape, sizePx: Int): Bitmap {
        // First, center-crop to square
        val minEdge  = minOf(src.width, src.height)
        val x        = (src.width  - minEdge) / 2
        val y        = (src.height - minEdge) / 2
        val squared  = Bitmap.createBitmap(src, x, y, minEdge, minEdge)
        val scaled   = Bitmap.createScaledBitmap(squared, sizePx, sizePx, true)

        val out    = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw mask
        val rect   = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
        when (shape) {
            CropShape.CIRCLE -> canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)
            CropShape.ROUNDED_SQUARE -> {
                val r = sizePx * 0.22f
                canvas.drawRoundRect(rect, r, r, paint)
            }
            CropShape.SQUARE -> canvas.drawRect(rect, paint)
        }

        // Draw image clipped to mask
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, null, Rect(0, 0, sizePx, sizePx), paint)
        return out
    }
}
