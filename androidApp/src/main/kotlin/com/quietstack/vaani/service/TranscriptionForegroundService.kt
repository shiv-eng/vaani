package com.quietstack.vaani.service

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
import com.quietstack.vaani.MainActivity
import com.quietstack.vaani.shared.domain.pipeline.TranscriptionProgressTracker
import com.quietstack.vaani.shared.domain.repository.ModelRepository
import com.quietstack.vaani.shared.domain.repository.RecordingRepository
import com.quietstack.vaani.shared.domain.usecase.LOADING_MODEL_SENTINEL
import com.quietstack.vaani.shared.domain.usecase.StartTranscriptionUseCase
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

private const val CHANNEL_ID = "transcription"
private const val COMPLETE_CHANNEL_ID = "transcription_complete"
private const val NOTIFICATION_ID = 1001
private const val ACTION_CANCEL = "com.quietstack.vaani.action.CANCEL_TRANSCRIPTION"
private const val EXTRA_RECORDING_ID = "recordingId"
private const val EXTRA_MODEL_ID = "modelId"
private const val EXTRA_LANGUAGE = "language"

private data class TranscriptionRequest(val recordingId: String, val modelId: String, val language: String?)

/**
 * FR-13: runs transcription jobs so they survive app backgrounding and
 * screen lock.
 *
 * Strictly one decode at a time, as a real FIFO queue — not "cancel the
 * old one and start the new one". That was tried and is fundamentally
 * broken: the actual decode is a plain synchronous JNI call with no
 * suspension point inside it, so Kotlin's cooperative coroutine
 * cancellation cannot interrupt it once it's started — cancel() only
 * marks the coroutine, it doesn't stop the native call already running on
 * that thread. A second recording finishing while the first is still
 * decoding used to "cancel" the first and start a second job anyway, which
 * then blocked silently on AndroidTranscriptionEngine's lock waiting for
 * the "cancelled" one to actually finish — visible as two recordings stuck
 * on RUNNING and a wait that only ever grew. Queueing means the second
 * recording's transcription honestly waits its turn instead of pretending
 * to preempt a job it can't actually stop.
 */
class TranscriptionForegroundService : Service() {

