# Vaani

<a href="https://play.google.com/store/apps/details?id=com.quietstack.vaani"><img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="80"></a>

A voice recorder for Android that also transcribes what you said — entirely
on the phone, with no account and no cloud. Tap record, and Vaani keeps
capturing audio in the background even if the screen locks or you switch
apps. When you stop, it turns the recording into text automatically using
on-device speech models (no internet needed after the one-time model
download).

Hindi is transcribed in Devanagari with AI4Bharat's IndicConformer; English
and other languages use OpenAI's Whisper. Every recording is a real audio
file first — export it as-is, as plain text, or as an SRT subtitle file,
whether the transcript came out right or not.

## Features

- Record audio of any length, with recording surviving backgrounding/lock
- Automatic on-device transcription with per-segment timestamps
- One-tap English / हिंदी switch on the recording screen, plus 90+ other
  languages through Whisper
- Search across every transcript you've recorded
- Bookmark exact moments in a recording
- Edit a transcript by hand
- Export as audio, plain text, or SRT
- Import audio files shared from other apps
- Short first-run walkthrough

## How transcription is routed

Whisper alone was unreliable for Hindi: it rendered Hindi in Urdu's script and
collapsed Hindi/English code-switching into English. Forcing an English
language token on Hindi speech made it translate rather than transcribe. So
the app picks the engine from the selected language:

- `hi` → IndicConformer (ONNX, int8) running on sherpa-onnx
- everything else → whisper.cpp (Q5_1 small model) through JNI

The routing lives in `RoutingTranscriptionEngine` in the `:whisper` module;
nothing above it knows a second engine exists.

## Privacy

No accounts, no analytics, no ads. Recordings and transcripts live only in
the app's private storage on-device. The only network use Vaani makes is the
one-time HTTPS download of the speech models (about 370 MB: the Whisper model
plus the Hindi model and its token list) from Hugging Face's public file
hosting — nothing else is ever sent anywhere.

## Module map

```
androidApp/     Compose UI, ViewModels, Koin wiring, foreground services
shared/         KMP: domain models, use cases, repositories, SQLDelight schema
whisper/        KMP: TranscriptionEngine interface, whisper.cpp/JNI glue,
                and the IndicConformer engine + language router
```

Clean Architecture / MVVM: domain interfaces and use cases live in
`shared/.../domain`, repository implementations in `shared/.../data`, and
presentation (ViewModels + Compose screens) in `androidApp/ui`.

## Building

1. **Android SDK / NDK.** `local.properties` (gitignored) needs `sdk.dir`
   pointing at your local Android SDK. NDK `27.2.12479018` and CMake are
   required for the `:whisper` module's native build.
2. **sherpa-onnx.** The prebuilt AAR is committed in `whisper/libs/` (see the
   README there for its source and checksum) and consumed through a `flatDir`
   repository in `settings.gradle.kts`.
3. **Gradle wrapper** is committed — `./gradlew.bat :androidApp:assembleDebug`
   (or `./gradlew` on macOS/Linux) builds a debug APK.
4. **Release builds** need a signing config. `androidApp/build.gradle.kts`
   reads `VAANI_UPLOAD_STORE_FILE` / `VAANI_UPLOAD_STORE_PASSWORD` /
   `VAANI_UPLOAD_KEY_ALIAS` / `VAANI_UPLOAD_KEY_PASSWORD` from
   `local.properties` (or equivalent environment variables for CI) — never
   hardcoded, never committed. `bundleRelease` produces the signed AAB for
   Play. The release build is R8-shrunk; the JNI keep rules for whisper.cpp
   and sherpa-onnx are in `androidApp/proguard-rules.pro`.

## Tech stack

Kotlin Multiplatform, Jetpack Compose (Material 3), Koin DI, SQLDelight,
Coroutines/Flow, Media3 (ExoPlayer + MediaSession) for playback, whisper.cpp
via JNI/NDK, and sherpa-onnx (ONNX Runtime) running AI4Bharat IndicConformer.

## Credits

whisper.cpp and OpenAI Whisper, AI4Bharat IndicConformer, and
[sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx).
