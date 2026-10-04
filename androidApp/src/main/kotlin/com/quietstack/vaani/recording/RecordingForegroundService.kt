package com.quietstack.vaani.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.quietstack.vaani.MainActivity
import com.quietstack.vaani.service.TranscriptionForegroundService
import com.quietstack.vaani.shared.data.AppSettings
import com.quietstack.vaani.shared.data.ModelCatalogue
import com.quietstack.vaani.shared.domain.repository.ModelRepository
import com.quietstack.vaani.shared.domain.usecase.BeginRecordingUseCase
import com.quietstack.vaani.shared.domain.usecase.FinalizeRecordingUseCase
import com.quietstack.vaani.transcription.PreflightCheck
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

private const val CHANNEL_ID = "recording"
private const val NOTIFICATION_ID = 2001
private const val MIN_MEANINGFUL_DURATION_MS = 1_000L

/**
 * FR-01/FR-02: a recording must survive backgrounding and screen lock, and
 * pause rather than crash on interruption (a headset unplugging). The
 * previous approach — a MediaRecorder owned directly by RecordViewModel —
 * dies the moment Android reclaims the backgrounded process, since a plain
 * ViewModel carries no process priority. A foreground service is what holds
 * that priority for the length of an arbitrarily long recording;
 * RecordViewModel now binds to this rather than owning the recorder itself.
 *
 * This deliberately does not request AudioFocus: real-device testing showed
 * requesting it (AUDIOFOCUS_GAIN_TRANSIENT for USAGE_MEDIA, the usual
 * recording-app pattern) got an immediate AUDIOFOCUS_LOSS callback with
 * nothing else audibly playing, auto-pausing every recording within
 * milliseconds of starting it — worse than not handling interruptions at
 * all. Phone-call interruption is a real gap left by dropping this; headset
 * unplug (a distinct, simpler mechanism) still works.
 *
 * The recording row is created the moment recording starts
 * (BeginRecordingUseCase), before there's a finished file — not for live
 * transcription (see below), but so a recording surviving a process death
 * mid-recording (force-stop, OS kill, crash) is a real row the app can find
 * and clean up next launch (CleanupOrphanedRecordingsUseCase), rather than
 * vanishing with no trace. FinalizeRecordingUseCase fills in the real
 * duration/size once MediaRecorder actually stops.
 *
 * Transcription itself is NOT attempted live, word-by-word, while recording.
 * That was tried (whisper.cpp decoding a rolling window every few seconds)
 * and dropped: a model accurate enough for real bilingual (Hindi/English)
 * speech is also too slow to decode a window faster than the window itself
 * takes to speak on a typical phone's CPU, so captions never appeared, and
 * the final catch-up decode on stop ran synchronously on the caller's
 * thread — blocking the whole UI, reading as the app being frozen. Instead,
 * [stopRecording] automatically starts the same proven batch pipeline used
 * by the manual "Transcribe" button on Detail, the moment recording ends —
 * no live captions, but no manual tap needed either, and Detail already
 * shows its own progress card while it runs.
 */
class RecordingForegroundService : Service() {

    private val beginRecordingUseCase: BeginRecordingUseCase by inject()
    private val finalizeRecordingUseCase: FinalizeRecordingUseCase by inject()
    private val deleteRecordingUseCase: DeleteRecordingUseCase by inject()
    private val modelRepository: ModelRepository by inject()
    private val appSettings: AppSettings by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var controller: AudioRecorderController
    private var headsetReceiver: BroadcastReceiver? = null

    private var currentRecordingId: String? = null

    val state get() = controller.state

