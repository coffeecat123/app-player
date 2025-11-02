package com.coffeecat.animeplayer.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.coffeecat.animeplayer.data.FolderInfo
import com.coffeecat.animeplayer.data.MediaInfo
import com.coffeecat.animeplayer.data.PlayerUiState
import com.coffeecat.animeplayer.utils.FOLDER_URIS
import com.coffeecat.animeplayer.utils.dataStore
import com.coffeecat.animeplayer.viewmodel.MediaProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.collections.map
import kotlin.collections.putAll
import kotlin.compareTo
import kotlin.plus
import kotlin.text.get

object PlayerHolder {
    private val VIDEO_EXTS = listOf("mp4", "mkv", "webm", "m4v", "mov")
    private val AUDIO_EXTS = listOf("mp3", "m4a", "wav", "ogg", "flac", "aac")
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val MEDIA_PROGRESS_JSON = stringPreferencesKey("media_progress_json")
    var exoPlayer: ExoPlayer? = null
    // 進度 Map
    private var _mediaProgressMap = MutableStateFlow<Map<String, MediaProgress>>(emptyMap())
    val mediaProgressMap = _mediaProgressMap.asStateFlow()

    // 播放器 UI 狀態
    private var _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    var draggingSeekPosMs by mutableStateOf<Long?>(null)
    var lastPlayingState by mutableStateOf(false)

    var duration by mutableStateOf(0L)
    var currentPosition by mutableStateOf(0L)

    var playbackSpeed by mutableStateOf(1f)
    var isExoplayerReady=false

    fun getExtension(fileName: String): String {
        val dot = fileName.lastIndexOf(".")
        return if (dot != -1) fileName.substring(dot + 1) else ""
    }

