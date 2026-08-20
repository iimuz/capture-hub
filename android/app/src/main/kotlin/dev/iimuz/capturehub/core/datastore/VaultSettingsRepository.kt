package dev.iimuz.capturehub.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val DEFAULT_FILE_NAME_PATTERN = "YYYY-MM-DD.md"

data class VaultSettings(
    val vaultUri: String,
    val fileNamePattern: String,
)

class VaultSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<VaultSettings?> =
        dataStore.data.map { preferences ->
            val uri = preferences[KEY_VAULT_URI] ?: return@map null
            VaultSettings(
                vaultUri = uri,
                // パターン構文が moment 風トークンへ変更される前に保存された値は
                // 新パーサーで無効になり得るため、無効な保存値は既定値へフォールバックする。
                fileNamePattern =
                    preferences[KEY_FILE_NAME_PATTERN]?.takeIf { isValidFileNamePattern(it) }
                        ?: DEFAULT_FILE_NAME_PATTERN,
            )
        }

    suspend fun saveVaultUri(uri: String) {
        dataStore.edit { it[KEY_VAULT_URI] = uri }
    }

    suspend fun saveFileNamePattern(pattern: String) {
        dataStore.edit { it[KEY_FILE_NAME_PATTERN] = pattern }
    }

    private companion object {
        val KEY_VAULT_URI = stringPreferencesKey("vault_uri")
        val KEY_FILE_NAME_PATTERN = stringPreferencesKey("file_name_pattern")
    }
}

fun hasPersistedWritePermission(
    context: Context,
    uriString: String,
): Boolean =
    context.contentResolver.persistedUriPermissions.any {
        it.uri.toString() == uriString && it.isReadPermission && it.isWritePermission
    }
