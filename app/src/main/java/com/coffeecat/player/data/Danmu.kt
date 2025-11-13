package com.coffeecat.player.data

data class Danmu(
    val id: Int,
    val text: String,
    val color: Int,
    val startTime: Long,
    var x: Float = 0f,
    var y: Float = 0f,
    var sizeDp: Float = 30f,
    var sizePx: Float = 0f,
    var textWidth: Float = 0f,
    var textWidthBase : Float = 0f,
    var shown: Boolean = false
)