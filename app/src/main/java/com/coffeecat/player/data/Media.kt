package com.coffeecat.player.data

import android.net.Uri


data class MediaInfo(
    val title: String,
    val uri: Uri,
    val duration: Long,
    val fileName: String,
    val extension: String,
    val isVideo: Boolean,
    val danmuUri: Uri? = null
)