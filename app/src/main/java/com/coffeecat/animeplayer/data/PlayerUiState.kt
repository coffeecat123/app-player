package com.coffeecat.animeplayer.data

import android.net.Uri

enum class PlayerLocation {
    HOME,
    FOLDER,
    SETTING
}
data class PlayerUiState(
    val folders: List<FolderInfo> = emptyList(),
    val currentMedia: MediaInfo? = null,
    val isFullScreen: Boolean = false,
    val canFullScreen: Boolean = false,
    val nowOrientation: String = "PORTRAIT",
    val location: PlayerLocation = PlayerLocation.HOME,
    val selectedFolderUri: Uri? = null,
    val canDelete: Boolean = false,
    val currentPosition: Long = 0L,
    val controlsVisible: Boolean = true,
    val isDetailsVisible: Boolean = true,
    val isMainActivityVisible: Boolean = true,
    val isDanmuEnabled: Boolean = true,
    val isDanmuSettingVisible: Boolean = false
)