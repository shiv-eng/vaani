package com.murmur.transcriber

import android.app.Application
import com.murmur.transcriber.di.appModule
import com.murmur.transcriber.shared.di.platformModule
import com.murmur.transcriber.shared.di.sharedModule
import com.murmur.transcriber.whisper.di.whisperModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MurmurApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MurmurApplication)
            modules(appModule, sharedModule, platformModule(), whisperModule)
        }
    }
}
