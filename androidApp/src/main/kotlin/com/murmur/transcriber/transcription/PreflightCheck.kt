package com.murmur.transcriber.transcription

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs

/**
 * FR-19: catch a doomed job before it starts rather than partway through a
 * two-hour recording. Peak RAM is estimated as a multiple of the model file
 * size — the spike measured a 96 MB model peaking around 780 MB RSS, a
 * roughly 8x ratio (spike/RESULTS.md) — since there is no per-model catalogue
 * of measured figures yet (that lands with Milestone 7).
 */
object PreflightCheck {

    private const val STORAGE_SAFETY_BUFFER_BYTES = 50L * 1024 * 1024
    private const val PEAK_RSS_MULTIPLIER = 8L

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
        val neededMemory = modelSizeBytes * PEAK_RSS_MULTIPLIER
        if (memInfo.availMem < neededMemory) {
            return Result.LowMemory(neededMemory, memInfo.availMem)
        }

        return Result.Ok
    }
}
