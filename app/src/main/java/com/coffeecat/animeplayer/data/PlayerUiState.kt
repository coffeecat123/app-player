package com.coffeecat.animeplayer.data

import android.net.Uri

data class PlayerUiState(
    val folders: List<FolderInfo> = emptyList(),
    val currentMedia: MediaInfo? = null,
    val isFullScreen: Boolean = false,
    val canFullScreen: Boolean = false,
    val nowOrientation: String = "PORTRAIT",
    val selectedFolderUri: Uri? = null,
    val canDelete: Boolean = false,
    val currentPosition: Long = 0L,
    val controlsVisible: Boolean = true,
    val isDetailsVisible: Boolean = true
)