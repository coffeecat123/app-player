package com.coffeecat.animeplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeecat.animeplayer.ui.screen.MainScreen
import com.coffeecat.animeplayer.viewmodel.MainViewModel
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.coffeecat.animeplayer.service.PlayerHolder
import com.coffeecat.animeplayer.service.PlayerService

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        //startService(Intent(this, PlayerService::class.java))

        setContent {
            MainScreen(viewModel = mainViewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        updateSystemUiForOrientation()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        updateSystemUiForOrientation()
    }

    @OptIn(UnstableApi::class)
    override fun onDestroy() {
        super.onDestroy()
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
