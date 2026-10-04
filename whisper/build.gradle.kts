import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

// See :shared build.gradle.kts for why ios targets are gated on the build host.
val isMacOs = System.getProperty("os.name").contains("Mac", ignoreCase = true)

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    if (isMacOs) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }

        // sherpa-onnx has no Maven/JitPack artifact — this AAR is downloaded
        // from its GitHub release and committed to libs/ (unlike the *.bin
        // model weights, this is compiled code the build needs, not a large
        // data file fetched at runtime). See libs/README.md for the exact
        // release, URL and sha256 this came from.
        androidMain.dependencies {
            implementation("com.k2fsa:sherpa-onnx:1.13.8@aar")
        }

        // Spike-only: exercises the JNI boundary directly on a device/emulator,
        // bypassing AudioDecoder/recording (still stubs) with a bundled WAV file.
        // Not part of the app or meant to stay long-term — see PRD section 14.
        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.junit)
        }

        if (isMacOs) {
            val iosX64Main by getting
            val iosArm64Main by getting
            val iosSimulatorArm64Main by getting
            val iosMain by creating {
                dependsOn(commonMain.get())
                iosX64Main.dependsOn(this)
                iosArm64Main.dependsOn(this)
                iosSimulatorArm64Main.dependsOn(this)
            }
        }
    }
}

android {
    namespace = "com.quietstack.vaani.whisper"
    compileSdk = 36
    ndkVersion = "27.2.12479018"

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // x86_64 is here only so the spike can run on the Android Studio emulator
        // before a physical reference device (D-01) is named. Narrow this to the
        // ABIs the reference device actually needs once it's picked.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                // CMake has no optimization flags by default; Gradle's "debug" Android
                // build type does not imply an optimized native build type. Without
                // this, ggml's matrix-multiply kernels run at -O0 — the spike's first
                // run at that setting measured a 104x real-time factor on the emulator,
                // entirely an artifact of this flag, not a signal about whisper.cpp itself.
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DCMAKE_BUILD_TYPE=Release",
                )
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/androidMain/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
