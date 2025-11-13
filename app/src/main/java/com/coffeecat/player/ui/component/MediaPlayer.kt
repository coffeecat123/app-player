package com.coffeecat.player.ui.component

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.coffeecat.player.data.MediaInfo
import com.coffeecat.player.service.PlayerHolder
import com.coffeecat.player.ui.layer.DanmuLayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("AutoboxingStateCreation")
@Composable
fun MediaPlayer(
    media: MediaInfo
) {
    val context = LocalContext.current
    val exoPlayer = PlayerHolder.exoPlayer
    val uiState by PlayerHolder.uiState.collectAsState()
    val controlsVisible = uiState.controlsVisible
    val isDanmuSettingVisible = uiState.isDanmuSettingVisible
    val nowOrientation = uiState.nowOrientation
    val scope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<Job?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var dragStartX by remember { mutableStateOf(0f) }
    var skippingMs by remember { mutableStateOf(0L) } // 顯示用
    var dragAllowed by remember { mutableStateOf(false) }
    var skippingtime by remember { mutableStateOf(0L) }


    var lastClickTime by remember { mutableStateOf(0L) }
    var clickJob by remember { mutableStateOf<Job?>(null) }

    fun resetHideTimer() {
        hideJob?.cancel()
        hideJob = scope.launch {
            delay(2000)
            PlayerHolder.toggleControlsVisible(false)
            PlayerHolder.toggleIsDanmuSettingVisible(false)
        }
    }
    fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%02d:%02d".format(min, sec)
    }
    /*
    LaunchedEffect(exoPlayer) {
        while (this.isActive) {
            if(exoPlayer==null)return@LaunchedEffect
            if (exoPlayer.duration != C.TIME_UNSET && exoPlayer.currentMediaItem != null) {
                if (PlayerHolder.draggingSeekPosMs == null) {
                    PlayerHolder.currentPosition = exoPlayer.currentPosition
                }
                PlayerHolder.duration = exoPlayer.duration
            }

            delay(200)
        }
    }

     */
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            resetHideTimer()
        }
    }
    if (exoPlayer != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp)
                .pointerInput(controlsVisible,isDanmuSettingVisible) {
                    val sidebarWidthPx = with(context) { 360.dp.toPx() } // 側欄寬度
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue

                            if (isDanmuSettingVisible && change.position.x > size.width - sidebarWidthPx) {
                                continue
                            }
                            hideJob?.cancel()

                            // pointer down
                            if (change.changedToDown()) {
                                dragStartX = change.position.x
                                isDragging = false

                                val now = System.currentTimeMillis()
                                val doubleClickDelay = 300L

                                val boxHeight = size.height.toFloat()
                                val yFraction = change.position.y / boxHeight
                                dragAllowed = yFraction in 0.2f..0.8f
                                if (controlsVisible && yFraction !in 0.3f..0.8f) {
                                    clickJob?.cancel()
                                    change.consume()
                                    continue
                                }

                                if (now - lastClickTime < doubleClickDelay) {
                                    // double click
                                    clickJob?.cancel()
                                    exoPlayer.let { if (it.isPlaying) it.pause() else it.play() }
                                    lastClickTime = 0L
                                } else {
                                    // single click (lazy)
                                    clickJob = scope.launch {
                                        delay(doubleClickDelay)
                                        PlayerHolder.toggleControlsVisible()
                                        PlayerHolder.toggleIsDanmuSettingVisible(false)
                                    }
                                    lastClickTime = now
                                }
                            }

                            // pointer move (drag)
                            if (change.pressed && change.positionChanged()) {
                                if (!dragAllowed) continue
                                PlayerHolder.toggleControlsVisible(true)
                                val newx = change.position.x
                                // 防止輕微抖動誤觸
                                if (!isDragging && abs(newx - dragStartX) > 12.dp.toPx()) {
                                    isDragging = true
                                    PlayerHolder.lastPlayingState = exoPlayer.isPlaying
                                    exoPlayer.pause() // 拖曳時先停
                                    clickJob?.cancel() // 不要觸發單擊
                                    dragStartX = (newx * 0.6 + dragStartX * 0.4).toFloat()
                                }

                                if (isDragging) {
                                    val deltaX = newx - dragStartX
                                    val width = size.width.toFloat()

                                    // 對齊原 JS 計算
                                    val xpos = (abs(deltaX) / width).coerceIn(0f, 1f)
                                    val t = xpos * 120f // 最多 120 秒

                                    val current = exoPlayer.currentPosition
                                    val duration = exoPlayer.duration

                                    skippingMs = if (deltaX > 0) {
                                        // 往右：快進
                                        val forward = current + (t * 1000).toLong()
                                        (forward - current).coerceAtMost(duration - current)
                                    } else {
                                        // 往左：倒退
                                        val backward = current - (t * 1000).toLong()
                                        (backward - current).coerceAtLeast(-current)
                                    }
                                    val nexPos = (exoPlayer.currentPosition + skippingMs)
                                        .coerceIn(0, exoPlayer.duration)
                                    PlayerHolder.draggingSeekPosMs = nexPos
                                    PlayerHolder.currentPosition = nexPos
                                    skippingtime = skippingMs
                                    change.consume()
                                }
                            }

                            // pointer up
                            if (change.changedToUp()) {
                                dragAllowed = false
                                if (isDragging) {
                                    PlayerHolder.draggingSeekPosMs?.let {
                                        if (abs(it - exoPlayer.currentPosition) > 3000) {
                                            PlayerHolder.clearDanmuTrigger.value = true
                                        }
                                        exoPlayer.seekTo(it)
                                    }
                                    PlayerHolder.draggingSeekPosMs = null
                                    if (PlayerHolder.lastPlayingState)
                                        exoPlayer.play()

                                    isDragging = false
                                    skippingMs = 0
                                }
                            }
                            if (change.changedToUpIgnoreConsumed()) {
                                resetHideTimer()
                            }
                        }
                    }
                }
        ) {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        player = exoPlayer
                        useController = false
                    }
                },
                modifier = Modifier.align(Alignment.Center)
            )
            DanmuLayer(
                modifier = Modifier.matchParentSize()
            )
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    TopControl(
                        media = media,
                        orientation = nowOrientation,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                    ControlBar(
                        orientation = nowOrientation,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
            //danmu_setting
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                AnimatedVisibility(
                    visible = isDanmuSettingVisible,
                    enter = slideInHorizontally(
                        initialOffsetX = { it }, // 從右邊滑入
                        animationSpec = tween(durationMillis = 300)
                    ),
                    exit = slideOutHorizontally(
                        targetOffsetX = { it }, // 往右滑出
                        animationSpec = tween(durationMillis = 300)
                    )
                ) {
                    DanmuSettingSidebar(
                        modifier = Modifier,
                        { resetHideTimer() }
                    ) {
                        DanmuSettingMenu()
                    }
                }
            }

            // 拖曳預覽
            Box(modifier = Modifier.fillMaxSize()) {
                val previewTime = PlayerHolder.draggingSeekPosMs ?: exoPlayer.currentPosition
                val dragAlpha = if (isDragging) 1f else 0f
                Text(
                    text = "${formatTime(previewTime)} / ${formatTime(exoPlayer.duration)}\n" +
                            "${if (skippingtime >= 0) "+" else "-"}${formatTime(abs(skippingtime))}",
                    fontSize = 20.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer { alpha = dragAlpha }
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}