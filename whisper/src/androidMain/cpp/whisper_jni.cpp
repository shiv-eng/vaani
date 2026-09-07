#include <jni.h>

#include <algorithm>
#include <android/log.h>
#include <cstdint>
#include <cstring>
#include <string>
#include <thread>

#include "whisper.h"

#define LOG_TAG "whisper_jni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// whisper.cpp occasionally emits a segment with a malformed UTF-8 byte
// sequence, or a valid 4-byte UTF-8 sequence (a character outside the Basic
// Multilingual Plane). env->NewStringUTF requires Java's "Modified UTF-8",
// which is stricter than standard UTF-8: it has no 4-byte form at all (a
// supplementary-plane character must be split into a UTF-16 surrogate pair
// first) and aborts the whole process — not just the call — on anything that
// doesn't fit. Decoding into UTF-16 ourselves and constructing the jstring
// via NewString (which takes UTF-16 code units directly) sidesteps Modified
// UTF-8 entirely instead of hoping every input already satisfies it.
static std::u16string utf8ToUtf16(const char *input) {
    std::u16string out;
    const auto *bytes = reinterpret_cast<const unsigned char *>(input);
    size_t len = std::strlen(input);
    size_t i = 0;
    while (i < len) {
        unsigned char b0 = bytes[i];
        uint32_t codepoint;
        int extra;
        if (b0 < 0x80) {
            out.push_back(static_cast<char16_t>(b0));
            i++;
            continue;
        } else if ((b0 & 0xE0) == 0xC0) {
            codepoint = b0 & 0x1F;
            extra = 1;
        } else if ((b0 & 0xF0) == 0xE0) {
            codepoint = b0 & 0x0F;
            extra = 2;
        } else if ((b0 & 0xF8) == 0xF0) {
            codepoint = b0 & 0x07;
            extra = 3;
        } else {
            out.push_back(0xFFFD);
            i++;
            continue;
        }

        if (i + extra >= len) {
            out.push_back(0xFFFD);
            i++;
            continue;
        }

        bool valid = true;
        uint32_t cp = codepoint;
        for (int k = 1; k <= extra; k++) {
            unsigned char bk = bytes[i + k];
            if ((bk & 0xC0) != 0x80) {
                valid = false;
                break;
            }
            cp = (cp << 6) | (bk & 0x3F);
        }
        if (!valid) {
            out.push_back(0xFFFD);
            i++;
            continue;
        }
        i += extra + 1;

        if (cp >= 0xD800 && cp <= 0xDFFF) {
            out.push_back(0xFFFD); // lone surrogate value: not a valid scalar
        } else if (cp <= 0xFFFF) {
            out.push_back(static_cast<char16_t>(cp));
        } else if (cp <= 0x10FFFF) {
            uint32_t v = cp - 0x10000;
            out.push_back(static_cast<char16_t>(0xD800 + (v >> 10)));
            out.push_back(static_cast<char16_t>(0xDC00 + (v & 0x3FF)));
        } else {
            out.push_back(0xFFFD);
        }
    }
    return out;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_quietstack_vaani_whisper_AndroidTranscriptionEngine_nativeLoadModel(
        JNIEnv *env, jobject /*thiz*/, jstring modelPath) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);

    struct whisper_context_params cparams = whisper_context_default_params();
    // No GPU backend is built for this Android target; make that explicit
    // rather than relying on whisper.cpp probing for one and falling back.
    cparams.use_gpu = false;
    struct whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);

    env->ReleaseStringUTFChars(modelPath, path);

    if (ctx == nullptr) {
        LOGE("whisper_init_from_file_with_params failed");
        return 0;
    }
    return reinterpret_cast<jlong>(ctx);
}

