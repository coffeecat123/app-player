package com.coffeecat.player.service

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class MediaSessionCallback(
    private val context: android.content.Context
) : MediaSession.Callback {

    /** 處理自訂按鈕 */
    @OptIn(UnstableApi::class)
    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            MediaSessionConstants.ACTION_MUSIC -> {
                //
            }
            MediaSessionConstants.ACTION_PREVIOUS -> {
                handlePrevious()
            }
            MediaSessionConstants.ACTION_NEXT -> {
                handleNext()
            }
            MediaSessionConstants.ACTION_STOP -> {
                val player = PlayerHolder.exoPlayer
                player?.pause()

                PlayerHolder.service?.stopForegroundNotification()
            }
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }
    private fun handlePrevious() {
        val ui = PlayerHolder.uiState.value
        val folder = ui.currentMediaFolder?:return
        val list = folder.medias
        val current = ui.currentMedia ?: return

        if (list.isEmpty()) return

        val index = list.indexOfFirst { it.uri == current.uri }
        if (index == -1) return

        val previousIndex = (index - 1 + list.size) % list.size
        val previousMedia = list[previousIndex]

        PlayerHolder.selectMedia(previousMedia, context,folder)
    }
    private fun handleNext() {
        val ui = PlayerHolder.uiState.value
        val folder = ui.currentMediaFolder?:return
        val list = folder.medias
        val current = ui.currentMedia ?: return

        if (list.isEmpty()) return

        val index = list.indexOfFirst { it.uri == current.uri }
        if (index == -1) return

        val nextIndex = (index + 1) % list.size
        val nextMedia = list[nextIndex]

        PlayerHolder.selectMedia(nextMedia, context,folder)
    }

    @OptIn(UnstableApi::class)
    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)
        return MediaSession.ConnectionResult.accept(
            connectionResult.availableSessionCommands
                .buildUpon()
                .add(MediaSessionConstants.CommandMusic)
                .add(MediaSessionConstants.CommandPrevious)
                .add(MediaSessionConstants.CommandNext)
                .add(MediaSessionConstants.CommandStop)
                .build(),
            connectionResult.availablePlayerCommands
                .buildUpon()
                //.remove(Player.COMMAND_PLAY_PAUSE)
                // 由於您有自訂的上一曲/下一曲按鈕，移除標準的跳轉指令
                .remove(Player.COMMAND_SEEK_TO_NEXT)
                .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
                .build()
        )
    }
}
