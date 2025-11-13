package com.coffeecat.animeplayer.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.coffeecat.animeplayer.MainActivity
import com.coffeecat.animeplayer.R
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerService : MediaLibraryService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var autoSaveJob: Job? = null
    private lateinit var mediaSession: MediaLibrarySession
    lateinit var mediaLibrarySessionCallback: MediaLibraryCallback

    private lateinit var connectivityManager: ConnectivityManager


    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()


        val player = PlayerHolder.exoPlayer ?: return

        // 關鍵步驟：使用自訂的 CustomMediaNotificationProvider
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.app_name
            )
                .apply {
                    setSmallIcon(R.drawable.round_queue_music_24)
                }
        )
        mediaLibrarySessionCallback = MediaLibraryCallback(this)

        mediaSession =
            MediaLibrarySession
                .Builder(this, player, mediaLibrarySessionCallback)
                .setSessionActivity(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
                .build()

        val sessionToken = SessionToken(this, ComponentName(this, PlayerService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())

        setupPlayerListener(player)
        updateNotification()

        connectivityManager = getSystemService()!!

        autoSaveJob?.cancel()
        autoSaveJob = serviceScope.launch {
            while (isActive) {
                delay(200)
                val player = PlayerHolder.exoPlayer ?: continue

                val playerState = withContext(Dispatchers.Main) {
                    if (player.duration != C.TIME_UNSET && player.currentMediaItem != null) {
                        Triple(player.currentPosition, player.duration, player.isPlaying)
                    } else null
                } ?: continue

                val currentPos = playerState.first
                val currentDur = playerState.second
                val isPlaying = playerState.third

                if (PlayerHolder.draggingSeekPosMs == null) {
                    PlayerHolder.currentPosition = currentPos
                }
                PlayerHolder.duration = currentDur
                PlayerHolder.isPlaying = isPlaying
                PlayerHolder.saveProgress(applicationContext)

                updateNotification()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        autoSaveJob?.cancel()
        PlayerHolder.saveProgress(applicationContext)
        mediaSession.release()
        PlayerHolder.exoPlayer?.release()
        PlayerHolder.clear()
        serviceScope.cancel()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
        return mediaSession
    }

    private fun setupPlayerListener(player: ExoPlayer) {
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                updateNotification()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                updateNotification()
            }
        })
    }

    private fun updateNotification() {
        val buttons = listOf(

            CommandButton.Builder()
                .setDisplayName("previous")
                .setIconResId(R.drawable.round_skip_previous_36)
                .setSessionCommand(MediaSessionConstants.CommandPrevious)
                .build(),

            CommandButton.Builder()
                .setDisplayName("next")
                .setIconResId(R.drawable.round_skip_next_36)
                .setSessionCommand(MediaSessionConstants.CommandNext)
                .build(),

            CommandButton.Builder()
                .setDisplayName("music")
                .setIconResId(R.drawable.round_music_note_36)
                .setSessionCommand(MediaSessionConstants.CommandMusic)
                .build(),

            CommandButton.Builder()
                .setDisplayName("stop")
                .setIconResId(R.drawable.round_close_36)
                .setSessionCommand(MediaSessionConstants.CommandStop)
                .build()
        )
        mediaSession.setCustomLayout(buttons)
    }

    companion object {
        private const val CHANNEL_ID = "animeplayer_channel"
        private const val NOTIFICATION_ID = 1
    }
}