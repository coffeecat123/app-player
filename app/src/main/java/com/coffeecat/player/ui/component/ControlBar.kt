package com.coffeecat.player.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeecat.player.R
import com.coffeecat.player.service.PlayerHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun ControlBar(
    orientation: String,
    modifier: Modifier = Modifier
) {
    val exoPlayer = PlayerHolder.exoPlayer ?: return
    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    val uiState by PlayerHolder.uiState.collectAsState()
    val isDanmuEnabled = uiState.isDanmuEnabled
    val selectedSpeed = PlayerHolder.playbackSpeed
    var expanded by remember { mutableStateOf(false) }


    LaunchedEffect(exoPlayer) {
        while (this.isActive) {
            isPlaying = exoPlayer.isPlaying
            delay(200)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.4f)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x00000000), Color(0xFF000000))
                )
            )
            .then(
                if (orientation == "LANDSCAPE")
                    Modifier.padding(horizontal = 32.dp)
                else
                    Modifier
            ),
        contentAlignment = Alignment.BottomStart
    ) {
        // 控制列
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = if (orientation == "LANDSCAPE")
                Modifier.fillMaxWidth().padding(bottom = 4.dp)
            else
                Modifier.fillMaxWidth().padding(bottom = 0.dp)
        ) {
            // 播放 / 暫停
            IconButton(onClick = {
                PlayerHolder.toggleControlsVisible(true)
                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White
                )
            }

            if(orientation!="LANDSCAPE") {
                PlayerSlider(scope, modifier = Modifier.weight(1f))
            }else{
                // 播放速度選擇
                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text("${selectedSpeed}x", color = Color(0xFFEEEEEE))
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        val speedOptions = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f)
                        speedOptions.forEach { speed ->
                            DropdownMenuItem(
                                onClick = {
                                    PlayerHolder.updatePlaybackSpeed(speed)
                                    expanded = false
                                },
                                text = { Text("${speed}x") }
                            )
                        }
                    }
                }
                //danmu
                IconButton(
                    onClick = { PlayerHolder.toggleIsDanmuEnabled() }
                ) {
                    Icon(
                        painter = painterResource(
                            if (isDanmuEnabled) R.drawable.danmu_on else R.drawable.danmu_off
                        ),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                }
                IconButton(
                    onClick = { PlayerHolder.toggleIsDanmuSettingVisible() }
                ) {
                    Icon(
                        painter = painterResource(
                            R.drawable.danmu_setting
                        ),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            // 全螢幕
            IconButton(onClick = { PlayerHolder.onFullScreenButtonClicked() }) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "FullScreen",
                    tint = Color.White
                )
            }
        }

        // Slider 位置根據方向調整
        if (orientation == "LANDSCAPE") {
            // LANDSCAPE 上方 Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
                    .offset(y = (-36).dp)
            ) {
                PlayerSlider(scope)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSlider(
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    val exoPlayer = PlayerHolder.exoPlayer ?: return
    val pos = PlayerHolder.currentPosition
    val duration = PlayerHolder.duration
    var seekJob by remember { mutableStateOf<Job?>(null) }

    Box(modifier = modifier.fillMaxWidth()) {
        // 時間文字靠右
        Text(
            text = "${formatTime(pos)} / ${formatTime(duration)}",
            color = Color(0xFFEEEEEE),
            fontSize = 12.sp,
            maxLines = 1,
            modifier = Modifier.align(Alignment.CenterEnd)
                .offset(y = (-16).dp)
        )
        //
        Slider(
            value = pos.toFloat(),
            onValueChange = { newValue ->
                if (PlayerHolder.draggingSeekPosMs == null && seekJob == null)
                    PlayerHolder.lastPlayingState = exoPlayer.isPlaying
                PlayerHolder.draggingSeekPosMs = newValue.toLong()
                PlayerHolder.currentPosition = newValue.toLong()
                exoPlayer.pause()
                seekJob?.cancel()
                seekJob = scope.launch {
                    delay(100L)
                    exoPlayer.seekTo(newValue.toLong())
                }
            },
            onValueChangeFinished = {
                seekJob?.cancel()
                exoPlayer.seekTo(PlayerHolder.currentPosition)
                if (PlayerHolder.lastPlayingState)
                    exoPlayer.play()
                PlayerHolder.draggingSeekPosMs = null
                PlayerHolder.clearDanmuTrigger.value=true
            },
            valueRange = 0f..duration.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
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

// 格式化時間函式，可共用
fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}