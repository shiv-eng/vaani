package com.murmur.transcriber.di

import com.murmur.transcriber.ui.library.LibraryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { LibraryViewModel(recordingRepository = get()) }
}
