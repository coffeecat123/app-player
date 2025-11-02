package com.coffeecat.animeplayer.viewmodel

import kotlinx.serialization.Serializable

@Serializable
data class MediaProgress(
    val current: Long,
    val duration: Long
)