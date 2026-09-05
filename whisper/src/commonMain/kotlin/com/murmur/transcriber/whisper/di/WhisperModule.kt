package com.murmur.transcriber.whisper.di

import com.murmur.transcriber.whisper.TranscriptionEngine
import com.murmur.transcriber.whisper.createTranscriptionEngine
import org.koin.dsl.module

val whisperModule = module {
    // factory, not single: a fresh native context per job, released when the job ends.
    factory<TranscriptionEngine> { createTranscriptionEngine() }
}
