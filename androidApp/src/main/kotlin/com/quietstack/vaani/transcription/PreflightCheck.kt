package com.quietstack.vaani.transcription

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs

/**
 * FR-19: catch a doomed job before it starts rather than partway through a
 * two-hour recording.
 *
 * Peak RAM is estimated as (model file size * multiplier) + a fixed
 * per-process overhead. The multiplier used to be a flat 8x taken directly
 * from one spike measurement (a 96 MB model peaking around 780 MB RSS,
 * spike/RESULTS.md) — that ratio almost certainly wasn't "8x model size",
 * it was "~530 MB of roughly fixed overhead (Android process baseline, the
 * app's own runtime, ggml's compute buffers) plus the model itself" that
 * only *looked* like a clean multiplier at that one model size. Reusing it
 * unchanged for the "medium" model (514 MB) demanded ~4.1 GB — confirmed on
 * a real device to reject a 9-second recording on a phone with a perfectly
 * normal 2.9 GB free, which is a shipped-app-breaking bug, not caution.
 * This is still an estimate, not a measurement — there is no per-model
 * catalogue of real figures yet (Milestone 7) — but it no longer explodes
 * for a bigger model the way a flat multiplier does.
 */
object PreflightCheck {

    private const val STORAGE_SAFETY_BUFFER_BYTES = 50L * 1024 * 1024
    private const val PEAK_RSS_MULTIPLIER = 2L
    private const val FIXED_OVERHEAD_BYTES = 300L * 1024 * 1024

    sealed interface Result {
        data object Ok : Result
        data class InsufficientStorage(val neededBytes: Long, val availableBytes: Long) : Result
        data class LowMemory(val neededBytes: Long, val availableBytes: Long) : Result
    }

    fun run(context: Context, modelSizeBytes: Long): Result {
        val stat = StatFs(context.filesDir.path)
        val availableStorage = stat.availableBytes
        val neededStorage = modelSizeBytes + STORAGE_SAFETY_BUFFER_BYTES
        if (availableStorage < neededStorage) {
            return Result.InsufficientStorage(neededStorage, availableStorage)
        }

        val memInfo = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memInfo)
        val neededMemory = modelSizeBytes * PEAK_RSS_MULTIPLIER + FIXED_OVERHEAD_BYTES
        if (memInfo.availMem < neededMemory) {
            return Result.LowMemory(neededMemory, memInfo.availMem)
        }

        return Result.Ok
    }
}
