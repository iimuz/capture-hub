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
    private var writeSettingsChangedCalls = 0
    private lateinit var repository: VaultSettingsRepository

    private fun TestScope.viewModel(hasPermission: Boolean = true): SettingsViewModel {
        val dataStore =
            PreferenceDataStoreFactory.create(scope = backgroundScope) {
                tmp.newFolder().resolve("settings.preferences_pb")
            }
        repository = VaultSettingsRepository(dataStore)
        return SettingsViewModel(
            repository = repository,
            takePersistablePermission = { takenPermissions += it },
            hasPermission = { hasPermission },
            onWriteSettingsChanged = { writeSettingsChangedCalls += 1 },
        )
    }

    @Test
    fun `onFolderPicked takes permission and saves uri`() =
        runTest {
            val vm = viewModel()
            vm.onFolderPicked(Uri.parse("content://vault"))
            advanceUntilIdle()
            assertEquals(listOf(Uri.parse("content://vault")), takenPermissions)
            assertEquals(1, writeSettingsChangedCalls)
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
    fun `uiState reflects the stored pattern before any typing`() =
        runTest {
            val vm = viewModel()
            repository.saveVaultUri("content://vault")
            repository.saveFileNamePattern("[note]YYYY-MM-DD.md")

            val state = vm.uiState.first { it.fileNamePattern == "[note]YYYY-MM-DD.md" }
            assertFalse(state.patternInvalid)
        }

    @Test
    fun `typing a valid pattern updates uiState but does not save or notify`() =
        runTest {
            val vm = viewModel()
            repository.saveVaultUri("content://vault")

            vm.onFileNamePatternChange("[note]YYYY-MM-DD.md")
            val state = vm.uiState.first { it.fileNamePattern == "[note]YYYY-MM-DD.md" }

            assertFalse(state.patternInvalid)
            assertEquals(0, writeSettingsChangedCalls)
            assertEquals("YYYY-MM-DD.md", repository.settings.first()?.fileNamePattern)
        }

    @Test
    fun `typing an invalid pattern flags it without saving or notifying`() =
        runTest {
            val vm = viewModel()
            repository.saveVaultUri("content://vault")

            vm.onFileNamePatternChange("[bad.md")
            val state = vm.uiState.first { it.patternInvalid }

            assertEquals("[bad.md", state.fileNamePattern)
            assertEquals(0, writeSettingsChangedCalls)
            assertEquals("YYYY-MM-DD.md", repository.settings.first()?.fileNamePattern)
        }

    @Test
    fun `onSaveClicked with a valid draft saves once and notifies once`() =
        runTest {
            val vm = viewModel()
            repository.saveVaultUri("content://vault")

            vm.onFileNamePatternChange("[note]YYYY-MM-DD.md")
            vm.onSaveClicked()
            advanceUntilIdle()

            assertEquals(1, writeSettingsChangedCalls)
            assertEquals("[note]YYYY-MM-DD.md", repository.settings.first()?.fileNamePattern)
        }

    @Test
    fun `onSaveClicked with an invalid draft does not save or notify`() =
        runTest {
            val vm = viewModel()
            repository.saveVaultUri("content://vault")

            vm.onFileNamePatternChange("[bad.md")
            vm.onSaveClicked()
            advanceUntilIdle()

            assertEquals(0, writeSettingsChangedCalls)
            assertEquals("YYYY-MM-DD.md", repository.settings.first()?.fileNamePattern)
        }

    @Test
    fun `onSaveClicked before any typing is a no-op`() =
        runTest {
            val vm = viewModel()
            repository.saveVaultUri("content://vault")

            vm.onSaveClicked()
            advanceUntilIdle()

            assertEquals(0, writeSettingsChangedCalls)
            assertEquals("YYYY-MM-DD.md", repository.settings.first()?.fileNamePattern)
        }
}
