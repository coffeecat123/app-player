package com.coffeecat.player.ui.component

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.coffeecat.player.R
import com.coffeecat.player.data.PlayerLocation
import com.coffeecat.player.service.PlayerHolder


@OptIn(UnstableApi::class)
@Composable
fun MainContent(
    context: Context,
    onAddFolder: () -> Unit
){

    val uiState by PlayerHolder.uiState.collectAsState()
    val settings by PlayerHolder.settings.collectAsState()
    val folders = uiState.folders
    val canDelete = uiState.canDelete
    val isDetailsVisible = uiState.isDetailsVisible
    val selectedFolder = uiState.selectedFolder
    val currentMediaFolder = uiState.currentMediaFolder
    val mediaProgressMap by PlayerHolder.mediaProgressMap.collectAsState(initial = emptyMap())

    val formatMillis: (Long) -> String = { millis ->
        val seconds = millis / 1000
        "%02d:%02d".format(seconds / 60, seconds % 60)
    }


    BackHandler {
        when(uiState.location) {
            PlayerLocation.FOLDER ->{
                PlayerHolder.goBack()
            }
            PlayerLocation.SETTING -> {
                PlayerHolder.goBack()
            }
            PlayerLocation.HOME ->{
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF222222))
    ) {
        Row (verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color(0xFF404040))){
            when (uiState.location) {
                PlayerLocation.FOLDER -> {
                    IconButton(onClick = {
                        PlayerHolder.goBack()
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_back),
                            contentDescription = "back",
                            tint = Color(0xFFEEEEEE)
                        )
                    }
                    if(selectedFolder==null)return@Row
                    Text(
                        selectedFolder.name,
                        fontSize = 24.sp,
                        maxLines = 1,
                        color = Color(0xFFEEEEEE),
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        PlayerHolder.toggleIsDetailsVisible()
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.outline_density_small_24),
                            contentDescription = null,
                            tint = Color(0xFFEEEEEE)
                        )
                    }
                    IconButton(onClick = { PlayerHolder.updateLocation(PlayerLocation.SETTING) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_settings_24),
                            contentDescription = "setting",
                            tint = Color(0xFFEEEEEE)
                        )
                    }
                }
                PlayerLocation.HOME -> {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { PlayerHolder.toggleCanDelete() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.outline_delete_24),
                            contentDescription = "X",
                            tint = Color(0xFFEEEEEE)
                        )
                    }
                    IconButton(onClick = onAddFolder) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_add_24),
                            contentDescription = "Add Folder",
                            tint = Color(0xFFEEEEEE)
                        )
                    }
                    IconButton(onClick = { PlayerHolder.updateLocation(PlayerLocation.SETTING) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_settings_24),
                            contentDescription = "setting",
                            tint = Color(0xFFEEEEEE)
                        )
                    }
                }
                PlayerLocation.SETTING -> {
                    IconButton(onClick = {
                        PlayerHolder.goBack()
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_back),
                            contentDescription = "back",
                            tint = Color(0xFFEEEEEE)
                        )
                    }
                    Text(
                        "Settings",
                        fontSize = 24.sp,
                        color = Color(0xFFEEEEEE),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp) // 項目間距
        ) {
            when(uiState.location) {
                PlayerLocation.FOLDER  -> {
                    if(selectedFolder == null)return@LazyColumn
                    val medias = selectedFolder.medias
                    val selectedMedia = uiState.currentMedia
                    itemsIndexed(selectedFolder.medias) { index, media ->
                        val progress = mediaProgressMap[media.uri.toString()]
                        val p =
                            progress?.let { if (it.duration > 0) it.current / it.duration.toFloat() else 0f }
                                ?: 0f
                        val isFinished = p > 0.9f
                        val currentText = progress?.let { formatMillis(it.current) } ?: "00:00"
                        val durationText = progress?.let { formatMillis(it.duration) } ?: "00:00"
                        val isSelected = selectedMedia?.uri == media.uri
                        val backgroundColor = when {
                            isSelected && isFinished -> Color(0x1FFFFFFF) // 選中且完成，半透明
                            isSelected -> Color(0x43FFFFFF)           // 選中但未完成，亮一點
                            else -> Color.Transparent
                        }
                        val textColor = if (isFinished) Color(0x88EEEEEE) else Color(0xFFEEEEEE)
                        val interactionSource = remember { MutableInteractionSource() }
                        Card(
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = ripple(
                                        color = Color.White,
                                        bounded = true
                                    )
                                ) { PlayerHolder.selectMedia(media, context, selectedFolder) },
                            elevation = CardDefaults.cardElevation(0.dp),
                            colors = CardDefaults.cardColors(containerColor = backgroundColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {

                                Column {
                                    Text(
                                        text = media.fileName,
                                        fontSize = 18.sp,
                                        color = textColor,
                                        maxLines = if (isDetailsVisible) 2 else 1,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 24.sp
                                    )
                                    if (isDetailsVisible) {
                                        Text(
                                            text = "$currentText / $durationText",
                                            fontSize = 14.sp,
                                            color = if (isSelected) Color(0xFFCCCCCC) else Color(
                                                0xFF626262
                                            )
                                        )
                                    }
                                }
                            }
                            if (index != medias.lastIndex) {
                                HorizontalDivider(
                                    Modifier
                                        .padding(horizontal = 16.dp)
                                        .height(0.5.dp),
                                    DividerDefaults.Thickness,
                                    Color(0xFFEEEEEE)
                                )
                            }
                        }
                    }
                }
                PlayerLocation.HOME -> {
                    itemsIndexed(folders) { index, folder ->
                        Card(
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    PlayerHolder.selectFolder(folder)
                                    PlayerHolder.updateLocation(PlayerLocation.FOLDER)
                                    PlayerHolder.loadMediasInFolder(context, folder)
                                },
                            elevation = CardDefaults.cardElevation(0.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = folder.name,
                                    fontSize = 18.sp,
                                    color = Color(0xFFEEEEEE),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 24.sp,
                                    modifier = Modifier.weight(1f) // 占據剩餘空間
                                )

                                if (canDelete&&folder.uri != currentMediaFolder?.uri) {
                                    Spacer(modifier = Modifier.width(8.dp)) // 適當間距
                                    IconButton(
                                        onClick = { PlayerHolder.removeFolder(context, folder) },
                                        modifier = Modifier
                                            .size(24.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.outline_cancel_24),
                                            contentDescription = "X",
                                            tint = Color(0xFFEEEEEE)
                                        )
                                    }
                                }
                            }
                            if (index != folders.lastIndex) {
                                HorizontalDivider(
                                    Modifier
                                        .padding(horizontal = 16.dp)
                                        .height(0.5.dp),
                                    DividerDefaults.Thickness,
                                    Color(0xFFEEEEEE)
                                )
                            }
                        }
                    }
                }
                PlayerLocation.SETTING -> {
                    items(1) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            // AutoPlay Switch
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    "Auto Play",
                                    fontSize = 18.sp,
                                    color = Color(0xFFEEEEEE),
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = settings.autoPlay,
                                    onCheckedChange = {
                                        PlayerHolder.toggleAutoPlay()
                                        PlayerHolder.saveSettings (context)
                                                      },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFEEEEEE),
                                        checkedTrackColor = Color(0xFFBBBBBB),
                                        uncheckedThumbColor = Color(0xFF666666),
                                        uncheckedTrackColor = Color(0xFF444444)
                                    )
                                )
                            }

                            // Background Playing Switch
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    "Background Playing",
                                    fontSize = 18.sp,
                                    color = Color(0xFFEEEEEE),
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = settings.backgroundPlaying,
                                    onCheckedChange = {
                                        PlayerHolder.toggleBackgroundPlaying()
                                        PlayerHolder.saveSettings (context)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFEEEEEE),
                                        checkedTrackColor = Color(0xFFBBBBBB),
                                        uncheckedThumbColor = Color(0xFF666666),
                                        uncheckedTrackColor = Color(0xFF444444)
                                    )
                                )
                            }

                            // Always Restart Switch
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    "Always Restart",
                                    fontSize = 18.sp,
                                    color = Color(0xFFEEEEEE),
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = settings.alwaysRestart,
                                    onCheckedChange = {
                                        PlayerHolder.toggleAlwaysRestart()
                                        PlayerHolder.saveSettings (context)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFEEEEEE),
                                        checkedTrackColor = Color(0xFFBBBBBB),
                                        uncheckedThumbColor = Color(0xFF666666),
                                        uncheckedTrackColor = Color(0xFF444444)
                                    )
                                )
                            }
                        }
                    }
                }

            }
        }
    }
}