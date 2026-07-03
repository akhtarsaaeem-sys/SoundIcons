package com.soundicons.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log

private const val TAG = "UriUtil"

/**
 * Helper functions for working with content URIs safely.
 */
object UriUtil {

    /**
     * Persist read permission for a content URI so it survives app restarts.
     * This is required when the URI comes from ACTION_OPEN_DOCUMENT.
     */
    fun persistReadPermission(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not persist URI permission: ${e.message}")
        }
    }

    /**
     * Returns the display file name for a content URI, or null on failure.
     */
    fun getFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (nameIndex >= 0) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "getFileName failed: ${e.message}")
            null
        }
    }

    /**
     * Validates that the URI is still accessible (file not deleted, permission still valid).
     */
    fun isUriAccessible(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(Uri.parse(uri.toString()))?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks whether a string is a valid non-empty URI.
     */
    fun isValidUri(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        return try {
            Uri.parse(uriString) != null
        } catch (e: Exception) {
            false
        }
    }
}
