package dev.iimuz.capturehub.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class VaultSettingsRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun kotlinx.coroutines.test.TestScope.repository(): VaultSettingsRepository {
        val dataStore =
            PreferenceDataStoreFactory.create(scope = backgroundScope) {
                tmp.newFolder().resolve("settings.preferences_pb")
            }
        return VaultSettingsRepository(dataStore)
    }

    @Test
    fun `settings is null until vault uri is saved`() =
        runTest {
            assertNull(repository().settings.first())
        }

    @Test
    fun `saved vault uri is returned with default pattern`() =
        runTest {
            val repository = repository()
            repository.saveVaultUri("content://vault")
            val settings = repository.settings.first()
            assertEquals("content://vault", settings?.vaultUri)
            assertEquals(DEFAULT_FILE_NAME_PATTERN, settings?.fileNamePattern)
        }

    @Test
    fun `saved file name pattern overrides default`() =
        runTest {
            val repository = repository()
            repository.saveVaultUri("content://vault")
            repository.saveFileNamePattern("yyyyMMdd.md")
            assertEquals("yyyyMMdd.md", repository.settings.first()?.fileNamePattern)
        }
}
