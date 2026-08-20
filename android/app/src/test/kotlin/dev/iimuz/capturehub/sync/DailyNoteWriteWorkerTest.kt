package dev.iimuz.capturehub.sync

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import dev.iimuz.capturehub.core.database.AppDatabase
import dev.iimuz.capturehub.core.database.CaptureEntity
import dev.iimuz.capturehub.core.database.CaptureStatus
import dev.iimuz.capturehub.core.datastore.VaultSettingsRepository
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DailyNoteWriteWorkerTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    AppDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun TestScope.repository(withVault: Boolean): VaultSettingsRepository {
        val dataStore =
            PreferenceDataStoreFactory.create(scope = backgroundScope) {
                tmp.newFolder().resolve("settings.preferences_pb")
            }
        val repository = VaultSettingsRepository(dataStore)
        if (withVault) {
            repository.saveVaultUri("content://vault")
        }
        return repository
    }

    private fun buildWorker(
        repository: VaultSettingsRepository,
        files: VaultFiles,
    ): DailyNoteWriteWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val factory =
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker =
                    DailyNoteWriteWorker(
                        appContext = appContext,
                        params = workerParameters,
                        dao = db.captureDao(),
                        settingsRepository = repository,
                        writer = DailyNoteWriter(),
                        vaultFiles = { files },
                    )
            }
        return TestListenableWorkerBuilder<DailyNoteWriteWorker>(context)
            .setWorkerFactory(factory)
            .build()
    }

    private fun capture(
        id: String,
        createdAt: Long = 0L,
    ) = CaptureEntity(
        id = id,
        text = "text-$id",
        createdAt = createdAt,
        status = CaptureStatus.RECEIVED,
    )

    @Test
    fun `writes pending captures and marks them written`() =
        runTest {
            val dao = db.captureDao()
            dao.insert(capture("a", createdAt = 1L))
            dao.insert(capture("b", createdAt = 2L))
            val files = InMemoryVaultFiles()
            val worker = buildWorker(repository(withVault = true), files)
            val result = worker.doWork()
            assertEquals(ListenableWorker.Result.success(), result)
            assertEquals(CaptureStatus.WRITTEN, dao.findById("a")?.status)
            assertEquals(CaptureStatus.WRITTEN, dao.findById("b")?.status)
            val content = files.files.values.single()
            assertTrue(content.contains("text-a"))
            assertTrue(content.contains("text-b"))
        }

    @Test
    fun `marks failed captures and retries on write failure`() =
        runTest {
            val dao = db.captureDao()
            dao.insert(capture("a"))
            val worker = buildWorker(repository(withVault = true), FailingVaultFiles())
            val result = worker.doWork()
            assertEquals(ListenableWorker.Result.retry(), result)
            assertEquals(CaptureStatus.FAILED_WRITE, dao.findById("a")?.status)
        }

    @Test
    fun `fails without touching captures when vault is not configured`() =
        runTest {
            val dao = db.captureDao()
            dao.insert(capture("a"))
            val worker = buildWorker(repository(withVault = false), InMemoryVaultFiles())
            val result = worker.doWork()
            assertEquals(ListenableWorker.Result.failure(), result)
            assertEquals(CaptureStatus.RECEIVED, dao.findById("a")?.status)
        }
}
