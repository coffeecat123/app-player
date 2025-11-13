package com.coffeecat.player.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun DanmuSettingSidebar(
    modifier: Modifier = Modifier,
    resetHideTimer: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(360.dp)
            .background(
                color = Color.Black.copy(alpha = 0.7f),
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while(true){
                        val event = awaitPointerEvent()
                        // 如果點擊在 Sidebar 外面，才消費事件
                        val sidebarWidth = 360.dp.toPx() // 側欄寬
                        resetHideTimer()
                        event.changes.forEach { change ->
                            if (change.position.x < size.width - sidebarWidth) {
                                change.consume()
                            }
                        }
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ){
            content()
        }
    }
}
