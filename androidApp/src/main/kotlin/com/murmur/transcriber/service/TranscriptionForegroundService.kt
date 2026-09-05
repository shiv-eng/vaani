package com.murmur.transcriber.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * FR-13: runs a transcription job so it survives app backgrounding and screen lock.
 * Wiring to the window-planner orchestrator (section 09) lands with the whisper.cpp spike;
 * this is the scaffold hook so the manifest declaration and process lifecycle exist first.
 */
class TranscriptionForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO: start foreground with a progress notification, then drive the
        // orchestrator use case, persisting each window via TranscriptionRepository.
        return START_NOT_STICKY
    }
}
