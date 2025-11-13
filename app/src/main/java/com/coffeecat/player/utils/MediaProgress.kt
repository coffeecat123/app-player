package com.coffeecat.animeplayer.utils

import kotlinx.serialization.Serializable

@Serializable
data class MediaProgress(
    val current: Long,
    val duration: Long
)