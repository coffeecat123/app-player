package com.coffeecat.animeplayer.utils

import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    val autoPlay: Boolean = true,
    val backgroundPlaying: Boolean = true
)