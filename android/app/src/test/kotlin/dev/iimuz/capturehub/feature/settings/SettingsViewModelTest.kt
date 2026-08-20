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

    // ViewModel の delay() は Dispatchers.Main (MainDispatcherRule 固有の
    // スケジューラ) 上で走るため、runTest 自身のスケジューラとは別に進める必要がある
    private fun TestScope.advancePastPatternSaveDebounce() {
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        advanceUntilIdle()
    }

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
    fun `invalid pattern is flagged and not saved`() =
        runTest {
            val vm = viewModel()
            vm.onFolderPicked(Uri.parse("content://vault"))
            vm.onFileNamePatternChange("[bad.md")
            val state = vm.uiState.first { it.patternInvalid }
            assertTrue(state.patternInvalid)
            assertEquals("YYYY-MM-DD.md", state.fileNamePattern)
        }

    @Test
    fun `valid pattern change saves and invokes callback after save`() =
        runTest {
            val vm = viewModel()
            // repository へ直接書き込み、ViewModel からの2回目の書き込みが発生する
            // 状況を避ける (viewModelScope 経由の DataStore 書き込みを2回連続で
            // 行うと、テスト用ディスパッチャの構成上 advanceUntilIdle では解決しない)。
            repository.saveVaultUri("content://vault")

            vm.onFileNamePatternChange("[note]YYYY-MM-DD.md")
            advancePastPatternSaveDebounce()

            assertEquals(1, writeSettingsChangedCalls)
            val state = vm.uiState.first { it.fileNamePattern == "[note]YYYY-MM-DD.md" }
            assertFalse(state.patternInvalid)
        }

    @Test
    fun `three rapid valid pattern changes save once with the last value`() =
        runTest {
            val vm = viewModel()
            repository.saveVaultUri("content://vault")

            vm.onFileNamePatternChange("[a]YYYY-MM-DD.md")
            vm.onFileNamePatternChange("[ab]YYYY-MM-DD.md")
            vm.onFileNamePatternChange("[abc]YYYY-MM-DD.md")
            advancePastPatternSaveDebounce()

            assertEquals(1, writeSettingsChangedCalls)
            val state = vm.uiState.first { it.fileNamePattern == "[abc]YYYY-MM-DD.md" }
            assertFalse(state.patternInvalid)
        }

    @Test
    fun `invalid input during debounce window cancels the pending save`() =
        runTest {
            val vm = viewModel()
            repository.saveVaultUri("content://vault")

            vm.onFileNamePatternChange("[note]YYYY-MM-DD.md")
            vm.onFileNamePatternChange("[bad.md")
            advancePastPatternSaveDebounce()

            assertEquals(0, writeSettingsChangedCalls)
            val state = vm.uiState.first { it.patternInvalid }
            assertTrue(state.patternInvalid)
            assertEquals("YYYY-MM-DD.md", state.fileNamePattern)
        }

    @Test
    fun `invalid pattern change neither saves nor invokes callback`() =
        runTest {
            val vm = viewModel()
            vm.onFileNamePatternChange("[bad.md")
            advancePastPatternSaveDebounce()

            assertEquals(0, writeSettingsChangedCalls)
            val state = vm.uiState.first { it.patternInvalid }
            assertTrue(state.patternInvalid)
        }
}
