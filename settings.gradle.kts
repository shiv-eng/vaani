pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // sherpa-onnx's AAR (see whisper/libs/README.md). A flatDir module
        // rather than files(...): AGP refuses a direct local .aar dependency
        // in a library module once it builds a release AAR.
        flatDir { dirs("whisper/libs") }
    }
}

rootProject.name = "vaani"

include(":androidApp")
include(":shared")
include(":whisper")
