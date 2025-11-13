package com.coffeecat.player.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

object DataStoreKeys {
    val MEDIA_PROGRESS_JSON = stringPreferencesKey("media_progress_json")
    val DANMU_SETTINGS_JSON = stringPreferencesKey("danmu_settings_json")
    val SETTINGS_JSON = stringPreferencesKey("settings_json")

    val FOLDER_URIS = stringSetPreferencesKey("saved_folder_uris")
}

// ---------------------------
// 通用的讀寫工具
// ---------------------------
object DataStoreUtils {

    suspend fun <T> savePreference(context: Context, key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { prefs -> prefs[key] = value }
    }

    fun <T> readPreference(context: Context, key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return context.dataStore.data.map { prefs ->
            prefs[key] ?: defaultValue
        }
    }
}
