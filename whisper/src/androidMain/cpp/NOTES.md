# Native sources land here during the spike

This directory is where the whisper.cpp git submodule and the JNI glue
(CMakeLists.txt, `whisper_jni.cpp`) get added, per PRD section 09's module map
and section 14's spike stage. Nothing here yet — get whisper.cpp building and
running on the reference device before any product code, as the PRD's risk
table (section 16) calls this the highest-uncertainty piece.

Planned layout:

```
cpp/
├── CMakeLists.txt
├── whisper_jni.cpp        # JNI glue implementing AndroidTranscriptionEngine's externals
└── whisper.cpp/           # git submodule, gitignored from this repo's history
```
