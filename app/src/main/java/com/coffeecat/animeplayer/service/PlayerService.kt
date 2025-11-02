package com.coffeecat.animeplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import com.coffeecat.animeplayer.MainActivity
import com.coffeecat.animeplayer.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerService : MediaSessionService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var autoSaveJob: Job? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        autoSaveJob?.cancel()
        autoSaveJob = serviceScope.launch {
            while (isActive) {
                delay(200)
                val player = PlayerHolder.exoPlayer ?: continue
                val posDur = withContext(Dispatchers.Main) {
                    if (player.duration != C.TIME_UNSET && player.currentMediaItem != null) {
                        player.currentPosition to player.duration
                    } else null
                } ?: continue

                if (PlayerHolder.draggingSeekPosMs == null) {
                    PlayerHolder.currentPosition = posDur.first
                }
                PlayerHolder.duration = posDur.second

                PlayerHolder.saveProgress(applicationContext)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PlayerHolder.saveProgress(applicationContext)
        mediaSession?.release()
        mediaSession = null
        serviceScope.cancel()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    @OptIn(UnstableApi::class)
    private fun buildNotification(): Notification {
        // 檢查 mediaSession 是否存在
        val session = mediaSession
            ?: throw IllegalStateException("MediaSession 未初始化")

        // 點擊通知打開 MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            REQUEST_CODE_MAIN,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // 創建各個操作的 PendingIntent
        val stopIntent = Intent(this, PlayerActionReceiver::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            REQUEST_CODE_STOP,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val previousIntent = Intent(this, PlayerActionReceiver::class.java).apply {
            action = ACTION_PREVIOUS
        }
        val previousPendingIntent = PendingIntent.getBroadcast(
            this,
            REQUEST_CODE_PREVIOUS,
            previousIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(this, PlayerActionReceiver::class.java).apply {
            action = ACTION_PLAY_PAUSE
        }
        val playPausePendingIntent = PendingIntent.getBroadcast(
            this,
            REQUEST_CODE_PLAY_PAUSE,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(this, PlayerActionReceiver::class.java).apply {
            action = ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            this,
            REQUEST_CODE_NEXT,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 根據播放狀態選擇圖標
        val player = PlayerHolder.exoPlayer
        val isPlaying = player?.isPlaying ?: false
        val playPauseIcon = if (isPlaying) {
            R.drawable.baseline_pause_24 // 暫停圖標（需要添加）
        } else {
            R.drawable.baseline_play_arrow_24 // 播放圖標（需要添加）
        }
        val albumArtBitmap = BitmapFactory.decodeResource(resources, R.drawable.outline_music_note_24)
        val selectedFolder = PlayerHolder.uiState.value.folders.find { it.uri == PlayerHolder.uiState.value.selectedFolderUri }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            // Show controls on lock screen even when user hides sensitive content.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.outline_music_note_24)
            // Add media control buttons that invoke intents in your media service
            .addAction(R.drawable.outline_skip_previous_24, "Previous", previousPendingIntent) // #0
            .addAction(playPauseIcon, "Pause", playPausePendingIntent) // #1
            .addAction(R.drawable.outline_skip_next_24, "Next", nextPendingIntent) // #2
            // Apply the media style template.
            .setStyle(MediaStyleNotificationHelper.MediaStyle(mediaSession!!)
                .setShowActionsInCompactView(1 ,0,2))
            .setContentTitle("${selectedFolder?.name}")
            .setContentText("${PlayerHolder.uiState.value.currentMedia?.title}")
            .setLargeIcon(albumArtBitmap)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Anime 播放",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "顯示動畫播放控制"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val exo = PlayerHolder.exoPlayer ?: return START_NOT_STICKY

        // 處理停止播放的 Action
        intent?.action?.let { action ->
            if (action == ACTION_STOP) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        // 初始化 MediaSession 和通知
        if (mediaSession == null) {
            mediaSession = MediaSession.Builder(this, exo).build()
            createNotificationChannel()
            try {
                startForeground(NOTIFICATION_ID, buildNotification())
            } catch (e: Exception) {
                Log.e("PlayerService", "Failed to start foreground", e)
            }
        } else {
            updateNotification()
        }

        return START_STICKY
    }

    fun updateNotification() {
        val notification = buildNotification()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "animeplayer_channel"
        private const val NOTIFICATION_ID = 1

        const val ACTION_STOP = "com.coffeecat.animeplayer.ACTION_STOP"
        const val ACTION_PREVIOUS = "com.coffeecat.animeplayer.ACTION_PREVIOUS"
        const val ACTION_PLAY_PAUSE = "com.coffeecat.animeplayer.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.coffeecat.animeplayer.ACTION_NEXT"

        private const val REQUEST_CODE_MAIN = 0
        private const val REQUEST_CODE_STOP = 1
        private const val REQUEST_CODE_PREVIOUS = 2
        private const val REQUEST_CODE_PLAY_PAUSE = 3
        private const val REQUEST_CODE_NEXT = 4
    }
}

class PlayerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val player = PlayerHolder.exoPlayer

        when (intent.action) {
            PlayerService.ACTION_STOP -> {
                Log.d("PlayerService", "Stop action received")
                val stopIntent = Intent(context, PlayerService::class.java).apply {
                    action = PlayerService.ACTION_STOP
                }
                context.startService(stopIntent)
            }

            PlayerService.ACTION_PREVIOUS -> {
                Log.d("PlayerService", "Previous action received")
                player?.seekToPrevious()
            }

            PlayerService.ACTION_PLAY_PAUSE -> {
                Log.d("PlayerService", "Play/Pause action received")
                player?.let {
                    if (it.isPlaying) {
                        it.pause()
                    } else {
                        it.play()
                    }
                }
                // 更新通知以反映播放狀態變化
                val serviceIntent = Intent(context, PlayerService::class.java)
                context.startService(serviceIntent)
            }

            PlayerService.ACTION_NEXT -> {
                Log.d("PlayerService", "Next action received")
                player?.seekToNext()
            }
        }
    }
}