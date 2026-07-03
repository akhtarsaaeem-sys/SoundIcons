package com.soundicons.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class AppSettings @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        val AUTO_STOP_PREVIOUS  = booleanPreferencesKey("auto_stop_previous")
        val DARK_THEME          = stringPreferencesKey("dark_theme")   // "SYSTEM","DARK","LIGHT"
        val DEFAULT_LOOP        = booleanPreferencesKey("default_loop")
        val DEFAULT_VOLUME      = floatPreferencesKey("default_volume")
        val HAPTIC_FEEDBACK     = booleanPreferencesKey("haptic_feedback")
    }

    val autoStopPrevious: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[AUTO_STOP_PREVIOUS] ?: true }

    val darkTheme: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[DARK_THEME] ?: "SYSTEM" }

    val defaultLoop: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[DEFAULT_LOOP] ?: false }

    val defaultVolume: Flow<Float> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[DEFAULT_VOLUME] ?: 1f }

    val hapticFeedback: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[HAPTIC_FEEDBACK] ?: true }

    suspend fun setAutoStopPrevious(value: Boolean) {
        context.dataStore.edit { it[AUTO_STOP_PREVIOUS] = value }
    }
    suspend fun setDarkTheme(value: String) {
        context.dataStore.edit { it[DARK_THEME] = value }
    }
    suspend fun setDefaultLoop(value: Boolean) {
        context.dataStore.edit { it[DEFAULT_LOOP] = value }
    }
    suspend fun setDefaultVolume(value: Float) {
        context.dataStore.edit { it[DEFAULT_VOLUME] = value }
    }
    suspend fun setHapticFeedback(value: Boolean) {
        context.dataStore.edit { it[HAPTIC_FEEDBACK] = value }
    }
}
