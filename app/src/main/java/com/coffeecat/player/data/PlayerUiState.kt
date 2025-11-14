package com.coffeecat.player.data

import android.net.Uri

enum class PlayerLocation {
    HOME,
    FOLDER,
    SETTING
}
data class PlayerUiState(
    val folders: List<FolderInfo> = emptyList(),
    val currentMedia: MediaInfo? = null,
    val currentMediaFolder: FolderInfo? = null,
    val isFullScreen: Boolean = false,
    val canFullScreen: Boolean = false,
    val nowOrientation: String = "PORTRAIT",
    val location: PlayerLocation = PlayerLocation.HOME,
    val selectedFolder: FolderInfo? = null,
    val canDelete: Boolean = false,
    val currentPosition: Long = 0L,
    val controlsVisible: Boolean = true,
    val isDetailsVisible: Boolean = true,
    val isMainActivityVisible: Boolean = true,
    val isDanmuEnabled: Boolean = true,
    val isDanmuSettingVisible: Boolean = false
)