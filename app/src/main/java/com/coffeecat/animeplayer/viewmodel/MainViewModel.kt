package com.coffeecat.animeplayer.viewmodel

import android.R
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.coffeecat.animeplayer.data.FolderInfo
import com.coffeecat.animeplayer.data.MediaInfo
import com.coffeecat.animeplayer.data.PlayerUiState
import com.coffeecat.animeplayer.service.PlayerHolder
import com.coffeecat.animeplayer.service.PlayerService
import com.coffeecat.animeplayer.ui.component.PlayerSlider
import com.coffeecat.animeplayer.utils.FOLDER_URIS
import com.coffeecat.animeplayer.utils.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs

class MainViewModel : ViewModel() {
}
