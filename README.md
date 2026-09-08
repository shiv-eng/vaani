# Vaani

A voice recorder for Android that also transcribes what you said — entirely
on the phone, with no account and no cloud. Tap record, and Vaani keeps
capturing audio in the background even if the screen locks or you switch
apps. When you stop, it turns the recording into text automatically using an
on-device speech model (no internet needed after the model's one-time
download).

Every recording is a real audio file first — export it as-is, as plain text,
or as an SRT subtitle file, whether the transcript came out right or not.

## Features

- Record audio of any length, with recording surviving backgrounding/lock
- Automatic on-device transcription (whisper.cpp), with per-segment
  timestamps
- Search across every transcript you've recorded
- Bookmark exact moments in a recording
- Edit a transcript by hand
- Export as audio, plain text, or SRT
- Import audio files shared from other apps
- Pick a transcription language, or let it auto-detect

## Privacy

No accounts, no analytics, no ads. Recordings and transcripts live only in
the app's private storage on-device. The only network call Vaani ever makes
is the one-time HTTPS download of the speech model from Hugging Face's
public file hosting — nothing else is ever sent anywhere.

## Module map

```
androidApp/     Compose UI, ViewModels, Koin wiring, foreground services
shared/         KMP: domain models, use cases, repositories, SQLDelight schema
whisper/        KMP: TranscriptionEngine interface + the whisper.cpp/JNI glue
```

Clean Architecture / MVVM: domain interfaces and use cases live in
`shared/.../domain`, repository implementations in `shared/.../data`, and
presentation (ViewModels + Compose screens) in `androidApp/ui`.

## Building

1. **Android SDK / NDK.** `local.properties` (gitignored) needs `sdk.dir`
   pointing at your local Android SDK. NDK `27.2.12479018` and CMake are
   required for the `:whisper` module's native build.
2. **Gradle wrapper** is committed — `./gradlew.bat :androidApp:assembleDebug`
   (or `./gradlew` on macOS/Linux) builds a debug APK.
3. **Release builds** need a signing config. `androidApp/build.gradle.kts`
   reads `VAANI_UPLOAD_STORE_FILE` / `VAANI_UPLOAD_STORE_PASSWORD` /
   `VAANI_UPLOAD_KEY_ALIAS` / `VAANI_UPLOAD_KEY_PASSWORD` from
   `local.properties` (or equivalent environment variables for CI) — never
   hardcoded, never committed. Without these set, `assembleRelease` still
   builds an unsigned APK.

## Tech stack

Kotlin Multiplatform, Jetpack Compose (Material 3), Koin DI, SQLDelight,
Coroutines/Flow, Media3 (ExoPlayer + MediaSession) for playback, and
whisper.cpp via JNI/NDK for on-device transcription.
