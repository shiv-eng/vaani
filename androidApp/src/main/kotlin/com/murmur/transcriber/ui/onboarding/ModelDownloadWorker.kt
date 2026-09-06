package com.murmur.transcriber.ui.onboarding

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.murmur.transcriber.shared.domain.repository.ModelRepository
import kotlinx.coroutines.flow.collect
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Finishes a deferred model download once the required network is available (FR-16). */
class ModelDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params), KoinComponent {

    private val modelRepository: ModelRepository by inject()

    override suspend fun doWork(): Result {
        val modelId = inputData.getString(KEY_MODEL_ID) ?: return Result.failure()
        return try {
            modelRepository.download(modelId).collect { }
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }

    companion object {
        private const val KEY_MODEL_ID = "modelId"
        private const val UNIQUE_WORK_NAME = "model-download"

        fun enqueue(context: Context, modelId: String, requireUnmetered: Boolean) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (requireUnmetered) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_MODEL_ID to modelId))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
