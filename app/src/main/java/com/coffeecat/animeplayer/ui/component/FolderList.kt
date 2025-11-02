package com.coffeecat.animeplayer.ui.component

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.coffeecat.animeplayer.R
import com.coffeecat.animeplayer.service.PlayerHolder
import com.coffeecat.animeplayer.viewmodel.MainViewModel


@OptIn(UnstableApi::class)
@Composable
fun FolderList(
    viewModel: MainViewModel,
    context: Context,
    onAddFolder: () -> Unit
){

    val uiState by PlayerHolder.uiState.collectAsState()
    val folders = uiState.folders
    val canDelete = uiState.canDelete
    val isDetailsVisible = uiState.isDetailsVisible
    val selectedFolderUri = uiState.selectedFolderUri
    val selectedFolder = folders.find { it.uri == selectedFolderUri }
    val mediaProgressMap by PlayerHolder.mediaProgressMap.collectAsState(initial = emptyMap())

    val formatMillis: (Long) -> String = { millis ->
        val seconds = millis / 1000
        "%02d:%02d".format(seconds / 60, seconds % 60)
    }


    BackHandler {
        PlayerHolder.selectFolder(null)
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
            if(selectedFolder != null){
                IconButton(onClick = { PlayerHolder.selectFolder(null)}) {
                    Icon(painter = painterResource(id = R.drawable.arrow_back),
                        contentDescription = null,
                        tint =Color(0xFFEEEEEE))
                }
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
                    Icon(painter = painterResource(id = R.drawable.outline_density_small_24),
                        contentDescription = null,
                        tint =Color(0xFFEEEEEE))
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {PlayerHolder.toggleCanDelete()}) {
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
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp) // 項目間距
        ) {
            if(selectedFolder != null){
                val  medias=(selectedFolder.medias).sortedBy { it.uri }
                itemsIndexed(medias) { index, media ->
                    Card(
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { PlayerHolder.selectMedia(media, context) },
                        elevation = CardDefaults.cardElevation(0.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            val progress = mediaProgressMap[media.uri.toString()]
                        Log.d("progress","${media.uri},${progress}")
                            val p = progress?.let { it.current / it.duration.toFloat() } ?: 0f
                            val currentText = progress?.let { formatMillis(it.current) } ?: "00:00"
                            val durationText = progress?.let { formatMillis(it.duration) } ?: "00:00"

                            Column {
                                Text(
                                    text = media.fileName,
                                    fontSize = 18.sp,
                                    color = if (p > 0.9f) Color(0x88EEEEEE) else Color(0xFFEEEEEE),
                                    maxLines = if (isDetailsVisible) 2 else 1,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 24.sp
                                )
                                if(isDetailsVisible) {
                                    Text(
                                        text = "$currentText / $durationText",
                                        fontSize = 14.sp,
                                        color = Color(0xFF626262)
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
            } else {
                itemsIndexed(folders) {index, folder ->
                    Card(
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                PlayerHolder.selectFolder(folder.uri)
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

                            if(canDelete){
                                Spacer(modifier = Modifier.width(8.dp)) // 適當間距
                                IconButton(onClick = { PlayerHolder.removeFolder(context, folder) },
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
        }
    }
}