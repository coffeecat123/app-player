package com.coffeecat.animeplayer.service

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

class MediaLibraryCallback(
    private val context: android.content.Context
) : MediaLibrarySession.Callback {

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
                val currentFolder = PlayerHolder.uiState.value.folders.find {
                    it.uri == PlayerHolder.uiState.value.selectedFolderUri
                }
                val currentMedia = PlayerHolder.uiState.value.currentMedia

                if (currentFolder != null && currentMedia != null) {
                    val currentIndex = currentFolder.medias.indexOfFirst { it.uri == currentMedia.uri }
                    val previousIndex = (currentIndex-1+currentFolder.medias.size)%currentFolder.medias.size
                    val previousMedia = currentFolder.medias[previousIndex]
                    PlayerHolder.selectMedia(previousMedia, context)
                }
            }
            MediaSessionConstants.ACTION_NEXT -> {
                val currentFolder = PlayerHolder.uiState.value.folders.find {
                    it.uri == PlayerHolder.uiState.value.selectedFolderUri
                }
                val currentMedia = PlayerHolder.uiState.value.currentMedia

                if (currentFolder != null && currentMedia != null) {
                    val currentIndex = currentFolder.medias.indexOfFirst { it.uri == currentMedia.uri }
                    val previousIndex = (currentIndex+1+currentFolder.medias.size)%currentFolder.medias.size
                    val previousMedia = currentFolder.medias[previousIndex]
                    PlayerHolder.selectMedia(previousMedia, context)
                }
            }
            MediaSessionConstants.ACTION_STOP -> {
                val player = PlayerHolder.exoPlayer
                player?.stop()
            }
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
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
                // 由於您有自訂的播放/暫停按鈕，移除標準的播放/暫停指令
                //.remove(Player.COMMAND_PLAY_PAUSE)
                // 由於您有自訂的上一曲/下一曲按鈕，移除標準的跳轉指令
                .remove(Player.COMMAND_SEEK_TO_NEXT)
                .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
                .build()
        )
    }
}
