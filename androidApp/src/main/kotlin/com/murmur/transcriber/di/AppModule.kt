package com.murmur.transcriber.di

import com.murmur.transcriber.recording.DeleteRecordingUseCase
import com.murmur.transcriber.ui.detail.DetailViewModel
import com.murmur.transcriber.ui.home.HomeViewModel
import com.murmur.transcriber.ui.importing.ImportViewModel
import com.murmur.transcriber.ui.library.LibraryViewModel
import com.murmur.transcriber.ui.record.RecordViewModel
import com.murmur.transcriber.ui.storage.StorageViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    factory { DeleteRecordingUseCase(context = androidContext(), recordingRepository = get()) }

    viewModel { LibraryViewModel(recordingRepository = get(), deleteRecordingUseCase = get()) }
    viewModel { HomeViewModel(recordingRepository = get()) }
    viewModel { RecordViewModel(context = androidContext(), finalizeRecordingUseCase = get()) }
    viewModel { ImportViewModel(context = androidContext(), importRecordingUseCase = get()) }
    viewModel { StorageViewModel(context = androidContext(), modelRepository = get()) }
    viewModel { (recordingId: String) ->
        DetailViewModel(
            recordingId = recordingId,
            context = androidContext(),
            recordingRepository = get(),
            transcriptionRepository = get(),
            modelRepository = get(),
            deleteRecordingUseCase = get(),
        )
    }
}
