package com.soundicons.app.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AudioPlayerManager"

/** In-app audio preview using ExoPlayer. Supports trim start/end. */
@Singleton
class AudioPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _playingIconId = MutableStateFlow<Long?>(null)
    val playingIconId: StateFlow<Long?> = _playingIconId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

    private var player: ExoPlayer? = null

    private fun getOrCreatePlayer(): ExoPlayer {
        if (player == null || player?.isReleased == true) {
            player = ExoPlayer.Builder(context).build().also { exo ->
                exo.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (!isPlaying && exo.playbackState == Player.STATE_ENDED) {
                            _playingIconId.value = null
                        }
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e(TAG, "Playback error: ${error.message}")
                        _hasError.value = true
                        _playingIconId.value = null
                        _isPlaying.value = false
                    }
                })
            }
        }
        return player!!
    }

    fun play(iconId: Long, audioUriString: String, volume: Float = 1f, loop: Boolean = false,
             trimStartMs: Long = 0L, trimEndMs: Long = 0L) {
        _hasError.value = false
        if (_playingIconId.value == iconId && _isPlaying.value) { stop(); return }
        try {
            val uri  = Uri.parse(audioUriString)
            val exo  = getOrCreatePlayer()
            val item = if (trimStartMs > 0 || trimEndMs > 0) {
                MediaItem.Builder().setUri(uri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(trimStartMs)
                            .apply { if (trimEndMs > 0) setEndPositionMs(trimEndMs) }
                            .build()
                    ).build()
            } else {
                MediaItem.fromUri(uri)
            }
            exo.stop(); exo.clearMediaItems()
            exo.setMediaItem(item)
            exo.volume     = volume.coerceIn(0f, 1f)
            exo.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            exo.prepare(); exo.play()
            _playingIconId.value = iconId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play: ${e.message}")
            _hasError.value = true
        }
    }

    fun stop()               { player?.stop(); _playingIconId.value = null; _isPlaying.value = false }
    fun pause()              { player?.pause() }
    fun resume()             { player?.play() }
    fun setVolume(v: Float)  { player?.volume = v.coerceIn(0f, 1f) }
    fun release()            { player?.release(); player = null; _playingIconId.value = null; _isPlaying.value = false }
}
