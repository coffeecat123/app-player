package com.coffeecat.player.ui.screen

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.OrientationEventListener
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.coffeecat.player.service.PlayerHolder
import com.coffeecat.player.ui.component.MainContent
import com.coffeecat.player.ui.component.MediaPlayer

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen() {

    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by PlayerHolder.uiState.collectAsState()
    val currentMedia = uiState.currentMedia
    val isFullScreen = uiState.isFullScreen
    val nowOrientation = uiState.nowOrientation
    val currentMediaState by rememberUpdatedState(currentMedia)

    LaunchedEffect(Unit) {
        PlayerHolder.initialize(context)
    }

    // Launcher 必須在 Composable 中
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            PlayerHolder.addFolder(context, it)
        }
    }
    if(nowOrientation=="LANDSCAPE"){
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }else{
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    DisposableEffect(Unit) {
        val listener = object : OrientationEventListener(context) {
            @OptIn(UnstableApi::class)
            override fun onOrientationChanged(angle: Int) {
                Log.d("MediaPlayer", currentMediaState?.title.toString())
                if(currentMediaState==null) return
                // angle: 0~359 度
                val t=25
                if (angle in 45+t..134-t || angle in 225+t..314-t) {
                    if (PlayerHolder.uiState.value.canFullScreen) {
                        PlayerHolder.changeOrientation("LANDSCAPE")
                        PlayerHolder.toggleFullScreen(true)
                    }
                } else {
                    if (!PlayerHolder.uiState.value.isFullScreen) {
                        PlayerHolder.toggleCanFullScreen(true)
                        PlayerHolder.changeOrientation("PORTRAIT")
                    }
                }
            }
        }
        listener.enable()

        onDispose { listener.disable() }
    }
    Column(modifier = Modifier.fillMaxSize()
        .background(Color(0xFF000000))
        .then(
            if (isFullScreen)
                Modifier
            else
                Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
        ),
    ) {

        Box(
            modifier =if (nowOrientation=="LANDSCAPE") {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1920f / 1080f)
            }
        ) {
            if(nowOrientation=="LANDSCAPE"){
                BackHandler {
                    PlayerHolder.changeOrientation("PORTRAIT")
                    PlayerHolder.toggleCanFullScreen(false)
                    PlayerHolder.toggleFullScreen(false)
                }
            }
            if (currentMedia!= null) {
                MediaPlayer(media = currentMedia
                )
            } else {
                Text(
                    text = "select a media",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        if(nowOrientation!="LANDSCAPE") {
            MainContent(
                context,
                onAddFolder = {
                    PlayerHolder.toggleCanDelete(false)
                    launcher.launch(null)
                }
            )
        }
    }
}