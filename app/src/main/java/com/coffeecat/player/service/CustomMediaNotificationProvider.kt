package com.coffeecat.player.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.coffeecat.player.MainActivity
import com.coffeecat.player.R
import com.google.common.collect.ImmutableList

@OptIn(UnstableApi::class)
class CustomMediaNotificationProvider(
    private val context: Context
) : MediaNotification.Provider {

    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val channelId = "player_channel"
    private val notificationId = 1

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "player notifications"
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun createNotification(
        session: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {

        val player = session.player
        val mediaMetadata = player.currentMediaItem?.mediaMetadata
        val title = mediaMetadata?.title?.toString() ?: "Player"
        val artist = mediaMetadata?.artist?.toString() ?: "Unknown"

        // 创建点击通知返回应用的 Intent
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建通知
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.round_queue_music_24)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(
                MediaStyleNotificationHelper.MediaStyle(session)
            )
            .setShowWhen(false)
            .setOngoing(player.isPlaying)

        // 处理 customLayout 中的按钮
        for (button in customLayout) {
            try {
                val command = button.sessionCommand
                if (command != null) {
                    val displayName = button.displayName
                    val iconResId = button.iconResId

                    // 将 Int 资源 ID 转换为 IconCompat
                    val icon = IconCompat.createWithResource(context, iconResId)

                    // 获取对应的 Player.Command
                    val playerCommand = getPlayerCommand(command.customAction)

                    // 创建通知操作
                    val action = actionFactory.createMediaAction(
                        session,
                        icon,
                        displayName,
                        playerCommand
                    )
                    notificationBuilder.addAction(action)
                }
            } catch (e: Exception) {
                // 跳过无法处理的按钮
            }
        }

        // 设置通知样式（仅用于 API 31+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notificationBuilder.setForegroundServiceBehavior(
                NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
            )
        }
        return MediaNotification(notificationId, notificationBuilder.build())
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: Bundle
    ): Boolean {
        // 处理自定义命令
        return false
    }

    /**
     * 将自定义命令字符串转换为 Player.Command
     * 需要根据你的 MediaSessionConstants 来映射
     */
    private fun getPlayerCommand(customAction: String): Int {
        return when (customAction) {
            MediaSessionConstants.CommandMusic.customAction -> androidx.media3.common.Player.COMMAND_PLAY_PAUSE
            MediaSessionConstants.CommandPrevious.customAction -> androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
            MediaSessionConstants.CommandNext.customAction -> androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
            MediaSessionConstants.CommandStop.customAction -> androidx.media3.common.Player.COMMAND_STOP
            else -> androidx.media3.common.Player.COMMAND_PLAY_PAUSE
        }
    }
}