# Spike results — 2026-09-05

Per PRD section 14: benchmark whisper.cpp with a quantised base model before
writing more product code. This is that benchmark. **None of these numbers
are from the PRD's reference device (D-01) — that device hasn't been named or
purchased yet.** They're from whatever hardware was available today. Treat
everything here as "the pipeline works and is in the right ballpark," not as
launch-ready figures.

## Devices used

| Device | ABI | RAM | Role |
|---|---|---|---|
| Android Studio emulator (`sdk_gphone64_x86_64`, API 35) | x86_64 | 2 GB (AVD config) | Proves the JNI/Gradle/CMake wiring builds and runs at all |
| OnePlus CPH2613, Android 16 | arm64-v8a, 8 cores | ~7.4 GB | Real ARM silicon with actual NEON — **not** the 4 GB budget target from the PRD, but the only physical device on hand |

D-01 still needs a real ~4 GB budget phone purchased and named. Every number
below will be worse on that device than on the OnePlus.

## Build: whisper.cpp via NDK/CMake — works

`:whisper` module builds `libwhisper_jni.so` for arm64-v8a, armeabi-v7a and
x86_64 via CMake + NDK 27.2.12479018, statically linking whisper.cpp/ggml
(`whisper/src/androidMain/cpp/CMakeLists.txt`). Zero build errors. This was
the PRD's top risk ("NDK build complexity slows delivery") — retired.

## Quantisation (D-02): Q5_1 confirmed as the starting choice

Quantised with whisper.cpp's own `quantize` tool (built standalone via the
same NDK toolchain, run on-device — no host C++ toolchain was available on
this machine).

| | Float (base, multilingual) | Q5_1 |
|---|---|---|
| File size | 141.1 MiB (147,951,465 bytes) | 56.9 MiB (59,707,625 bytes) — **2.5x smaller** |
| Model size in RAM (reported by whisper.cpp) | 147.4 MB | 59.1 MB |

## Accuracy: no observed loss on one sample

Both models transcribed the standard whisper.cpp test clip (`samples/jfk.wav`,
11.0s, clean English speech) identically, word for word:

> "And so my fellow Americans, ask not what your country can do for you, ask what you can do for your country."

This is **one short, clean, English-only clip** — it says nothing about Q5_1's
accuracy on real recordings, noisy audio, or (critically) Hinglish. The
Hinglish validation the PRD calls for (section 10, "Language strategy") is
still outstanding — it needs a real Hindi/English code-switched recording,
which nobody has supplied yet.

## Speed: comfortably faster than real-time on the OnePlus, not yet on the emulator

Real end-to-end transcription of the 11.0s clip via `whisper-cli`, on the
OnePlus, 4 threads:

| Model | Total time | Real-time factor |
|---|---|---|
| Float | 8.80s | 0.80x (1.25x faster than real-time) |
| Q5_1 | 5.00s | 0.46x (2.2x faster than real-time) |

`whisper-bench`'s synthetic 30s-encoder pass on the OnePlus (encode time only,
not a full transcription) shows the same direction: 5.96s float vs 2.31s
Q5_1 — roughly a 2.6x speedup from quantisation, tracking the 2.5x size drop.

On the x86_64 emulator, the same synthetic bench took 18.9s (float) for the
encoder alone with a real-time factor around 11x when run through the actual
JNI path in `WhisperEngineSpikeTest` — but that number is an artifact of
emulated x86_64 without native SIMD tuning, not a signal about real hardware.
**Don't use the emulator number for anything.** The OnePlus number is the
first real one, and it's still not the target device.

## What this settles

- Runtime choice (whisper.cpp via NDK/CMake): works, no changes needed.
- Quantisation (Q5_1): confirmed as the shipping default. 2.5x smaller, ~2x
  faster, no observed accuracy loss on the one sample tested.
- JNI boundary (`TranscriptionEngine` in `:whisper`): correct — loads a model,
  runs inference, returns segments with plausible text, releases cleanly.

## What's still open

- **D-01**: name and buy the actual ~4 GB reference device. Nothing above
  substitutes for it — a mid-range 2026 OnePlus is not a cheap phone.
- **Hinglish validation**: needs a real code-switched recording. This is the
  PRD's stated differentiator and the biggest unvalidated risk in the
  document (section 16: "positioning-ending" if it fails).
- **Peak RAM under load, thermal behaviour, battery drain**: not measured.
  `whisper-bench`'s reported "model size" figures are a floor, not a peak —
  the PRD's actual metric (section 11) needs profiling on-device during a
  long file, which needs the reference device to be meaningful.
- **Two-hour-file behaviour**: only an 11-second clip has been tested. Window
  planning (`WindowPlanner`, already in `:shared`) hasn't been exercised
  end-to-end with the real decoder yet, since `AudioDecoder` is still a stub.
