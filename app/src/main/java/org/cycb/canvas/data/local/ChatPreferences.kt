package org.cycb.canvas.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.cycb.canvas.data.model.ChatFolder

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chat_prefs")

class ChatPreferences(private val context: Context) {

    companion object {
        private val PINNED_CHATS_KEY = stringSetPreferencesKey("pinned_chats")
        private val HIDDEN_CHATS_KEY = stringSetPreferencesKey("hidden_chats")
        private val CHAT_FOLDERS_KEY = stringPreferencesKey("chat_folders")
    }

    val folders: Flow<List<ChatFolder>> = context.dataStore.data
        .map { preferences ->
            val foldersJson = preferences[CHAT_FOLDERS_KEY] ?: "[]"
            try {
                Json.decodeFromString<List<ChatFolder>>(foldersJson)
            } catch (e: Exception) {
                emptyList()
            }
        }

    val pinnedChatIds: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[PINNED_CHATS_KEY] ?: emptySet()
        }

    val hiddenChatIds: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[HIDDEN_CHATS_KEY] ?: emptySet()
        }

    suspend fun togglePinChat(chatId: String) {
        context.dataStore.edit { preferences ->
            val currentPinned = preferences[PINNED_CHATS_KEY] ?: emptySet()
            if (currentPinned.contains(chatId)) {
                preferences[PINNED_CHATS_KEY] = currentPinned - chatId
            } else {
                preferences[PINNED_CHATS_KEY] = currentPinned + chatId
            }
        }
    }

    suspend fun hideChat(chatId: String) {
        context.dataStore.edit { preferences ->
            val currentHidden = preferences[HIDDEN_CHATS_KEY] ?: emptySet()
            preferences[HIDDEN_CHATS_KEY] = currentHidden + chatId
        }
    }

    suspend fun unhideChat(chatId: String) {
        context.dataStore.edit { preferences ->
            val currentHidden = preferences[HIDDEN_CHATS_KEY] ?: emptySet()
            if (currentHidden.contains(chatId)) {
                preferences[HIDDEN_CHATS_KEY] = currentHidden - chatId
            }
        }
    }

    suspend fun saveFolders(folders: List<ChatFolder>) {
        context.dataStore.edit { preferences ->
            preferences[CHAT_FOLDERS_KEY] = Json.encodeToString(folders)
        }
    }

    suspend fun addChatToFolder(folderId: String, chatId: String) {
        context.dataStore.edit { preferences ->
            val foldersJson = preferences[CHAT_FOLDERS_KEY] ?: "[]"
            val currentFolders = try {
                Json.decodeFromString<List<ChatFolder>>(foldersJson)
            } catch (e: Exception) {
                emptyList()
            }

            val updatedFolders = currentFolders.map { folder ->
                if (folder.id == folderId) {
                    folder.copy(chatIds = folder.chatIds + chatId)
                } else {
                    folder
                }
            }
            preferences[CHAT_FOLDERS_KEY] = Json.encodeToString(updatedFolders)
        }
    }
}
