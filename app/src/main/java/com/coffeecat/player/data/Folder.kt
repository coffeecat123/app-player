package com.coffeecat.player.data

import android.net.Uri


data class FolderInfo(
    val name: String,
    val uri: Uri,
    val medias: List<MediaInfo>
)