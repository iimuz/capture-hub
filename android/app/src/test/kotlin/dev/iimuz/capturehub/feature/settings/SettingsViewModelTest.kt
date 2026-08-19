package dev.iimuz.capturehub.feature.settings

import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.iimuz.capturehub.MainDispatcherRule
import dev.iimuz.capturehub.core.datastore.VaultSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tmp = org.junit.rules.TemporaryFolder()

    private val takenPermissions = mutableListOf<Uri>()
    private var vaultConfiguredCalls = 0

    private fun TestScope.viewModel(hasPermission: Boolean = true): SettingsViewModel {
        val dataStore =
            PreferenceDataStoreFactory.create(scope = backgroundScope) {
                tmp.newFolder().resolve("settings.preferences_pb")
            }
        return SettingsViewModel(
            repository = VaultSettingsRepository(dataStore),
            takePersistablePermission = { takenPermissions += it },
            hasPermission = { hasPermission },
            onVaultConfigured = { vaultConfiguredCalls += 1 },
        )
    }

    @Test
    fun `onFolderPicked takes permission and saves uri`() =
        runTest {
            val vm = viewModel()
            vm.onFolderPicked(Uri.parse("content://vault"))
            advanceUntilIdle()
            assertEquals(listOf(Uri.parse("content://vault")), takenPermissions)
            assertEquals(1, vaultConfiguredCalls)
            val state = vm.uiState.first { it.vaultUri != null }
            assertEquals("content://vault", state.vaultUri)
            assertFalse(state.permissionLost)
        }

    @Test
    fun `permissionLost is true when persisted permission is missing`() =
        runTest {
            val vm = viewModel(hasPermission = false)
            vm.onFolderPicked(Uri.parse("content://vault"))
            val state = vm.uiState.first { it.vaultUri != null }
            assertTrue(state.permissionLost)
        }

    @Test
    fun `invalid pattern is flagged and not saved`() =
        runTest {
            val vm = viewModel()
            vm.onFolderPicked(Uri.parse("content://vault"))
            vm.onFileNamePatternChange("[bad.md")
            val state = vm.uiState.first { it.patternInvalid }
            assertTrue(state.patternInvalid)
            assertEquals("yyyy-MM-dd.md", state.fileNamePattern)
        }
}
