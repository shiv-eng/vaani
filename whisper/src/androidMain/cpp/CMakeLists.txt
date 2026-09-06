cmake_minimum_required(VERSION 3.22)
project(whisper_jni)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Static-link whisper/ggml into whisper_jni.so rather than shipping a second .so —
# one library to load, one ABI boundary to reason about.
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)

add_subdirectory(whisper.cpp)

add_library(whisper_jni SHARED
    whisper_jni.cpp
)

target_link_libraries(whisper_jni
    PRIVATE
        whisper
        android
        log
)
