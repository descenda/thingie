package org.cycb.canvas.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Serializable
data class StoredAccount(
    val userId: String,
    val token: String,
    val username: String,
    val displayName: String,
    val profilePicture: String? = null
)

class TokenManager(private val context: Context) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token") // Legacy
        private val USER_ID_KEY = stringPreferencesKey("user_id") // Legacy
        private val ACCOUNTS_KEY = stringPreferencesKey("accounts_json")
        private val ACTIVE_USER_ID_KEY = stringPreferencesKey("active_user_id")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val accounts: Flow<List<StoredAccount>> = context.dataStore.data.map { preferences ->
        val accountsJson = preferences[ACCOUNTS_KEY]
        if (accountsJson != null) {
            try {
                json.decodeFromString<List<StoredAccount>>(accountsJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            // Migration: if we have a legacy token, create an account from it
            val legacyToken = preferences[TOKEN_KEY]
            val legacyUserId = preferences[USER_ID_KEY]
            if (legacyToken != null && legacyUserId != null) {
                listOf(StoredAccount(legacyUserId, legacyToken, "User", "User"))
            } else {
                emptyList()
            }
        }
    }

    val activeUserId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACTIVE_USER_ID_KEY] ?: preferences[USER_ID_KEY]
    }

    val activeAccount: Flow<StoredAccount?> = context.dataStore.data.map { preferences ->
        val accountsJson = preferences[ACCOUNTS_KEY]
        val activeId = preferences[ACTIVE_USER_ID_KEY] ?: preferences[USER_ID_KEY]
        
        if (accountsJson != null && activeId != null) {
            val accounts = try {
                json.decodeFromString<List<StoredAccount>>(accountsJson)
            } catch (e: Exception) {
                emptyList<StoredAccount>()
            }
            accounts.find { it.userId == activeId }
        } else if (preferences[TOKEN_KEY] != null && preferences[USER_ID_KEY] != null) {
            StoredAccount(preferences[USER_ID_KEY]!!, preferences[TOKEN_KEY]!!, "User", "User")
        } else {
            null
        }
    }

    val token: Flow<String?> = activeAccount.map { it?.token }

    suspend fun saveAccount(account: StoredAccount) {
        context.dataStore.edit { preferences ->
            val accountsJson = preferences[ACCOUNTS_KEY]
            val currentAccounts = if (accountsJson != null) {
                try {
                    json.decodeFromString<List<StoredAccount>>(accountsJson).toMutableList()
                } catch (e: Exception) {
                    mutableListOf()
                }
            } else {
                mutableListOf()
            }

            // Remove if already exists (to update)
            currentAccounts.removeAll { it.userId == account.userId }
            currentAccounts.add(account)

            preferences[ACCOUNTS_KEY] = json.encodeToString(currentAccounts)
            preferences[ACTIVE_USER_ID_KEY] = account.userId
            
            // Legacy support
            preferences[TOKEN_KEY] = account.token
            preferences[USER_ID_KEY] = account.userId
        }
    }

    suspend fun switchAccount(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[ACTIVE_USER_ID_KEY] = userId
            
            // Update legacy keys for backward compatibility
            val accountsJson = preferences[ACCOUNTS_KEY]
            if (accountsJson != null) {
                val accounts = json.decodeFromString<List<StoredAccount>>(accountsJson)
                accounts.find { it.userId == userId }?.let {
                    preferences[TOKEN_KEY] = it.token
                    preferences[USER_ID_KEY] = it.userId
                }
            }
        }
    }

    suspend fun removeAccount(userId: String) {
        context.dataStore.edit { preferences ->
            val accountsJson = preferences[ACCOUNTS_KEY]
            if (accountsJson != null) {
                val currentAccounts = json.decodeFromString<List<StoredAccount>>(accountsJson).toMutableList()
                currentAccounts.removeAll { it.userId == userId }
                preferences[ACCOUNTS_KEY] = json.encodeToString(currentAccounts)
                
                if (preferences[ACTIVE_USER_ID_KEY] == userId) {
                    val nextAccount = currentAccounts.firstOrNull()
                    if (nextAccount != null) {
                        preferences[ACTIVE_USER_ID_KEY] = nextAccount.userId
                        preferences[TOKEN_KEY] = nextAccount.token
                        preferences[USER_ID_KEY] = nextAccount.userId
                    } else {
                        preferences.remove(ACTIVE_USER_ID_KEY)
                        preferences.remove(TOKEN_KEY)
                        preferences.remove(USER_ID_KEY)
                    }
                }
            }
        }
    }

    suspend fun saveToken(token: String, userId: String) {
        // Fallback for old calls, but we should prefer saveAccount
        saveAccount(StoredAccount(userId, token, "User", "User"))
    }

    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