    private val startTranscriptionUseCase: StartTranscriptionUseCase by inject()
    private val recordingRepository: RecordingRepository by inject()
    private val modelRepository: ModelRepository by inject()
    private val progressTracker: TranscriptionProgressTracker by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pendingQueue = ArrayDeque<TranscriptionRequest>()
    private var processingJob: Job? = null
    private var currentRecordingId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            // Only the queued-but-not-yet-started requests can actually be
            // dropped, for the reason explained on the class doc — the one
            // currently decoding runs to completion regardless.
            pendingQueue.clear()
            return START_NOT_STICKY
        }

        val recordingId = intent?.getStringExtra(EXTRA_RECORDING_ID)
        val modelId = intent?.getStringExtra(EXTRA_MODEL_ID)
        if (recordingId == null || modelId == null) {
            if (processingJob == null) stopSelf()
            return START_NOT_STICKY
        }
        val language = intent.getStringExtra(EXTRA_LANGUAGE)

        val alreadyQueuedOrRunning = currentRecordingId == recordingId || pendingQueue.any { it.recordingId == recordingId }
        if (!alreadyQueuedOrRunning) {
            pendingQueue.addLast(TranscriptionRequest(recordingId, modelId, language))
        }
        ensureChannel()
        startForeground(NOTIFICATION_ID, buildLoadingNotification("Recording", elapsedSeconds = 0))
        if (processingJob == null) {
            processNext()
        }
        return START_NOT_STICKY
    }

    private fun processNext() {
        val request = pendingQueue.removeFirstOrNull()
        if (request == null) {
            currentRecordingId = null
            stopSelf()
            return
        }
        currentRecordingId = request.recordingId
        processingJob = serviceScope.launch {
            runTranscription(request.recordingId, request.modelId, request.language)
            processingJob = null
            processNext()
        }
    }

    private suspend fun runTranscription(recordingId: String, modelId: String, language: String?) {
        val model = modelRepository.observeDownloaded().first().firstOrNull { it.id == modelId }
        val recordingName = recordingRepository.getById(recordingId)?.displayName
        if (model == null || recordingName == null) {
            return
        }
        val modelPath = File(filesDir, "models/${model.fileName}").absolutePath

        // whisper.cpp's own progress callback only starts firing once decode
        // actually begins — nothing reports progress during the model load
        // itself, so without this the notification just sits on an
        // indeterminate spinner with zero forward motion for however long
        // that takes. A ticking elapsed-time count is the one real, honest
        // number available for that phase; it stops the moment real decode
        // progress arrives and that takes over.
        var sawRealProgress = false
        val loadingTicker = serviceScope.launch {
            var elapsedSeconds = 0
            while (!sawRealProgress) {
                delay(1000)
                elapsedSeconds++
                if (!sawRealProgress) updateLoadingNotification(recordingName, elapsedSeconds)
            }
        }

        var succeeded = true
        try {
            startTranscriptionUseCase(recordingId, modelPath, modelId, language)
                .catch { succeeded = false /* the use case already persists CANCELLED/FAILED before rethrowing */ }
                .collect { progress ->
                    progressTracker.update(recordingId, progress)
                    // The sentinel means "genuinely loading" — the ticker above
                    // already drives the notification for that; only real decode
                    // progress (always >= 0) gets the percent-based notification.
                    if (progress != LOADING_MODEL_SENTINEL) {
                        sawRealProgress = true
                        updateNotification(recordingName, progress)
                    }
                }
        } finally {
            loadingTicker.cancel()
            progressTracker.clear(recordingId)
        }

        // A transcription commonly outlives the user having the app open —
        // it's explicitly allowed to take minutes (see PreflightCheck /
        // TranscribingCard). Without this, finishing while the app is closed
        // is invisible: the ongoing progress notification just disappears
        // with stopForeground, and nothing tells the user it's actually
        // done. A separate, dismissible notification on its own channel
        // (so it can actually alert, unlike the low-priority ongoing one)
        // closes that gap.
        if (succeeded) {
            postCompletionNotification(recordingId, recordingName)
        }
    }

    override fun onDestroy() {
        processingJob = null
        pendingQueue.clear()
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
        if (manager.getNotificationChannel(COMPLETE_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                // DEFAULT, not LOW — this one is a single actionable moment
                // ("your transcript is ready"), not an ongoing status the
                // ongoing ones deliberately stay quiet about.
                NotificationChannel(COMPLETE_CHANNEL_ID, "Transcript ready", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "A recording finished transcribing"
                },
            )
        }
    }

    private fun postCompletionNotification(recordingId: String, recordingName: String) {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, COMPLETE_CHANNEL_ID)
            .setContentTitle("Transcript ready")
            .setContentText("“$recordingName” has been transcribed")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(recordingId.hashCode(), notification)
    }

    private fun baseNotificationBuilder(recordingName: String): NotificationCompat.Builder {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cancelIntent = PendingIntent.getService(
            this, 0,
            Intent(this, TranscriptionForegroundService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Transcribing “$recordingName”")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(NotificationCompat.Action(0, "Cancel", cancelIntent))
    }

    private fun buildNotification(recordingName: String, progress: Float): Notification {
        val percent = (progress * 100).roundToInt()
        return baseNotificationBuilder(recordingName)
            .setContentText("$percent% complete")
            .setProgress(100, percent, false)
            .build()
    }

    private fun buildLoadingNotification(recordingName: String, elapsedSeconds: Int): Notification =
        baseNotificationBuilder(recordingName)
            .setContentText("Loading speech model… ${elapsedSeconds}s")
            .setProgress(0, 0, true)
            .build()

    private fun updateNotification(recordingName: String, progress: Float) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(recordingName, progress))
    }

    private fun updateLoadingNotification(recordingName: String, elapsedSeconds: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildLoadingNotification(recordingName, elapsedSeconds))
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
