package com.coffeecat.player.ui.component

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.coffeecat.player.service.PlayerHolder

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("AutoboxingStateCreation")
@Composable
fun DanmuSettingMenu() {
    var speed by remember { mutableStateOf(PlayerHolder.danmuSpeedMultiplier) }
    var size by remember { mutableStateOf(PlayerHolder.danmuSizeDp) }
    var opacity by remember { mutableStateOf(PlayerHolder.danmuOpacity) }
    var range by remember { mutableStateOf(PlayerHolder.danmuRange) }
    var limit by remember { mutableStateOf(PlayerHolder.danmuLimit) }
    val context= LocalContext.current

    Column(modifier = Modifier.padding(8.dp)) {
        // Speed
        Text("Speed: %.1fx".format(speed), color = Color.White)
        Slider(
            value = speed,
            onValueChange = {
                speed = it
                PlayerHolder.danmuSpeedMultiplier = it
            },
            onValueChangeFinished = {
                PlayerHolder.saveDanmuSettings(context)
            },
            valueRange = 0.5f..3f,
            modifier = Modifier.padding(horizontal = 0.dp),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color(0xFFFF6B6B), shape = CircleShape)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(8.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFFFF6B6B),
                        inactiveTrackColor = Color(0x80646464)
                    )
                )
            }
        )

        // Size
        Text("Size: ${size.toInt()}px", color = Color.White)
        Slider(
            value = size,
            onValueChange = {
                size = it
                PlayerHolder.danmuSizeDp = it
            },
            onValueChangeFinished = {
                PlayerHolder.saveDanmuSettings(context)
            },
            valueRange = 12f..36f,
            modifier = Modifier.padding(horizontal = 8.dp),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color(0xFFFF6B6B), shape = CircleShape)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(8.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFFFF6B6B),
                        inactiveTrackColor = Color(0x80646464)
                    )
                )
            }
        )

        // Opacity
        Text("Opacity: %.0f%%".format(opacity * 100), color = Color.White)
        Slider(
            value = opacity,
            onValueChange = {
                opacity = it
                PlayerHolder.danmuOpacity = it
            },
            onValueChangeFinished = {
                PlayerHolder.saveDanmuSettings(context)
            },
            valueRange = 0.01f..1f,
            modifier = Modifier.padding(horizontal = 8.dp),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color(0xFFFF6B6B), shape = CircleShape)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(8.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFFFF6B6B),
                        inactiveTrackColor = Color(0x80646464)
                    )
                )
            }
        )

        // Range
        Text("Range: %.0f%%".format(range * 100), color = Color.White)
        Slider(
            value = range,
            onValueChange = {
                range = it
                PlayerHolder.danmuRange = it
            },
            onValueChangeFinished = {
                PlayerHolder.saveDanmuSettings(context)
            },
            valueRange = 0.1f..1f,
            modifier = Modifier.padding(horizontal = 8.dp),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color(0xFFFF6B6B), shape = CircleShape)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(8.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFFFF6B6B),
                        inactiveTrackColor = Color(0x80646464)
                    )
                )
            }
        )

        // Limit
        Text("Limit: ${limit.toInt()}", color = Color.White)
        Slider(
            value = limit,
            onValueChange = {
                limit = it
                PlayerHolder.danmuLimit = it
            },
            onValueChangeFinished = {
                PlayerHolder.saveDanmuSettings(context)
            },
            valueRange = 5f..200f,
            modifier = Modifier.padding(horizontal = 8.dp),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color(0xFFFF6B6B), shape = CircleShape)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(8.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFFFF6B6B),
                        inactiveTrackColor = Color(0x80646464)
                    )
                )
            }
        )
    }
}
