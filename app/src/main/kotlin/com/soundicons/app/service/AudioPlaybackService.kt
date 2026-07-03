package com.soundicons.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.soundicons.app.MainActivity
import com.soundicons.app.R
import com.soundicons.app.util.VibrationUtil

private const val TAG            = "AudioPlaybackService"

const val AUDIO_SERVICE_ACTION_PLAY   = "com.soundicons.app.ACTION_PLAY"
const val AUDIO_SERVICE_ACTION_STOP   = "com.soundicons.app.ACTION_STOP"

const val AUDIO_EXTRA_WIDGET_ID       = "extra_widget_id"
const val AUDIO_EXTRA_URI             = "extra_audio_uri"
const val AUDIO_EXTRA_NAME            = "extra_icon_name"
const val AUDIO_EXTRA_VOLUME          = "extra_volume"
const val AUDIO_EXTRA_LOOP            = "extra_loop"
const val AUDIO_EXTRA_TRIM_START_MS   = "extra_trim_start_ms"  // Feature 2
const val AUDIO_EXTRA_TRIM_END_MS     = "extra_trim_end_ms"    // Feature 2
const val AUDIO_EXTRA_VIBRATE         = "extra_vibrate"        // Feature 11
const val AUDIO_EXTRA_VIBRATE_MS      = "extra_vibrate_ms"     // Feature 11

private const val NOTIFICATION_ID = 8001
private const val CHANNEL_ID      = "soundicons_playback"

class AudioPlaybackService : Service() {

    private var player: MediaPlayer? = null
    private var activeWidgetId: Int  = -1

    override fun onCreate() { super.onCreate(); createNotificationChannel() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            AUDIO_SERVICE_ACTION_PLAY -> handlePlay(intent)
            AUDIO_SERVICE_ACTION_STOP -> handleStop()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() { releasePlayer(); super.onDestroy() }

    private fun handlePlay(intent: Intent) {
        val audioUriStr  = intent.getStringExtra(AUDIO_EXTRA_URI)
        val widgetId     = intent.getIntExtra(AUDIO_EXTRA_WIDGET_ID, -1)
        val name         = intent.getStringExtra(AUDIO_EXTRA_NAME) ?: "Sound"
        val volume       = intent.getFloatExtra(AUDIO_EXTRA_VOLUME, 1f).coerceIn(0f, 1f)
        val loop         = intent.getBooleanExtra(AUDIO_EXTRA_LOOP, false)
        val trimStart    = intent.getLongExtra(AUDIO_EXTRA_TRIM_START_MS, 0L)   // Feature 2
        val trimEnd      = intent.getLongExtra(AUDIO_EXTRA_TRIM_END_MS, 0L)     // Feature 2
        val vibrate      = intent.getBooleanExtra(AUDIO_EXTRA_VIBRATE, false)   // Feature 11
        val vibrateMs    = intent.getLongExtra(AUDIO_EXTRA_VIBRATE_MS, 100L)    // Feature 11

        if (audioUriStr.isNullOrBlank()) { stopSelf(); return }

        // Toggle same widget = stop
        if (widgetId == activeWidgetId && player?.isPlaying == true) {
            handleStop(); return
        }

        releasePlayer()
        activeWidgetId = widgetId

        // Feature 11: Vibration
        if (vibrate) VibrationUtil.vibrate(applicationContext, vibrateMs)

        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(applicationContext, Uri.parse(audioUriStr))
                isLooping = loop
                setVolume(volume, volume)
                setOnPreparedListener { mp ->
                    // Feature 2: Seek to trim start after prepare
                    if (trimStart > 0) mp.seekTo(trimStart.toInt())
                    mp.start()
                }
                setOnCompletionListener { if (!loop) handleStop() }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    handleStop(); true
                }
                prepareAsync()
            }

            // Feature 2: Schedule stop at trim end
            if (trimEnd > 0 && trimEnd > trimStart) {
                val delay = trimEnd - trimStart
                android.os.Handler(mainLooper).postDelayed({ handleStop() }, delay)
            }

            startForeground(NOTIFICATION_ID, buildNotification(name))
        } catch (e: SecurityException) {
            Log.e(TAG, "URI permission denied", e); handleStop()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaPlayer", e); handleStop()
        }
    }

    private fun handleStop() {
        releasePlayer()
        activeWidgetId = -1
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releasePlayer() {
        try { player?.apply { if (isPlaying) stop(); release() } } catch (e: Exception) { }
        player = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Sound Widget Playback", NotificationManager.IMPORTANCE_LOW)
            .apply { description = "Shown while a widget is playing a sound"; setShowBadge(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(soundName: String): Notification {
        val openApp = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 0, buildStopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget_note)
            .setContentTitle(soundName)
            .setContentText("Tap to open  ·  Stop to silence")
            .setContentIntent(openApp)
            .addAction(R.drawable.ic_widget_note, "Stop", stop)
            .setOngoing(true).setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW).build()
    }

    companion object {
        fun buildPlayIntent(context: Context, widgetId: Int, audioUri: String, name: String,
                            volume: Float, loop: Boolean, trimStartMs: Long = 0L,
                            trimEndMs: Long = 0L, vibrate: Boolean = false,
                            vibrateMs: Long = 100L): Intent =
            Intent(context, AudioPlaybackService::class.java).apply {
                action = AUDIO_SERVICE_ACTION_PLAY
                putExtra(AUDIO_EXTRA_WIDGET_ID,     widgetId)
                putExtra(AUDIO_EXTRA_URI,           audioUri)
                putExtra(AUDIO_EXTRA_NAME,          name)
                putExtra(AUDIO_EXTRA_VOLUME,        volume)
                putExtra(AUDIO_EXTRA_LOOP,          loop)
                putExtra(AUDIO_EXTRA_TRIM_START_MS, trimStartMs)
                putExtra(AUDIO_EXTRA_TRIM_END_MS,   trimEndMs)
                putExtra(AUDIO_EXTRA_VIBRATE,       vibrate)
                putExtra(AUDIO_EXTRA_VIBRATE_MS,    vibrateMs)
            }

        fun buildStopIntent(context: Context): Intent =
            Intent(context, AudioPlaybackService::class.java).apply { action = AUDIO_SERVICE_ACTION_STOP }
    }
}
