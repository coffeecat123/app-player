package com.coffeecat.player.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.Html
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import com.coffeecat.player.R
import com.coffeecat.player.data.Danmu
import com.coffeecat.player.data.FolderInfo
import com.coffeecat.player.data.MediaInfo
import com.coffeecat.player.data.PlayerLocation
import com.coffeecat.player.data.PlayerUiState
import com.coffeecat.player.utils.DanmuSettings
import com.coffeecat.player.utils.DataStoreKeys
import com.coffeecat.player.utils.DataStoreUtils
import com.coffeecat.player.utils.MediaProgress
import com.coffeecat.player.utils.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.StringReader

@SuppressLint("AutoboxingStateCreation")
object PlayerHolder {
    private val VIDEO_EXTS = listOf("mp4", "mkv", "webm", "m4v", "mov")
    private val AUDIO_EXTS = listOf("mp3", "m4a", "wav", "ogg", "flac", "aac")
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var exoPlayer: ExoPlayer? = null
    // 進度 Map
    private var _mediaProgressMap = MutableStateFlow<Map<String, MediaProgress>>(emptyMap())
    val mediaProgressMap = _mediaProgressMap.asStateFlow()

    // 播放器 UI 狀態
    private var _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()
    private val _settings = MutableStateFlow(Settings())
    val settings = _settings.asStateFlow()

    var draggingSeekPosMs by mutableStateOf<Long?>(null)
    var lastPlayingState by mutableStateOf(false)

    var duration by mutableStateOf(0L)
    var currentPosition by mutableStateOf(0L)
    var isPlaying by mutableStateOf(false)
    var playbackSpeed by mutableStateOf(1f)

    val locationHistory = mutableListOf<PlayerLocation>()
    val danmuList = mutableListOf<Danmu>()
    var danmuSpeedMultiplier by mutableStateOf(1f)
    var danmuSizeDp by mutableStateOf(24f)
    var danmuOpacity by mutableStateOf(1f)
    var danmuRange by mutableStateOf(1f)
    var danmuLimit by mutableStateOf(200f)
    var clearDanmuTrigger = mutableStateOf(false)
    var onDanmuLoaded: ((List<Danmu>) -> Unit)? = null

    var service: PlayerService? = null
    suspend fun initialize(context: Context) {
        loadFolders(context)
        initializeMediaProgressMap(context)
        loadAllSettings(context)
    }
    fun getExtension(fileName: String): String {
        val dot = fileName.lastIndexOf(".")
        return if (dot != -1) fileName.substring(dot + 1) else ""
    }
    fun saveDanmuSettings(context: Context) {
        val danmuSettings = DanmuSettings(
            speedMultiplier = danmuSpeedMultiplier,
            sizeDp = danmuSizeDp,
            opacity = danmuOpacity,
            range = danmuRange,
            limit = danmuLimit
        )
        serviceScope.launch(Dispatchers.IO) {
            DataStoreUtils.savePreference(
                context,
                DataStoreKeys.DANMU_SETTINGS_JSON,
                Json.encodeToString(danmuSettings)
            )
        }
    }

