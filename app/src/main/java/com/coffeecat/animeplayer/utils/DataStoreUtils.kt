package com.coffeecat.animeplayer.utils

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlin.collections.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "folder_prefs")
val FOLDER_URIS = stringSetPreferencesKey("saved_folder_uris")
