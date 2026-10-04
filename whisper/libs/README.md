# libs/

## sherpa-onnx-1.13.8.aar

Prebuilt Android AAR from [k2-fsa/sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)
(Apache-2.0). No Maven Central or JitPack artifact exists for this project as
of this writing, so the AAR is committed here directly rather than resolved
as a normal Gradle dependency.

- **Source**: https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/sherpa-onnx-1.13.8.aar
- **SHA-256**: `633c24321e06b1fe79feafa03ea16cbc0f8a286641e2da3559bac91bdb13bd96`
- **Used for**: `IndicConformerEngine` (Hindi transcription) — see
  `whisper/src/androidMain/kotlin/com/quietstack/vaani/whisper/IndicConformerEngine.kt`.

To upgrade: download the new release's AAR from the same releases page,
verify its sha256, replace this file, and update `whisper/build.gradle.kts`'s
`implementation(files(...))` path to match the new filename.
