# Vaani (working name — see PRD decision D-03)

A recorder that transcribes hour-long audio on a cheap phone, entirely offline
and free. Full product requirements: see the PRD (Claude artifact, linked in
project notes) — this file only covers what's in the repo and what's next.

## Status

Scaffold only. The module structure, data model, DI wiring, and the
`whisper.cpp` boundary interface exist and are meant to compile, but the parts
that need the actual native runtime or platform audio APIs are stubs that
throw `NotImplementedError` on purpose:

- Recording audio (no `MediaRecorder` wiring yet)
- Decoding a window to PCM (`AudioDecoder` — needs `MediaCodec`/`MediaExtractor`)
- Running whisper.cpp itself (`TranscriptionEngine` — needs the NDK build)
- Model download (`ModelRepository.download` — needs a model catalogue + CDN)

Everything else — the library list, the SQLDelight schema, the transcription
job/segment bookkeeping, the window planner — is real and has a real path to
being exercised end to end once the native piece lands.

## Module map

```
androidApp/     Compose UI, ViewModels, Koin wiring, foreground service shell
shared/         KMP: domain models, use cases, repositories, SQLDelight schema
whisper/        KMP: TranscriptionEngine interface; native glue lands here
```

See PRD section 09 for the full architecture rationale (why Koin over Hilt,
SQLDelight over Room, whisper.cpp over TFLite, etc).

## Before this builds

1. **Android SDK / NDK.** `local.properties` points at the SDK already
   installed on this machine. The NDK and CMake aren't installed yet — you
   only need them once the whisper.cpp spike starts wiring native code into
   `:whisper/src/androidMain/cpp`.
2. **Gradle wrapper.** Not committed yet — this repo was scaffolded without a
   local Gradle install. Opening the project in Android Studio will offer to
   generate the wrapper (or use its bundled Gradle) automatically. If you'd
   rather do it from the command line and have Gradle installed, run:
   ```bash
   gradle wrapper --gradle-version 8.10
   ```

## Next step: the spike

Per the PRD's release plan (section 14), the next real milestone is *not* more
scaffolding — it's benchmarking whisper.cpp with a quantised base model on a
**named** budget Android device, before any more product code. That's blocked
on:

- **D-01**: naming the actual reference handset (a purchase decision)
- **D-02**: confirming the quantisation level (Q5_1 assumed) against measured
  size / memory / WER on real audio, including Hinglish

Once those are settled, the `:whisper` module's `cpp/` directory gets the
whisper.cpp submodule, a `CMakeLists.txt`, and the JNI glue implementing the
three `external fun`s in `AndroidTranscriptionEngine`.

## Open, permanent decisions

- **D-03**: product name and package identifier. Renamed from "Murmur" to
  "Vaani" and `com.quietstack.vaani` throughout this codebase after real
  collision research (Play Store search: "Parrot", "Jot" and "Scribe" all had
  direct existing competitors in this exact category; "Vaani" and "Awaaz" did
  not). Still worth a final check before publishing, since the applicationId
  becomes permanent on first Play upload.
