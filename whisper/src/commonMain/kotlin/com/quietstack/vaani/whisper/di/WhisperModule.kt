package com.quietstack.vaani.whisper.di

import com.quietstack.vaani.whisper.TranscriptionEngine
import com.quietstack.vaani.whisper.createTranscriptionEngine
import org.koin.dsl.module

val whisperModule = module {
    // single, not factory: loading a model costs far more than transcribing a
    // short recording, so the same engine (and its loaded model) stays warm
    // across jobs for the app's lifetime — see AndroidTranscriptionEngine.
    single<TranscriptionEngine> { createTranscriptionEngine() }
}
