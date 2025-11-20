package com.coffeecat.player

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.runtime.LaunchedEffect
import com.coffeecat.player.ui.screen.MainScreen
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.util.UnstableApi
import com.coffeecat.player.service.PlayerHolder
import com.coffeecat.player.service.PlayerService

class MainActivity : ComponentActivity() {

    @OptIn(UnstableApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MainScreen()
            LaunchedEffect(Unit) {
                PlayerHolder.uiState.collect { state ->
                    if (state.isPlaying&&state.isMainActivityVisible) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
        }
        PlayerHolder.toggleIsMainActivityVisible(true)
    }

    override fun onResume() {
        super.onResume()
        PlayerHolder.toggleIsMainActivityVisible(true)
        updateSystemUiForOrientation()
        PlayerHolder.currentSurface?.let { PlayerHolder.exoPlayer?.setVideoSurface(it) }
    }
    override fun onPause() {
        super.onPause()
        PlayerHolder.toggleIsMainActivityVisible(false)
        PlayerHolder.exoPlayer?.setVideoSurface(null)
    }
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        updateSystemUiForOrientation()
    }

    @OptIn(UnstableApi::class)
    override fun onDestroy() {
        super.onDestroy()
        PlayerHolder.toggleIsMainActivityVisible(false)
        if(!PlayerHolder.settings.value.backgroundPlaying){
            val intent = Intent(this, PlayerService::class.java)
            stopService(intent)
            PlayerHolder.service?.stopAll()
        }
    }
}

fun ComponentActivity.updateSystemUiForOrientation() {
    val isLandscape = resources.configuration.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val controller = WindowInsetsControllerCompat(window, window.decorView)

    if (isLandscape) {
        controller.hide(
            WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.navigationBars()
        )
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    } else {
        controller.show(
            WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.navigationBars()
        )
    }
}
