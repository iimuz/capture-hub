package dev.iimuz.capturehub.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptureDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: CaptureDao

    @Before
    fun setUp() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    AppDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        dao = db.captureDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun capture(
        id: String,
        createdAt: Long = 0L,
        status: CaptureStatus = CaptureStatus.RECEIVED,
    ) = CaptureEntity(id = id, text = "text-$id", createdAt = createdAt, status = status)

    @Test
    fun `insert and find by id`() =
        runTest {
            dao.insert(capture("a"))
            assertEquals("text-a", dao.findById("a")?.text)
            assertEquals(CaptureStatus.RECEIVED, dao.findById("a")?.status)
        }

    @Test
    fun `find by id returns null for unknown id`() =
        runTest {
            assertNull(dao.findById("missing"))
        }

    @Test
    fun `insert ignores duplicate capture id`() =
        runTest {
            dao.insert(capture("a"))
            val second = dao.insert(capture("a").copy(text = "other"))
            assertEquals(-1L, second)
            assertEquals("text-a", dao.findById("a")?.text)
        }

    @Test
    fun `pending returns received and failed ordered by createdAt`() =
        runTest {
            dao.insert(capture("written", createdAt = 1L, status = CaptureStatus.WRITTEN))
            dao.insert(capture("received", createdAt = 3L))
            dao.insert(capture("failed", createdAt = 2L, status = CaptureStatus.FAILED_WRITE))
            assertEquals(listOf("failed", "received"), dao.pending().map { it.id })
        }

    @Test
    fun `updateStatus transitions capture state`() =
        runTest {
            dao.insert(capture("a"))
            dao.updateStatus("a", CaptureStatus.WRITTEN)
            assertEquals(CaptureStatus.WRITTEN, dao.findById("a")?.status)
        }

    @Test
    fun `latest emits newest capture by createdAt`() =
        runTest {
            dao.insert(capture("old", createdAt = 1L))
            dao.insert(capture("new", createdAt = 2L))
            assertEquals("new", dao.latest().first()?.id)
        }
}
