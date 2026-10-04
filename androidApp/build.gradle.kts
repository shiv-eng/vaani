import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
}

// Release signing reads from local.properties first (gitignored — see that
// file's own comment for where the real keystore/credentials live), falling
// back to environment variables of the same name for CI, where there's no
// local.properties at all. Never hardcoded, per the launch checklist.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingProp(key: String): String? = localProps.getProperty(key) ?: System.getenv(key)

android {
    namespace = "com.quietstack.vaani"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.quietstack.vaani" // placeholder — see PRD decision D-03, permanent once published
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "0.2.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = signingProp("VAANI_UPLOAD_STORE_FILE")
            // Only configured when the credentials are actually present, so a
            // debug-only checkout (no local.properties signing entries, no env
            // vars) still configures and builds fine — just can't assemble a
            // signed release.
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = signingProp("VAANI_UPLOAD_STORE_PASSWORD")
                keyAlias = signingProp("VAANI_UPLOAD_KEY_ALIAS")
                keyPassword = signingProp("VAANI_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    packaging {
        resources.excludes.add("META-INF/*")
    }

    bundle {
        language { enableSplit = true }
        density { enableSplit = true }
        abi { enableSplit = true }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":whisper"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
}
