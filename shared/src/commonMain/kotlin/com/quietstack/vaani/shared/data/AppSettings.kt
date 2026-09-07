package com.quietstack.vaani.shared.data

import com.russhwolf.settings.Settings

/** Small persisted flags that aren't worth a database table. */
class AppSettings(private val settings: Settings) {
    var hasCompletedOnboarding: Boolean
        get() = settings.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        set(value) = settings.putBoolean(KEY_ONBOARDING_COMPLETE, value)

    /**
     * Whisper language code to force for transcription (e.g. "hi"), or null for
     * auto-detect. Defaults to English ("en"), not auto-detect: auto-detect
     * repeatedly misidentified real Hindi speech as a related-but-wrong
     * language (Urdu, then Marathi) in testing, and forcing "hi" didn't fix it
     * either — Hindi rendering is a known-unreliable case for this model
     * regardless of language setting (see WhisperLanguages.kt). English is the
     * language this app is actually verified to work well for, so it's the
     * default a new user lands on; anyone can pick a different language,
     * including auto-detect, from the picker. An explicit auto-detect choice
     * is stored as [AUTO_DETECT_VALUE] rather than by clearing the key, so it
     * can be told apart from "never chosen" (which falls back to English).
     */
    var transcriptionLanguage: String?
        get() = when (val stored = settings.getStringOrNull(KEY_TRANSCRIPTION_LANGUAGE)) {
            null -> DEFAULT_LANGUAGE
            AUTO_DETECT_VALUE -> null
            else -> stored
        }
        set(value) = settings.putString(KEY_TRANSCRIPTION_LANGUAGE, value ?: AUTO_DETECT_VALUE)

    /**
     * How the transcript reads on Detail: false (default) shows each segment
     * on its own line with a tappable timestamp that seeks playback there;
     * true joins all segments into one continuous block of text with no
     * timestamps, for reading or copying as prose. A per-user reading
     * preference, not tied to any one recording.
     */
    var transcriptContinuousView: Boolean
        get() = settings.getBoolean(KEY_TRANSCRIPT_CONTINUOUS_VIEW, false)
        set(value) = settings.putBoolean(KEY_TRANSCRIPT_CONTINUOUS_VIEW, value)

    private companion object {
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        const val KEY_TRANSCRIPTION_LANGUAGE = "transcription_language"
        const val KEY_TRANSCRIPT_CONTINUOUS_VIEW = "transcript_continuous_view"
        const val DEFAULT_LANGUAGE = "en"
        const val AUTO_DETECT_VALUE = "auto"
    }
}
