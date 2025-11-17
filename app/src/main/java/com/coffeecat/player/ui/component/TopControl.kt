package com.coffeecat.player.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeecat.player.data.MediaInfo
import com.coffeecat.player.data.Orientation

@Composable
fun TopControl(
    media: MediaInfo,
    orientation: Orientation,
    modifier: Modifier
) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.3f)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF000000), Color(0x00000000))
                )
            )
            .then(
                if (orientation == Orientation.LANDSCAPE)
                    Modifier.padding(horizontal = 32.dp)
                else
                    Modifier
            ),
        contentAlignment = Alignment.TopStart
    ) {
        Text(
            text = media.title,
            color = Color(0xFFEEEEEE),
            fontSize = 20.sp,
            maxLines = 1,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp)
        )
    }
}
