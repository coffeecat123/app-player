package com.coffeecat.player.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import com.coffeecat.player.MainActivity
import com.coffeecat.player.R
import com.coffeecat.player.data.RepeatMode
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
    private lateinit var mediaSession: MediaSession
    lateinit var mediaSessionCallback: MediaSessionCallback
    private var mediaSessionReleased = true


    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        PlayerHolder.service = this

        val player = PlayerHolder.exoPlayer ?: return

        setupPlayerListener(player)

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
                    PlayerHolder.exoplayerCurrentPosition = currentPos
                }
                PlayerHolder.duration = currentDur
                PlayerHolder.isPlaying = isPlaying
                PlayerHolder.saveProgress(applicationContext)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
        autoSaveJob?.cancel()
        PlayerHolder.saveProgress(applicationContext)
        if(!mediaSessionReleased) {
            mediaSession.release()
            mediaSessionReleased=true
        }
        PlayerHolder.exoPlayer?.release()
        PlayerHolder.clear()
        PlayerHolder.service = null
        serviceScope.cancel()
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (!::mediaSession.isInitialized || mediaSessionReleased) {
            initMediaSessionIfNeeded()
        }
        return START_NOT_STICKY
    }
    fun stopAll() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        autoSaveJob?.cancel()
        PlayerHolder.saveProgress(applicationContext)
        if(!mediaSessionReleased) {
            mediaSession.release()
            mediaSessionReleased=true
        }
        PlayerHolder.exoPlayer?.release()
        PlayerHolder.clear()
        PlayerHolder.service = null
        serviceScope.cancel()
        stopSelf()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    @OptIn(UnstableApi::class)
    fun initMediaSessionIfNeeded() {
        val player = PlayerHolder.exoPlayer ?: return

        setMediaNotificationProvider(
            CustomMediaNotificationProvider(
                this
            )
        )
        mediaSessionCallback = MediaSessionCallback(this)

        mediaSession =
            MediaSession
                .Builder(this, player)
                .setCallback(mediaSessionCallback)
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
        MediaController.Builder(this, sessionToken).buildAsync()
        //val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        //controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())

        updateNotification()
        mediaSessionReleased = false
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

    fun updateNotification() {
        val uiState=PlayerHolder.uiState.value
        val button1=CommandButton.Builder()
            .setDisplayName("previous")
            .setIconResId(R.drawable.round_skip_previous_36)
            .setSessionCommand(MediaSessionConstants.CommandPrevious)
            .build()

        val button2=CommandButton.Builder()
            .setDisplayName("next")
            .setIconResId(R.drawable.round_skip_next_36)
            .setSessionCommand(MediaSessionConstants.CommandNext)
            .build()
        val musicIcon=if(uiState.isShuffle) R.drawable.outline_shuffle_on_24 else R.drawable.round_music_note_36
        val button3=CommandButton.Builder()
        .setDisplayName("music")
            .setIconResId(musicIcon)
            .setSessionCommand(MediaSessionConstants.CommandMusic)
            .build()
        val repeatIcon=when(uiState.repeatMode){
            RepeatMode.NO_REPEAT -> R.drawable.rounded_repeat_24
            RepeatMode.REPEAT_ONE -> R.drawable.outline_repeat_one_on_24
            RepeatMode.REPEAT_ALL -> R.drawable.rounded_repeat_on_24
        }
        val button4=CommandButton.Builder()
            .setDisplayName("repeatMode")
            .setIconResId(repeatIcon)
            .setSessionCommand(MediaSessionConstants.CommandRepeat)
            .build()
        val buttons = listOf(button1,button2,button3,button4)
        mediaSession.setCustomLayout(buttons)
    }
}