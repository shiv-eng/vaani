package com.murmur.transcriber.di

import com.murmur.transcriber.ui.home.HomeViewModel
import com.murmur.transcriber.ui.library.LibraryViewModel
import com.murmur.transcriber.ui.record.RecordViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { LibraryViewModel(recordingRepository = get()) }
    viewModel { HomeViewModel(recordingRepository = get()) }
    viewModel { RecordViewModel(context = androidContext(), finalizeRecordingUseCase = get()) }
}