    fun saveSettings(context: Context) {
        serviceScope.launch(Dispatchers.IO) {
            DataStoreUtils.savePreference(
                context,
                DataStoreKeys.SETTINGS_JSON,
                Json.encodeToString(_settings.value)
            )
        }
    }
    suspend fun loadAllSettings(context: Context) {
        // 讀取彈幕設定
        val danmuJson = DataStoreUtils.readPreference(
            context,
            DataStoreKeys.DANMU_SETTINGS_JSON,
            "{}"
        ).first()
        try {
            val danmuSettings = Json.decodeFromString<DanmuSettings>(danmuJson)
            danmuSpeedMultiplier = danmuSettings.speedMultiplier
            danmuSizeDp = danmuSettings.sizeDp
            danmuOpacity = danmuSettings.opacity
            danmuRange = danmuSettings.range
            danmuLimit = danmuSettings.limit
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 讀取使用者偏好設定
        val settingsJson = DataStoreUtils.readPreference(
            context,
            DataStoreKeys.SETTINGS_JSON,
            "{}"
        ).first()
        try {
            val settings = Json.decodeFromString<Settings>(settingsJson)
            _settings.value = settings
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun saveProgress(context: Context) {
        val media = _uiState.value.currentMedia ?: return
        val dur = duration
        if (dur <= 0L) return
        _mediaProgressMap.update { map ->
            map.toMutableMap().apply {
                put(media.uri.toString(), MediaProgress(currentPosition, dur))
            }
        }
        serviceScope.launch(Dispatchers.IO){
            DataStoreUtils.savePreference(
                context,
                DataStoreKeys.MEDIA_PROGRESS_JSON,
                Json.encodeToString(_mediaProgressMap.value)
            )
        }
    }
    fun loadFolders(context: Context) {
        serviceScope.launch {
            val folderUrisFlow = DataStoreUtils.readPreference(
                context,
                DataStoreKeys.FOLDER_URIS,
                emptySet()
            ).map { set -> set.map { it.toUri() } }


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

            val medias = kotlinx.coroutines.withContext(Dispatchers.IO) {
                val doc = DocumentFile.fromTreeUri(context, folder.uri) ?: return@withContext emptyList<MediaInfo>()

                // 先把所有檔案整理成 Map: 檔名 -> DocumentFile
                val allFiles = doc.listFiles().filter { it.isFile }.associateBy { it.name ?: "" }

                allFiles.values
                    .mapNotNull { file ->
                        val name = file.name ?: return@mapNotNull null
                        val ext = getExtension(name).lowercase()
                        val isVideo = VIDEO_EXTS.contains(ext)
                        val isAudio = AUDIO_EXTS.contains(ext)

                        if (!isVideo && !isAudio) return@mapNotNull null

                        val xmlName = name.replaceAfterLast('.', "xml")
                        val xmlFile = allFiles[xmlName]

                        MediaInfo(
                            title = name.substringBeforeLast(".", name),
                            uri = file.uri,
                            duration = 0L,
                            fileName = name,
                            extension = ext,
                            isVideo = isVideo,
                            danmuUri = xmlFile?.uri
                        )
                    }
                    .sortedBy { it.uri }
            }

            // 回到主線程更新 UI
            _uiState.update { state ->
                val updatedFolders = state.folders.map {
                    if (it.uri == folder.uri) it.copy(medias = medias) else it
                }

                val updatedSelectedFolder =
                    if (state.selectedFolder?.uri == folder.uri)
                        state.selectedFolder.copy(medias = medias)
                    else
                        state.selectedFolder

                state.copy(
                    folders = updatedFolders,
                    selectedFolder = updatedSelectedFolder
                )
            }
        }
    }

    fun addFolder(context: Context, uri: Uri) {
        serviceScope.launch {
            val doc = DocumentFile.fromTreeUri(context, uri) ?: return@launch
            val folderName = doc.name ?: "未知資料夾"

            if (_uiState.value.folders.any { it.name == folderName }) return@launch

            val allFiles = doc.listFiles().filter { it.isFile }.associateBy { it.name ?: "" }

            val medias = allFiles.values
                .mapNotNull { file ->
                    val name = file.name ?: return@mapNotNull null
                    val ext = getExtension(name).lowercase()

                    val isVideo = VIDEO_EXTS.contains(ext)
                    val isAudio = AUDIO_EXTS.contains(ext)
                    if (!isVideo && !isAudio) return@mapNotNull null

                    val xmlName = name.replaceAfterLast('.', "xml")
                    val xmlFile = allFiles[xmlName]

                    MediaInfo(
                        title = name.substringBeforeLast(".", name),
                        uri = file.uri,
                        duration = 0L,
                        fileName = name,
                        extension = ext,
                        isVideo = isVideo,
                        danmuUri = xmlFile?.uri
                    )
                }

            if (medias.isEmpty()) return@launch

            val updatedFolders = _uiState.value.folders + FolderInfo(folderName, uri, medias)
            _uiState.update { it.copy(folders = updatedFolders) }

            // 保存權限
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            // 寫入 DataStore
            val currentUris = DataStoreUtils.readPreference(
                context,
                DataStoreKeys.FOLDER_URIS,
                emptySet()
            ).first().toMutableSet()

            currentUris.add(uri.toString())
            DataStoreUtils.savePreference(context, DataStoreKeys.FOLDER_URIS, currentUris)
        }
    }

    fun selectFolder(folder: FolderInfo) {
        _uiState.update { it.copy(selectedFolder = folder) }
    }

    fun toggleIsMainActivityVisible (a: Boolean? = null) =
        _uiState.update { it.copy(isMainActivityVisible  = a ?: !it.isMainActivityVisible ) }

    fun toggleFullScreen(a: Boolean? = null) =
        _uiState.update { it.copy(isFullScreen = a ?: !it.isFullScreen) }

    fun toggleCanDelete(a: Boolean? = null) =
        _uiState.update { it.copy(canDelete = a ?: !it.canDelete) }

    fun toggleIsDanmuEnabled(a: Boolean? = null) =
        _uiState.update { it.copy(isDanmuEnabled = a ?: !it.isDanmuEnabled) }
    fun toggleIsDanmuSettingVisible(a: Boolean? = null) =
        _uiState.update { it.copy(isDanmuSettingVisible = a ?: !it.isDanmuSettingVisible) }

    fun toggleCanFullScreen(a: Boolean? = null) =
        _uiState.update { it.copy(canFullScreen = a ?: !it.canFullScreen) }

    fun toggleControlsVisible(a: Boolean? = null) =
        _uiState.update { it.copy(controlsVisible = a ?: !it.controlsVisible) }

    fun toggleIsDetailsVisible(a: Boolean? = null) =
        _uiState.update { it.copy(isDetailsVisible = a ?: !it.isDetailsVisible) }
    fun toggleAutoPlay(a: Boolean? = null) =
        _settings.update { it.copy(autoPlay = a ?: !it.autoPlay) }
    fun toggleBackgroundPlaying(a: Boolean? = null) =
        _settings.update { it.copy(backgroundPlaying = a ?: !it.backgroundPlaying) }

    fun changeOrientation(a: String) {
        _uiState.update { it.copy(nowOrientation = a) }
    }
    fun updateLocation(newLocation: PlayerLocation) {
        locationHistory.add(_uiState.value.location)
        _uiState.update { it.copy(location = newLocation,
            canDelete = false) }
    }
    fun goBack() {
        if(locationHistory.isNotEmpty()) {
            val last = locationHistory.removeAt(locationHistory.lastIndex)
            _uiState.update { it.copy(location = last) }
        } else {
            _uiState.update { it.copy(location = PlayerLocation.HOME) }
        }
    }

    fun removeFolder(context: Context, folder: FolderInfo) {
        serviceScope.launch {
            // 更新 UI 狀態
            _uiState.update { state ->
                state.copy(folders = state.folders.filter { it.uri != folder.uri })
            }

            // 從 DataStore 移除
            val currentUris = DataStoreUtils.readPreference(
                context,
                DataStoreKeys.FOLDER_URIS,
                emptySet()
            ).first().toMutableSet()

            currentUris.remove(folder.uri.toString())

            DataStoreUtils.savePreference(
                context,
                DataStoreKeys.FOLDER_URIS,
                currentUris
            )
        }
    }


    suspend fun initializeMediaProgressMap(context: Context) {
        // 使用 DataStoreUtils 讀取 MediaProgress JSON
        val jsonString = DataStoreUtils.readPreference(
            context,
            DataStoreKeys.MEDIA_PROGRESS_JSON,
            "{}"
        ).first() // Flow 轉成單次值

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
    fun selectMedia(media: MediaInfo, context: Context,folder: FolderInfo) {
        if (_uiState.value.currentMedia == media) return
        saveProgress(context)
        _uiState.update { it.copy(
            currentMedia = media,
            currentMediaFolder = folder
        ) }
        duration=0
        clearDanmuTrigger.value=true
        play(context, media)
        serviceScope.launch(Dispatchers.IO) {
            loadDanmusForMedia(context, media)
        }
    }

    @OptIn(UnstableApi::class)
    fun loadDanmusForMedia(context: Context, media: MediaInfo) {
        val danmuUri = media.danmuUri ?: run {
            Log.d("Danmu", "該影片沒有設定彈幕 URI")
            onDanmuLoaded?.invoke(listOf())
            return
        }

        serviceScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(danmuUri)?.use { input ->
                    val xmlString = input.bufferedReader().readText()
                    Log.d("Danmu", "成功讀取彈幕檔案: $danmuUri, 大小=${xmlString.length} bytes")

                    val list = parseDanmus(xmlString)

                    danmuList.clear()
                    danmuList.addAll(list)
                    danmuList.sortBy { it.startTime }
                    onDanmuLoaded?.invoke(list)
                    Log.d("Danmu", "成功載入 ${list.size} 條彈幕")
                }
            } catch (e: Exception) {
                Log.e("Danmu", "讀取彈幕失敗: ${e.message}")
            }
        }
    }

    private fun parseDanmus(xmlString: String): List<Danmu> {
        val list = mutableListOf<Danmu>()
        val parser = org.xmlpull.v1.XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xmlString))

        var eventType = parser.eventType
        var idCounter = 0

        while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "d") {
                val pAttr = parser.getAttributeValue(null, "p") ?: ""
                val p = pAttr.split(",")
                if (p.size >= 4) {
                    val startTime = (p[0].toFloatOrNull() ?: 0f) * 1000
                    val size = p[2].toFloatOrNull() ?: 30f
                    val rawColorInt = p[3].toIntOrNull() ?: 0xFFFFFF

                    // 直接算好 ARGB Int
                    val argbColorInt = if (rawColorInt == 0x00000F) 0xFFFFFFFF.toInt() else rawColorInt or 0xFF000000.toInt()

                    val rawText = parser.nextText() ?: ""

                    val text = Html.fromHtml(rawText, Html.FROM_HTML_MODE_LEGACY).toString()
                    list.add(
                        Danmu(
                            id = idCounter++,
                            text = text,
                            color = argbColorInt,  // 新增一個欄位
                            startTime = startTime.toLong(),
                            x = 0f,
                            y = 0f,
                            sizeDp = size
                        )
                    )
                }
            }
            eventType = parser.next()
        }

        return list
    }
    @OptIn(UnstableApi::class)
    fun play(context: Context, media: MediaInfo) {
        val uri=media.uri
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED&&_settings.value.autoPlay) {
                            val current = _uiState.value.currentMedia ?: return
                            val folder = _uiState.value.currentMediaFolder ?: return
                            val medias = folder.medias
                            val currentIndex = medias.indexOf(current)
                            val nextIndex = (currentIndex + 1) % medias.size
                            val nextMedia = medias[nextIndex]

                            selectMedia(nextMedia,context,folder)
                        }
                    }
                })
            }
        }
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.cft)
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val coverByteArray = byteArrayOutputStream.toByteArray()

        var savedPos = _mediaProgressMap.value[uri.toString()]?.current ?: 0L
        val saveDuration = _mediaProgressMap.value[uri.toString()]?.duration ?: 0L
        if(saveDuration!=0L&&saveDuration-100<savedPos) {
            savedPos = 0
        }
        val selectedFolder=_uiState.value.currentMediaFolder
        exoPlayer?.apply {
            setMediaItem(
                MediaItem.fromUri(uri).buildUpon()
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(media.title)
                            .setArtist(selectedFolder?.name ?: "未知動畫")
                            .setArtworkData(coverByteArray, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                            .build()

                    )
                    .build())
            prepare()
            seekTo(savedPos)
            play()
            playbackParameters = PlaybackParameters(1f)
        }
        playbackSpeed = 1f
        val intent = Intent(context, PlayerService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun clear() {
        exoPlayer?.release()
        exoPlayer = null
        currentPosition = 0
        duration = 0
        isPlaying = false
        draggingSeekPosMs = null
        _uiState.value = PlayerUiState()
    }
}