package com.soundicons.app.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.soundicons.app.data.model.SoundIcon
import java.io.BufferedReader
import java.io.InputStreamReader

private const val TAG = "BackupUtil"

/**
 * Handles JSON-based backup and restore of [SoundIcon] data.
 * Only metadata is exported; audio/image files are referenced by URI.
 */
object BackupUtil {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Serialize [icons] to JSON and write to the given [uri] (e.g. user-chosen file).
     * Returns true on success.
     */
    fun exportToUri(context: Context, uri: Uri, icons: List<SoundIcon>): Boolean {
        return try {
            val json = gson.toJson(icons)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Export failed: ${e.message}")
            false
        }
    }

    /**
     * Read JSON from [uri] and deserialize back to a list of [SoundIcon].
     * Returns null if reading or parsing fails.
     */
    fun importFromUri(context: Context, uri: Uri): List<SoundIcon>? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val json = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
            val type = object : TypeToken<List<SoundIcon>>() {}.type
            gson.fromJson<List<SoundIcon>>(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Import failed: ${e.message}")
            null
        }
    }
}
