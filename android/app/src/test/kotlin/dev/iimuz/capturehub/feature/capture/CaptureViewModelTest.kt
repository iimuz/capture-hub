package dev.iimuz.capturehub.feature.capture

import dev.iimuz.capturehub.MainDispatcherRule
import dev.iimuz.capturehub.core.database.CaptureStatus
import dev.iimuz.capturehub.core.database.FakeCaptureDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CaptureViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = FakeCaptureDao()
    private var savedCallbacks = 0

    private fun viewModel() =
        CaptureViewModel(
            dao = dao,
            onSaved = { savedCallbacks += 1 },
            newId = { "01TESTID0000000000000000AA" },
            now = { 1_000L },
        )

    @Test
    fun `save inserts received capture and clears input`() =
        runTest {
            val vm = viewModel()
            vm.onTextChange("hello")
            vm.save()
            assertEquals(1, dao.captures.size)
            val capture = dao.captures.first()
            assertEquals("01TESTID0000000000000000AA", capture.id)
            assertEquals("hello", capture.text)
            assertEquals(1_000L, capture.createdAt)
            assertEquals(CaptureStatus.RECEIVED, capture.status)
            assertEquals("", vm.text.value)
            assertEquals(1, savedCallbacks)
        }

    @Test
    fun `save trims surrounding whitespace`() =
        runTest {
            val vm = viewModel()
            vm.onTextChange("  hello \n")
            vm.save()
            assertEquals("hello", dao.captures.first().text)
        }

    @Test
    fun `save ignores blank input`() =
        runTest {
            val vm = viewModel()
            vm.onTextChange("   \n ")
            vm.save()
            assertTrue(dao.captures.isEmpty())
            assertEquals(0, savedCallbacks)
        }

    @Test
    fun `save emits save event`() =
        runTest {
            val vm = viewModel()
            vm.onTextChange("hello")
            vm.save()
            // Channel に送信済みのイベントを受信できなければ runTest がタイムアウトする
            vm.saveEvents.first()
        }
}