    fun saveProgress(context: Context) {
        val media = _uiState.value.currentMedia ?: return
        val dur = duration
        if (dur <= 0L || dur == C.TIME_UNSET) return
        _mediaProgressMap.update { map ->
            map.toMutableMap().apply {
                put(media.uri.toString(), MediaProgress(currentPosition, dur))
            }
        }
        serviceScope.launch(Dispatchers.IO){
            context.dataStore.edit { prefs ->
                prefs[MEDIA_PROGRESS_JSON] = Json.encodeToString(_mediaProgressMap.value)
            }
        }
    }
    fun loadFolders(context: Context) {
        serviceScope.launch {
            val folderUrisFlow: Flow<List<Uri>> = context.dataStore.data
                .map { prefs -> prefs[FOLDER_URIS]?.map { it.toUri() } ?: emptyList() }

            folderUrisFlow.collect { uris ->
                val oldFolders = _uiState.value.folders

                val folderList = uris.mapNotNull { uri ->
                    val doc = DocumentFile.fromTreeUri(context, uri) ?: return@mapNotNull null
                    val folderName = doc.name ?: return@mapNotNull null

                    // 找舊資料，保留 videos
                    val existing = oldFolders.find { it.uri == uri }

                    FolderInfo(
                        name = folderName,
                        uri = uri,
                        medias = existing?.medias ?: emptyList()
                    )
                }

                _uiState.update { it.copy(folders = folderList) }
            }
        }
    }
    fun loadMediasInFolder(context: Context, folder: FolderInfo) {
        serviceScope.launch {
            if (folder.medias.isNotEmpty()) return@launch

            val medias = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val doc = DocumentFile.fromTreeUri(context, folder.uri) ?: return@withContext emptyList<MediaInfo>()

                doc.listFiles()
                    .filter { it.isFile }
                    .mapNotNull { file ->
                        val name = file.name ?: return@mapNotNull null
                        val ext = getExtension(name).lowercase()

                        val isVideo = VIDEO_EXTS.contains(ext)
                        val isAudio = AUDIO_EXTS.contains(ext)

                        if (!isVideo && !isAudio) return@mapNotNull null

                        MediaInfo(
                            title = name.substringBeforeLast(".", name),
                            uri = file.uri,
                            duration = 0L,
                            fileName = name,
                            extension = ext,
                            isVideo = isVideo
                        )
                    }
            }

            // 回到主線程更新 UI
            _uiState.update { state ->
                val updatedFolders = state.folders.map {
                    if (it.uri == folder.uri) it.copy(medias = medias) else it
                }
                state.copy(folders = updatedFolders)
            }
        }
    }
    fun addFolder(context: Context, uri: Uri) {
        serviceScope.launch {
            val doc = DocumentFile.fromTreeUri(context, uri) ?: return@launch
            val folderName = doc.name ?: "未知資料夾"

            if (_uiState.value.folders.any { it.name == folderName }) return@launch

            val medias = doc.listFiles()
                .filter { it.isFile }
                .mapNotNull { file ->
                    val name = file.name ?: return@mapNotNull null
                    val ext = getExtension(name).lowercase()

                    val isVideo = VIDEO_EXTS.contains(ext)
                    val isAudio = AUDIO_EXTS.contains(ext)

                    if (!isVideo && !isAudio) return@mapNotNull null

                    MediaInfo(
                        title = name.substringBeforeLast(".", name),
                        uri = file.uri,
                        duration = 0L,
                        fileName = name,
                        extension = ext,
                        isVideo = isVideo
                    )
                }

            if (medias.isEmpty()) return@launch

            val updatedFolders = _uiState.value.folders + FolderInfo(folderName, uri, medias)
            _uiState.update { it.copy(folders = updatedFolders) }

            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            context.dataStore.edit { prefs ->
                val currentUris = prefs[FOLDER_URIS]?.toMutableSet() ?: mutableSetOf()
                currentUris.add(uri.toString())
                prefs[FOLDER_URIS] = currentUris
            }
        }
    }

    fun selectMedia(media: MediaInfo, context: Context) {
        if (_uiState.value.currentMedia == media) return
        saveProgress(context)
        _uiState.update { it.copy(currentMedia = media) }
        duration=0
        play(context, media.uri)
    }

    fun selectFolder(uri: Uri?) {
        _uiState.update { it.copy(selectedFolderUri = uri) }
    }

    fun toggleFullScreen(a: Boolean? = null) =
        _uiState.update { it.copy(isFullScreen = a ?: !it.isFullScreen) }

    fun toggleCanDelete(a: Boolean? = null) =
        _uiState.update { it.copy(canDelete = a ?: !it.canDelete) }

    fun toggleCanFullScreen(a: Boolean? = null) =
        _uiState.update { it.copy(canFullScreen = a ?: !it.canFullScreen) }

    fun toggleControlsVisible(a: Boolean? = null) =
        _uiState.update { it.copy(controlsVisible = a ?: !it.controlsVisible) }

    fun toggleIsDetailsVisible(a: Boolean? = null) =
        _uiState.update { it.copy(isDetailsVisible = a ?: !it.isDetailsVisible) }

    fun changeOrientation(a: String) {
        _uiState.update { it.copy(nowOrientation = a) }
    }

    fun removeFolder(context: Context, folder: FolderInfo) {
        serviceScope.launch {
            // 更新 UI 狀態
            _uiState.update { state ->
                state.copy(folders = state.folders.filter { it.uri != folder.uri })
            }

            // 從 DataStore 移除
            context.dataStore.edit { prefs ->
                val currentUris = prefs[FOLDER_URIS]?.toMutableSet() ?: mutableSetOf()
                currentUris.remove(folder.uri.toString())
                prefs[FOLDER_URIS] = currentUris
            }
        }
    }

    suspend fun initializeMediaProgressMap(context: Context) {
        val prefs = context.dataStore.data.first()
        val jsonString = prefs[MEDIA_PROGRESS_JSON] ?: "{}"
        val map: Map<String, MediaProgress> = Json.decodeFromString(jsonString)

        _mediaProgressMap.update { current ->
            current.toMutableMap().apply { putAll(map) }
        }
    }
    fun updatePlaybackSpeed(speed: Float) {
        playbackSpeed = speed
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
    }
    fun onFullScreenButtonClicked() {
        if (_uiState.value.nowOrientation == "LANDSCAPE") {
            _uiState.update {
                it.copy(
                    nowOrientation = "PORTRAIT",
                    canFullScreen = false,
                    isFullScreen = false
                )
            }
        } else {
            _uiState.update { it.copy(nowOrientation = "LANDSCAPE", isFullScreen = true) }
        }
    }

    fun play(context: Context, uri: Uri) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build()
        }
        val savedPos = _mediaProgressMap.value[uri.toString()]?.current ?: 0L

        exoPlayer?.apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            seekTo(savedPos)
            play()
            playbackParameters = PlaybackParameters(1f)
        }
        playbackSpeed = 1f
        val intent = Intent(context, PlayerService::class.java).apply {
            putExtra("uri", uri.toString())
        }
        ContextCompat.startForegroundService(context, intent)
    }
}