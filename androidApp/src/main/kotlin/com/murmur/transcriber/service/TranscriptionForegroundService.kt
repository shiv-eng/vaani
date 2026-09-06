package com.murmur.transcriber.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.murmur.transcriber.MainActivity
import com.murmur.transcriber.shared.domain.repository.ModelRepository
import com.murmur.transcriber.shared.domain.repository.RecordingRepository
import com.murmur.transcriber.shared.domain.usecase.StartTranscriptionUseCase
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

private const val CHANNEL_ID = "transcription"
private const val NOTIFICATION_ID = 1001
private const val ACTION_CANCEL = "com.murmur.transcriber.action.CANCEL_TRANSCRIPTION"
private const val EXTRA_RECORDING_ID = "recordingId"
private const val EXTRA_MODEL_ID = "modelId"
private const val EXTRA_LANGUAGE = "language"

/**
 * FR-13: runs a transcription job so it survives app backgrounding and screen
 * lock. One job at a time — starting a new one while another runs cancels
 * the first, matching a single foreground notification's single progress bar.
 */
class TranscriptionForegroundService : Service() {

    private val startTranscriptionUseCase: StartTranscriptionUseCase by inject()
    private val recordingRepository: RecordingRepository by inject()
    private val modelRepository: ModelRepository by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var runningJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            runningJob?.cancel()
            stopSelf()
            return START_NOT_STICKY
        }

        val recordingId = intent?.getStringExtra(EXTRA_RECORDING_ID)
        val modelId = intent?.getStringExtra(EXTRA_MODEL_ID)
        if (recordingId == null || modelId == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        val language = intent.getStringExtra(EXTRA_LANGUAGE)

        runningJob?.cancel()
        ensureChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Recording", progress = 0f))
        runningJob = serviceScope.launch { runTranscription(recordingId, modelId, language) }
        return START_NOT_STICKY
    }

    private suspend fun runTranscription(recordingId: String, modelId: String, language: String?) {
        val model = modelRepository.observeDownloaded().first().firstOrNull { it.id == modelId }
        val recordingName = recordingRepository.getById(recordingId)?.displayName
        if (model == null || recordingName == null) {
            stopSelf()
            return
        }
        val modelPath = File(filesDir, "models/${model.fileName}").absolutePath

        startTranscriptionUseCase(recordingId, modelPath, modelId, language)
            .catch { /* the use case already persists CANCELLED/FAILED before rethrowing */ }
            .collect { progress -> updateNotification(recordingName, progress) }

        stopSelf()
    }

    override fun onDestroy() {
        runningJob = null
        super.onDestroy()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Transcription", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Ongoing transcription progress"
                },
            )
        }
    }

    private fun buildNotification(recordingName: String, progress: Float): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cancelIntent = PendingIntent.getService(
            this, 0,
            Intent(this, TranscriptionForegroundService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val percent = (progress * 100).roundToInt()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Transcribing “$recordingName”")
            .setContentText("$percent% complete")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percent, false)
            .setContentIntent(contentIntent)
            .addAction(NotificationCompat.Action(0, "Cancel", cancelIntent))
            .build()
    }

    private fun updateNotification(recordingName: String, progress: Float) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(recordingName, progress))
    }

    companion object {
        fun start(context: Context, recordingId: String, modelId: String, language: String?) {
            val intent = Intent(context, TranscriptionForegroundService::class.java)
                .putExtra(EXTRA_RECORDING_ID, recordingId)
                .putExtra(EXTRA_MODEL_ID, modelId)
                .putExtra(EXTRA_LANGUAGE, language)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
