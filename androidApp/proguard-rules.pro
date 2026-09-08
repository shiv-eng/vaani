# Vaani release ProGuard/R8 rules.
#
# The JNI keep rules below are not precautionary — they fix a real,
# confirmed-by-inspection breakage: whisper_jni.cpp resolves these classes
# and members by string name from native code (JNIEnv::FindClass,
# GetMethodID), which R8 cannot see or trace. Without these rules, a release
# build installs and launches fine but transcription silently breaks
# (NoSuchMethodError / null class lookups deep in native code) — debug builds
# never catch this since minification is off there. Verified by inspecting
# whisper_jni.cpp's actual FindClass/GetMethodID calls, not guessed generically.

# nativeTranscribe (whisper_jni.cpp) does
#   FindClass("com/quietstack/vaani/whisper/EngineSegment")
#   GetMethodID(segClass, "<init>", "(JJLjava/lang/String;)V")
# and constructs one per transcribed segment via NewObject — the class name,
# and its constructor's name/signature, must survive unrenamed.
-keep class com.quietstack.vaani.whisper.EngineSegment {
    <init>(long, long, java.lang.String);
}

# Same function also does GetObjectClass(progressCallback) then
# GetMethodID(..., "onProgress", "(I)V") to report decode progress mid-call —
# ProgressCallback is a private nested class in AndroidTranscriptionEngine,
# exactly the kind of "looks unused, only called reflectively" member R8
# would otherwise strip or rename.
-keep class com.quietstack.vaani.whisper.AndroidTranscriptionEngine$ProgressCallback {
    void onProgress(int);
}

# The native side binds these by their Java-mangled symbol names
# (Java_com_quietstack_vaani_whisper_AndroidTranscriptionEngine_...) baked
# into whisper_jni.cpp at compile time — AGP's default rules already keep
# native methods, but keeping the declaring class explicitly removes any
# doubt about R8 renaming AndroidTranscriptionEngine itself.
-keepclasseswithmembernames,includedescriptorclasses class com.quietstack.vaani.whisper.AndroidTranscriptionEngine {
    native <methods>;
}

# Diagnostic logging (AndroidTranscriptionEngine's Log.i calls) is dev-only
# noise in a release build, not user-facing and not sensitive on its own —
# strips cleanly since R8 can prove these calls have no side effects once
# minification is on.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Standard Kotlin coroutines rule (state-machine fields must survive, or
# suspending calls break) — recommended by kotlinx.coroutines itself for R8.
-keepclassmembers class kotlin.coroutines.Continuation

# SQLDelight, Koin, Media3 and WorkManager all ship their own consumer
# ProGuard rules inside their AARs (merged automatically) — nothing
# additional needed here for them.
