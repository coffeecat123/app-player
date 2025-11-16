package com.coffeecat.player.utils

import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    val autoPlay: Boolean = true,
    val backgroundPlaying: Boolean = true,
    val alwaysRestart: Boolean = false
)