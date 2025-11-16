package com.coffeecat.player.ui.layer

import android.annotation.SuppressLint
import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.coffeecat.player.data.Danmu
import com.coffeecat.player.service.PlayerHolder
import kotlin.math.abs
import kotlin.math.max


@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("AutoboxingStateCreation")
@Composable
fun DanmuLayer(
    modifier: Modifier = Modifier,
) {

    val danmuSpeedMultiplier by remember { derivedStateOf { PlayerHolder.danmuSpeedMultiplier } }
    val danmuSizeDp by remember { derivedStateOf { PlayerHolder.danmuSizeDp } }
    val danmuOpacity by remember { derivedStateOf { PlayerHolder.danmuOpacity } }
    val danmuRange by remember { derivedStateOf { PlayerHolder.danmuRange } }
    val danmuLimit by remember { derivedStateOf { PlayerHolder.danmuLimit.toInt() } }

    val danmuBaseSpeed = 200f
    val density = LocalDensity.current.density

    var canvasWidth by remember { mutableStateOf(1920f) }
    var canvasHeight by remember { mutableStateOf(1080f) }

    val lineSpacingPx = 8f * density
    var linePositions by remember { mutableStateOf(listOf<Float>()) }

    val danmus = remember { mutableStateListOf<Danmu>() }
    val activeDanmus = remember { mutableStateListOf<Danmu>() }
    val danmuBuffer = remember { mutableStateListOf<Danmu>() }
    var lastDanmuTime by remember { mutableStateOf(0L) }
    var pointer by remember { mutableStateOf(0) }

    val uiState by PlayerHolder.uiState.collectAsState()
    val isDanmuEnabled = uiState.isDanmuEnabled

    val paint = remember { Paint().apply { isAntiAlias = true } }
    val redrawTrigger = remember { mutableStateOf(0) }

    var danmuSizePx = danmuSizeDp * density
    var densityInterval by remember { mutableStateOf(200L) }

    // -----------------------------
    // 工具函式
    // -----------------------------
    fun updateLinePositions() {
        val availableHeight = canvasHeight * danmuRange
        val totalLineHeight = danmuSizePx + lineSpacingPx
        val maxLines = max(1, (availableHeight / totalLineHeight).toInt())
        linePositions = List(maxLines) { i -> i * totalLineHeight + lineSpacingPx / 2 }
    }

    fun updateDensity() {
        val avgDuration = canvasWidth / (danmuBaseSpeed * danmuSpeedMultiplier)
        val maxPerSecond = danmuLimit / avgDuration
        densityInterval = if (maxPerSecond > 0) (1000 / maxPerSecond).toLong() else 1000L
    }

    fun clearDanmus() {
        activeDanmus.clear()
        danmuBuffer.clear()
        pointer = 0
        lastDanmuTime = 0
        danmus.forEach { it.shown = false }
    }

    fun getNonOverlappingLine(): Float? {
        if (linePositions.isEmpty()) {
            updateLinePositions()
            if (linePositions.isEmpty()) return 0f
        }
        val availableLines = mutableListOf<Float>()
        linePositions.forEach { lineY ->
            val lineDanmus = activeDanmus.filter { abs(it.y - lineY) < 2f }
            if (lineDanmus.isNotEmpty()) {
                val hasEdgeConflict = lineDanmus.any { it.x + it.textWidth >= canvasWidth - danmuSizePx }
                if (!hasEdgeConflict) availableLines.add(lineY)
            } else {
                availableLines.add(lineY)
            }
        }
        return availableLines.randomOrNull() ?: -1f
    }

    fun tryAddDanmu(d: Danmu): Boolean {
        if (activeDanmus.size >= danmuLimit) {
            danmuBuffer.add(d)
            return false
        }
        val lineY = getNonOverlappingLine() ?: run {
            danmuBuffer.add(d)
            return false
        }
        if (lineY < 0) {
            danmuBuffer.add(d)
            return false
        }
        d.shown = true
        d.x = canvasWidth
        d.y = lineY
        activeDanmus.add(d)
        return true
    }

    fun triggerDanmus(currentTimeMs: Long) {
        if (!isDanmuEnabled || danmus.isEmpty()) return

        val actualSpeed = danmuBaseSpeed * danmuSpeedMultiplier
        val totalDuration = canvasWidth / actualSpeed
        val timeWindowStart = currentTimeMs - 2000L
        val timeWindowEnd = currentTimeMs + (totalDuration * 1000).toLong() + 1000L

        // 將符合時間的彈幕加入 buffer
        while (pointer < danmus.size && danmus[pointer].startTime <= timeWindowEnd) {
            val d = danmus[pointer]
            if (!d.shown && d.startTime >= timeWindowStart) {
                danmuBuffer.add(d)
                d.shown = true
            }
            pointer++
        }

        // 按時間排序 buffer
        danmuBuffer.sortBy { it.startTime }
    }

    fun processBuffer(currentTimeMs: Long) {
        if (danmuBuffer.isEmpty()) return

        if (System.currentTimeMillis() - lastDanmuTime >= densityInterval) {
            if (getNonOverlappingLine() != -1f) {
                var nextDanmu: Danmu? = null
                while (danmuBuffer.isNotEmpty()) {
                    val candidate = danmuBuffer.removeAt(0)
                    if (abs(currentTimeMs - candidate.startTime) < 5000 || danmuBuffer.size < danmuLimit - activeDanmus.size) {
                        nextDanmu = candidate
                        break
                    }
                }
                nextDanmu?.let { tryAddDanmu(it) }
                lastDanmuTime = System.currentTimeMillis()
            }
        }
    }
    LaunchedEffect(danmuSizeDp, danmuRange, canvasWidth, canvasHeight) {
        danmuSizePx=danmuSizeDp*density
        updateLinePositions()
        updateDensity()
    }
    LaunchedEffect(danmuSizeDp) {
        clearDanmus()
        danmus.forEach {
            it.sizeDp=danmuSizeDp
            it.sizePx=danmuSizePx
            it.textWidth = it.textWidthBase * danmuSizePx/20f
        }
    }
    LaunchedEffect(PlayerHolder.clearDanmuTrigger.value) {
        if (PlayerHolder.clearDanmuTrigger.value) {
            clearDanmus()
            PlayerHolder.clearDanmuTrigger.value = false
        }
    }
    LaunchedEffect(Unit) {
        PlayerHolder.onDanmuLoaded = { list ->
            // 取得所有彈幕
            danmus.clear()
            danmus.addAll(list)
        }
    }
    // -----------------------------
    // LaunchedEffect 主循環
    // -----------------------------
    LaunchedEffect( danmus.toList()) {
        clearDanmus()
        if(danmus.isEmpty())return@LaunchedEffect
        // 預先計算文字寬度
        danmus.forEach {
            it.sizePx=danmuSizePx
            paint.textSize = 20f
            it.textWidthBase  = paint.measureText(it.text)
            it.textWidth = it.textWidthBase * danmuSizePx/20f
        }

        var lastTime = System.nanoTime()

        while (true) {
            withFrameNanos { now ->
                val delta = (now - lastTime) / 1_000_000_000f
                lastTime = now

                if (PlayerHolder.isPlaying && isDanmuEnabled) {
                    val currentTimeMs = PlayerHolder.exoplayerCurrentPosition

                    triggerDanmus(PlayerHolder.exoplayerCurrentPosition)
                    processBuffer(currentTimeMs)

                    // 移動 activeDanmus
                    val speed = danmuBaseSpeed * danmuSpeedMultiplier
                    val iterator = activeDanmus.listIterator()
                    while (iterator.hasNext()) {
                        val dm = iterator.next()
                        dm.x -= speed * delta
                        if (dm.x + dm.textWidth < 0) iterator.remove()
                    }
                }
                redrawTrigger.value++
            }
        }
    }

    // -----------------------------
    // Canvas 渲染
    // -----------------------------
    Canvas(modifier = modifier.onSizeChanged { size ->
        canvasWidth = size.width.toFloat()
        canvasHeight = size.height.toFloat()
        updateLinePositions()
        updateDensity()
    }) {
        redrawTrigger.value
        if(PlayerHolder.uiState.value.isDanmuEnabled) {
            paint.textSize = danmuSizePx
            activeDanmus.forEach { dm ->
                val baselineY = dm.y - paint.ascent()

                val textPath = android.graphics.Path()
                paint.getTextPath(dm.text, 0, dm.text.length, dm.x, baselineY, textPath)

                drawContext.canvas.withSave {
                    drawContext.canvas.nativeCanvas.clipOutPath(textPath)

                    val shadowPaint = Paint(paint).apply {
                        color = 0xFF000000.toInt()
                        alpha = 128
                        maskFilter = BlurMaskFilter(2f, BlurMaskFilter.Blur.NORMAL) // 2px blur
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        dm.text,
                        dm.x + 1f,
                        baselineY + 1f,
                        shadowPaint
                    )
                }
                paint.color = dm.color
                paint.alpha = (danmuOpacity * 255).toInt()
                drawContext.canvas.nativeCanvas.drawText(dm.text, dm.x, baselineY, paint)
            }
        }
    }
}
