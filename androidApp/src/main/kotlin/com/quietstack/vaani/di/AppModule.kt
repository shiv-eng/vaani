package com.quietstack.vaani.di

import com.quietstack.vaani.recording.CleanupOrphanedRecordingsUseCase
import com.quietstack.vaani.recording.DeleteRecordingUseCase
import com.quietstack.vaani.ui.detail.DetailViewModel
import com.quietstack.vaani.ui.edit.EditViewModel
import com.quietstack.vaani.ui.home.HomeViewModel
import com.quietstack.vaani.ui.importing.ImportViewModel
import com.quietstack.vaani.ui.library.LibraryViewModel
import com.quietstack.vaani.ui.onboarding.OnboardingViewModel
import com.quietstack.vaani.ui.record.RecordViewModel
import com.quietstack.vaani.ui.search.SearchViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    factory { DeleteRecordingUseCase(context = androidContext(), recordingRepository = get()) }
    factory {
        val deleteRecordingUseCase = get<DeleteRecordingUseCase>()
        CleanupOrphanedRecordingsUseCase(
            recordingRepository = get(),
            transcriptionRepository = get(),
            deleteRecording = deleteRecordingUseCase::invoke,
        )
    }

    viewModel { LibraryViewModel(recordingRepository = get(), deleteRecordingUseCase = get()) }
    viewModel { HomeViewModel(recordingRepository = get(), transcriptionRepository = get()) }
    viewModel { RecordViewModel(context = androidContext(), appSettings = get()) }
    viewModel { ImportViewModel(context = androidContext(), importRecordingUseCase = get()) }
    viewModel { SearchViewModel(recordingRepository = get(), transcriptionRepository = get()) }
    viewModel { OnboardingViewModel(context = androidContext(), modelRepository = get(), appSettings = get()) }
    viewModel { (recordingId: String) ->
        DetailViewModel(
            recordingId = recordingId,
            context = androidContext(),
            recordingRepository = get(),
            transcriptionRepository = get(),
            modelRepository = get(),
            deleteRecordingUseCase = get(),
            appSettings = get(),
            progressTracker = get(),
            bookmarkRepository = get(),
        )
    }
    viewModel { (recordingId: String) -> EditViewModel(recordingId = recordingId, transcriptionRepository = get()) }
}
