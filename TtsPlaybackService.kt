package com.metroreader.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.metroreader.app.MainActivity
import com.metroreader.app.R
import com.metroreader.app.tts.MetroTtsEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TtsPlaybackService : MediaSessionService() {

    @Inject
    lateinit var ttsEngine: MetroTtsEngine

    private var mediaSession: MediaSession? = null

    companion object {
        const val CHANNEL_ID = "metro_reader_tts"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY = "com.metroreader.app.ACTION_PLAY"
        const val ACTION_PAUSE = "com.metroreader.app.ACTION_PAUSE"
        const val ACTION_STOP = "com.metroreader.app.ACTION_STOP"
        const val ACTION_NEXT = "com.metroreader.app.ACTION_NEXT"
        const val ACTION_PREV = "com.metroreader.app.ACTION_PREV"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ttsEngine.initialize(this)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY  -> ttsEngine.play()
            ACTION_PAUSE -> ttsEngine.pause()
            ACTION_STOP  -> { ttsEngine.stop(); stopSelf() }
            ACTION_NEXT  -> { /* handled by AudioViewModel */ }
            ACTION_PREV  -> { /* handled by AudioViewModel */ }
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        ttsEngine.shutdown()
        mediaSession?.release()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = PendingIntent.getService(
            this, 1,
            Intent(this, TtsPlaybackService::class.java).apply { action = ACTION_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, TtsPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_splash_logo)
            .setContentTitle("Metro Reader")
            .setContentText("Playing audiobook")
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1))
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Metro Reader Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Text-to-Speech audiobook playback"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}