namespace {
struct ProgressUserData {
    JNIEnv *env;
    jobject callback;
    jmethodID method;
};

// whisper_full's main loop calls this on the same thread that called
// whisper_full (see src/whisper.cpp) — never a ggml worker thread — so the
// JNIEnv captured for the nativeTranscribe call stays valid here.
void onWhisperProgress(struct whisper_context * /*ctx*/, struct whisper_state * /*state*/,
                        int progress, void *user_data) {
    auto *data = reinterpret_cast<ProgressUserData *>(user_data);
    data->env->CallVoidMethod(data->callback, data->method, (jint) progress);
}
}  // namespace

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_quietstack_vaani_whisper_AndroidTranscriptionEngine_nativeTranscribe(
        JNIEnv *env, jobject /*thiz*/, jlong handle, jfloatArray pcm, jint /*sampleRate*/,
        jstring language, jobject progressCallback) {
    auto *ctx = reinterpret_cast<struct whisper_context *>(handle);

    jclass segClass = env->FindClass("com/quietstack/vaani/whisper/EngineSegment");
    jmethodID ctor = env->GetMethodID(segClass, "<init>", "(JJLjava/lang/String;)V");

    if (ctx == nullptr) {
        LOGE("nativeTranscribe called with a null context");
        return env->NewObjectArray(0, segClass, nullptr);
    }

    jsize numSamples = env->GetArrayLength(pcm);
    jfloat *samples = env->GetFloatArrayElements(pcm, nullptr);

    // Q5_1 base on a 4GB device has no cores to spare for anything else running
    // concurrently, so this deliberately leaves one core for the rest of the
    // system rather than claiming every core (D-04/section 11 territory —
    // revisit once the reference device is named and this is measured).
    int threads = std::max(1, (int) std::thread::hardware_concurrency() - 1);

    whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wparams.print_progress = false;
    wparams.print_special = false;
    wparams.print_realtime = false;
    wparams.print_timestamps = false;
    wparams.single_segment = false;
    wparams.n_threads = threads;

    jclass progressClass = env->GetObjectClass(progressCallback);
    jmethodID progressMethod = env->GetMethodID(progressClass, "onProgress", "(I)V");
    ProgressUserData progressData{env, progressCallback, progressMethod};
    wparams.progress_callback = onWhisperProgress;
    wparams.progress_callback_user_data = &progressData;

    std::string lang;
    std::string initialPrompt;
    if (language != nullptr) {
        const char *langChars = env->GetStringUTFChars(language, nullptr);
        lang = langChars;
        env->ReleaseStringUTFChars(language, langChars);
        wparams.language = lang.c_str();

        // Hindi and Urdu are the same spoken language in different scripts,
        // and Whisper's training data blurs the two — forcing language="hi"
        // alone doesn't reliably stop it from decoding in Urdu's script.
        // Seeding a short Devanagari-script Hindi prompt biases the decoder
        // toward continuing in that script from the very first token, the
        // standard workaround for this specific, well-documented Whisper
        // limitation. Not a guaranteed fix — a training-data ambiguity like
        // this doesn't have one — but a real, evidence-based lever neither
        // language-forcing nor a bigger model fully closed on their own.
        if (lang == "hi") {
            initialPrompt = "नमस्ते, आज मैं आपसे हिंदी में बात कर रहा हूँ।";
            wparams.initial_prompt = initialPrompt.c_str();
        }
    } else {
        wparams.language = nullptr; // auto-detect
    }

    int result = whisper_full(ctx, wparams, samples, numSamples);
    env->ReleaseFloatArrayElements(pcm, samples, JNI_ABORT);

    if (result != 0) {
        LOGE("whisper_full returned %d", result);
        env->DeleteLocalRef(segClass);
        return env->NewObjectArray(0, segClass, nullptr);
    }

    int nSegments = whisper_full_n_segments(ctx);
    jobjectArray out = env->NewObjectArray(nSegments, segClass, nullptr);

    for (int i = 0; i < nSegments; i++) {
        // whisper reports timestamps in centiseconds; EngineSegment wants ms,
        // relative to the window (StartTranscriptionUseCase shifts to file time).
        int64_t startMs = whisper_full_get_segment_t0(ctx, i) * 10;
        int64_t endMs = whisper_full_get_segment_t1(ctx, i) * 10;
        const char *text = whisper_full_get_segment_text(ctx, i);
        std::u16string utf16Text = utf8ToUtf16(text);

        jstring jtext = env->NewString(reinterpret_cast<const jchar *>(utf16Text.data()), utf16Text.size());
        jobject seg = env->NewObject(segClass, ctor, (jlong) startMs, (jlong) endMs, jtext);
        env->SetObjectArrayElement(out, i, seg);
        env->DeleteLocalRef(seg);
        env->DeleteLocalRef(jtext);
    }

    env->DeleteLocalRef(segClass);
    return out;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_quietstack_vaani_whisper_AndroidTranscriptionEngine_nativeRelease(
        JNIEnv * /*env*/, jobject /*thiz*/, jlong handle) {
    if (handle == 0) return;
    whisper_free(reinterpret_cast<struct whisper_context *>(handle));
}