    inner class LocalBinder : Binder() {
        fun getService(): RecordingForegroundService = this@RecordingForegroundService
    }

    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        controller = AudioRecorderController(applicationContext, serviceScope)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        if (!controller.state.value.isRecording) {
            registerHeadsetReceiver()
            val startInfo = controller.start()
            val newId = UUID.randomUUID().toString()
            currentRecordingId = newId
            serviceScope.launch {
                beginRecordingUseCase(newId, startInfo.filePath, startInfo.mimeType, startInfo.sampleRate)
            }
        }
        return START_NOT_STICKY
    }

    fun pause() = controller.pause()

    fun resume() = controller.resume()

    /**
     * Null means the recording was discarded, not saved — see
     * [MIN_MEANINGFUL_DURATION_MS]. RecordViewModel already treats a null
     * result as "nothing to navigate to", the same as if nothing had been
     * recorded at all.
     */
    suspend fun stopRecording(): String? {
        val result = controller.stop()
        val id = requireNotNull(currentRecordingId)

        // An accidental tap-then-immediate-back (or tap-then-tap-stop) leaves
        // a near-zero-length file — real-device testing showed this saving a
        // permanent, useless "0s" entry in the library instead of just being
        // discarded, which is what a user backing out mid-recording expects.
        val savedId = if (result.durationMs < MIN_MEANINGFUL_DURATION_MS) {
            deleteRecordingUseCase(id)
            null
        } else {
            finalizeRecordingUseCase(id, result.durationMs, result.sizeBytes)
            startTranscriptionIfPossible(id)
            id
        }

        unregisterHeadsetReceiver()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return savedId
    }

    /**
     * Same preflight this recording would get from the manual "Transcribe"
     * button (PreflightCheck) — auto-starting must not skip the one check
     * standing between this and an OOM mid-job. If it fails, or no model is
     * downloaded yet, the recording is simply left untranscribed — Detail's
     * existing "Not transcribed yet" + manual Transcribe button covers that,
     * same as it always has for imports.
     */
    private suspend fun startTranscriptionIfPossible(recordingId: String) {
        // Not just "whichever model was downloaded most recently" — since
        // onboarding started downloading IndicConformer's Hindi model and
        // tokens file alongside the Whisper one, that query can return
        // either of those instead of the actual transcription model,
        // sending whisper.cpp a tokens.txt path as if it were a .bin file.
        // DEFAULT is the one real selectable model this pipeline ever
        // passes to loadModel() — Hindi routing happens inside
        // RoutingTranscriptionEngine based on language, invisibly to this.
        val model = modelRepository.observeDownloaded().first().firstOrNull { it.id == ModelCatalogue.DEFAULT.id } ?: return
        if (PreflightCheck.run(applicationContext, model.sizeBytes) != PreflightCheck.Result.Ok) return
        val language = appSettings.transcriptionLanguage
        TranscriptionForegroundService.start(applicationContext, recordingId, model.id, language)
    }

    override fun onDestroy() {
        unregisterHeadsetReceiver()
        super.onDestroy()
    }

    private fun registerHeadsetReceiver() {
        // ACTION_HEADSET_PLUG is a sticky broadcast: registering the receiver
        // immediately replays the last known plug state even with no real event
        // just happening. On a phone with nothing ever plugged in, that replay is
        // "unplugged" (state 0) — treating it as a real unplug paused every
        // recording within milliseconds of starting it (a real-device finding).
        // Track the previous state and only react to an actual plugged (nonzero)
        // -> unplugged (0) transition, not the initial replay.
        var previousState: Int? = null
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action != Intent.ACTION_HEADSET_PLUG) return
                val state = intent.getIntExtra("state", -1)
                val previous = previousState
                previousState = state
                if (previous != null && previous != 0 && state == 0) {
                    pause()
                }
            }
        }
        headsetReceiver = receiver
        registerReceiver(receiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))
    }

    private fun unregisterHeadsetReceiver() {
        headsetReceiver?.let { runCatching { unregisterReceiver(it) } }
        headsetReceiver = null
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Recording", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Ongoing audio recording"
                },
            )
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording")
            .setContentText("Vaani is recording audio")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .build()
    }
}
