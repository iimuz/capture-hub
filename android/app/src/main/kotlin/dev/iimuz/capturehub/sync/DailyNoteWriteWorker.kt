package dev.iimuz.capturehub.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import dev.iimuz.capturehub.core.database.CaptureDao
import dev.iimuz.capturehub.core.database.CaptureStatus
import dev.iimuz.capturehub.core.datastore.VaultSettingsRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class DailyNoteWriteWorker(
    appContext: Context,
    params: WorkerParameters,
    private val dao: CaptureDao,
    private val settingsRepository: VaultSettingsRepository,
    private val writer: DailyNoteWriter,
    private val vaultFiles: (String) -> VaultFiles,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        // Vault 未設定時は再試行しない。設定完了時に再エンキューされる
        val settings = settingsRepository.settings.first() ?: return Result.failure()
        val files =
            try {
                vaultFiles(settings.vaultUri)
            } catch (_: Exception) {
                dao.pending().forEach { dao.updateStatus(it.id, CaptureStatus.FAILED_WRITE) }
                return Result.retry()
            }
        var anyFailed = false
        for (capture in dao.pending()) {
            when (writer.append(files, settings.fileNamePattern, capture)) {
                WriteResult.Written, WriteResult.AlreadyWritten -> {
                    dao.updateStatus(capture.id, CaptureStatus.WRITTEN)
                }

                is WriteResult.Failed -> {
                    dao.updateStatus(capture.id, CaptureStatus.FAILED_WRITE)
                    anyFailed = true
                }
            }
        }
        return if (anyFailed) Result.retry() else Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "daily-note-write"
    }
}

fun enqueueDailyNoteWrite(context: Context) {
    val request =
        OneTimeWorkRequestBuilder<DailyNoteWriteWorker>()
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS,
                TimeUnit.MILLISECONDS,
            ).build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        DailyNoteWriteWorker.UNIQUE_WORK_NAME,
        ExistingWorkPolicy.APPEND_OR_REPLACE,
        request,
    )
}

class CaptureHubWorkerFactory(
    private val dao: CaptureDao,
    private val settingsRepository: VaultSettingsRepository,
    private val writer: DailyNoteWriter,
    private val vaultFiles: (Context, String) -> VaultFiles,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? =
        when (workerClassName) {
            DailyNoteWriteWorker::class.java.name -> {
                DailyNoteWriteWorker(
                    appContext = appContext,
                    params = workerParameters,
                    dao = dao,
                    settingsRepository = settingsRepository,
                    writer = writer,
                    vaultFiles = { uri -> vaultFiles(appContext, uri) },
                )
            }

            else -> {
                null
            }
        }
}
