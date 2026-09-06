package com.murmur.transcriber

import android.app.Application
import com.murmur.transcriber.di.appModule
import com.murmur.transcriber.shared.di.platformModule
import com.murmur.transcriber.shared.di.sharedModule
import com.murmur.transcriber.transcription.StuckJobRecovery
import com.murmur.transcriber.whisper.di.whisperModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

class MurmurApplication : Application(), KoinComponent {

    private val stuckJobRecovery: StuckJobRecovery by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MurmurApplication)
            modules(appModule, sharedModule, platformModule(), whisperModule)
        }

        // A job left QUEUED/RUNNING from before this process started has no live service
        // behind it — see StuckJobRecovery for why that's always true in this architecture.
        CoroutineScope(Dispatchers.Default).launch { stuckJobRecovery.recoverIfNeeded() }
    }
}
