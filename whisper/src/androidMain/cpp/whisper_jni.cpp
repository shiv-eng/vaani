#include <jni.h>

#include <algorithm>
#include <android/log.h>
#include <string>
#include <thread>

#include "whisper.h"

#define LOG_TAG "whisper_jni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jlong JNICALL
Java_com_murmur_transcriber_whisper_AndroidTranscriptionEngine_nativeLoadModel(
        JNIEnv *env, jobject /*thiz*/, jstring modelPath) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);

    struct whisper_context_params cparams = whisper_context_default_params();
    struct whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);

    env->ReleaseStringUTFChars(modelPath, path);

    if (ctx == nullptr) {
        LOGE("whisper_init_from_file_with_params failed");
        return 0;
    }
    return reinterpret_cast<jlong>(ctx);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_murmur_transcriber_whisper_AndroidTranscriptionEngine_nativeTranscribe(
        JNIEnv *env, jobject /*thiz*/, jlong handle, jfloatArray pcm, jint /*sampleRate*/,
        jstring language) {
    auto *ctx = reinterpret_cast<struct whisper_context *>(handle);

    jclass segClass = env->FindClass("com/murmur/transcriber/whisper/EngineSegment");
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

    std::string lang;
    if (language != nullptr) {
        const char *langChars = env->GetStringUTFChars(language, nullptr);
        lang = langChars;
        env->ReleaseStringUTFChars(language, langChars);
        wparams.language = lang.c_str();
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

        jstring jtext = env->NewStringUTF(text);
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
Java_com_murmur_transcriber_whisper_AndroidTranscriptionEngine_nativeRelease(
        JNIEnv * /*env*/, jobject /*thiz*/, jlong handle) {
    if (handle == 0) return;
    whisper_free(reinterpret_cast<struct whisper_context *>(handle));
}
