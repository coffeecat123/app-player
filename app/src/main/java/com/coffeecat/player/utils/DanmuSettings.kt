package com.coffeecat.animeplayer.utils

import kotlinx.serialization.Serializable

@Serializable
data class DanmuSettings(
    val speedMultiplier: Float = 1f,
    val sizeDp: Float = 24f,
    val opacity: Float = 1f,
    val range: Float = 1f,
    val limit: Float = 200f
)