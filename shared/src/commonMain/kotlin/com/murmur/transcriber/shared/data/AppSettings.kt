package com.murmur.transcriber.shared.data

import com.russhwolf.settings.Settings

/** Small persisted flags that aren't worth a database table. */
class AppSettings(private val settings: Settings) {
    var hasCompletedOnboarding: Boolean
        get() = settings.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        set(value) = settings.putBoolean(KEY_ONBOARDING_COMPLETE, value)

    private companion object {
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    }
}
