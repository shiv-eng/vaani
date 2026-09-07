package com.quietstack.vaani.recording

import com.quietstack.vaani.shared.domain.model.Recording
import com.quietstack.vaani.shared.domain.model.RecordingSource
import com.quietstack.vaani.shared.domain.model.Segment
import com.quietstack.vaani.shared.domain.model.TranscriptionJob
import com.quietstack.vaani.shared.domain.model.TranscriptionState
import com.quietstack.vaani.shared.domain.repository.RecordingRepository
import com.quietstack.vaani.shared.domain.repository.TranscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression coverage for the interrupted-recording cleanup this session
 * added: a recording row is created at durationMs=0 the instant recording
 * starts (see BeginRecordingUseCase) so live segments have a real id to
 * attach to, so if the process dies before FinalizeRecordingUseCase ever
 * runs (force-stop, OS kill, crash), that row is stuck forever with no
 * usable audio. This must delete exactly those rows — not a recording that
 * legitimately has zero duration for some other reason, and never an
 * imported file, which never goes through the live durationMs=0 stage.
 */
class CleanupOrphanedRecordingsUseCaseTest {

    private fun recording(
        id: String,
        source: RecordingSource,
        durationMs: Long,
    ) = Recording(
        id = id,
        displayName = id,
        fileName = "$id.m4a",
        createdAt = 0L,
        durationMs = durationMs,
        sizeBytes = 0L,
        source = source,
        mimeType = "audio/mp4",
        sampleRate = 16_000,
        transcriptionState = TranscriptionState.NONE,
    )

    @Test
    fun deletesOnlyOrphanedRecordedEntries() = runTest {
        val recordings = FakeRecordingRepository(
            listOf(
                recording("orphaned-live", RecordingSource.RECORDED, durationMs = 0L),
                recording("finished-live", RecordingSource.RECORDED, durationMs = 5_000L),
                recording("imported-zero-duration", RecordingSource.IMPORTED, durationMs = 0L),
            ),
        )
        val transcriptions = FakeTranscriptionRepository()
        val deleted = mutableListOf<String>()

        CleanupOrphanedRecordingsUseCase(recordings, transcriptions, deleteRecording = { deleted.add(it) })()

        assertEquals(listOf("orphaned-live"), deleted)
        assertTrue(transcriptions.failAllRunningJobsCalled)
    }

    @Test
    fun doesNothingWhenNoRecordingsAreOrphaned() = runTest {
        val recordings = FakeRecordingRepository(
            listOf(recording("finished-live", RecordingSource.RECORDED, durationMs = 5_000L)),
        )
        val transcriptions = FakeTranscriptionRepository()
        val deleted = mutableListOf<String>()

        CleanupOrphanedRecordingsUseCase(recordings, transcriptions, deleteRecording = { deleted.add(it) })()

        assertTrue(deleted.isEmpty())
        assertTrue(transcriptions.failAllRunningJobsCalled)
    }
}

private class FakeRecordingRepository(initial: List<Recording>) : RecordingRepository {
    private val state = MutableStateFlow(initial)
    override fun observeAll(): Flow<List<Recording>> = state
    override suspend fun getById(id: String): Recording? = state.value.firstOrNull { it.id == id }
    override suspend fun create(
        filePath: String,
        displayName: String,
        durationMs: Long,
        sizeBytes: Long,
        source: RecordingSource,
        mimeType: String,
        sampleRate: Int,
        id: String?,
    ): Recording = error("not needed for this test")
    override suspend fun rename(id: String, newName: String) = error("not needed for this test")
    override suspend fun updateDuration(id: String, durationMs: Long, sizeBytes: Long) = error("not needed for this test")
    override suspend fun delete(id: String) {
        state.value = state.value.filterNot { it.id == id }
    }
}

private class FakeTranscriptionRepository : TranscriptionRepository {
    var failAllRunningJobsCalled = false
        private set

    override fun observeJob(recordingId: String): Flow<TranscriptionJob?> = error("not needed for this test")
    override fun observeSegments(recordingId: String): Flow<List<Segment>> = error("not needed for this test")
    override suspend fun createJob(recordingId: String, modelId: String, language: String?, windowsTotal: Int): TranscriptionJob =
        error("not needed for this test")
    override suspend fun appendSegment(jobId: String, segment: Segment) = error("not needed for this test")
    override suspend fun markWindowDone(jobId: String, windowsDone: Int) = error("not needed for this test")
    override suspend fun markComplete(jobId: String) = error("not needed for this test")
    override suspend fun markFailed(jobId: String, error: String) = error("not needed for this test")
    override suspend fun cancel(jobId: String) = error("not needed for this test")
    override suspend fun editSegment(segmentId: String, editedText: String) = error("not needed for this test")
    override suspend fun searchRecordingIds(query: String): List<String> = error("not needed for this test")
    override suspend fun failAllRunningJobs() {
        failAllRunningJobsCalled = true
    }
}
