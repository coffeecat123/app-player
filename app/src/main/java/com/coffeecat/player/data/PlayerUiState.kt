package com.coffeecat.player.data

enum class PlayerLocation {
    HOME,
    FOLDER,
    SETTING
}
enum class RepeatMode {
    NO_REPEAT,
    REPEAT_ONE,
    REPEAT_ALL;

    fun next(): RepeatMode = when(this) {
        NO_REPEAT -> REPEAT_ONE
        REPEAT_ONE -> REPEAT_ALL
        REPEAT_ALL -> NO_REPEAT
    }
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
    val isDanmuSettingVisible: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.NO_REPEAT,
    val isShuffle: Boolean=false
)