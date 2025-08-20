cmake_minimum_required(VERSION 3.22.1)
project(whisper_jni)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)

# Pull whisper.cpp (pinned)
FetchContent_Declare(
        whispercpp
        GIT_REPOSITORY https://github.com/ggerganov/whisper.cpp.git
        GIT_TAG v1.5.4
)
FetchContent_MakeAvailable(whispercpp)
# ^ This defines the target "whisper" (and its ggml dep) already.

# Our JNI shim
add_library(whisper_jni SHARED
        whisper_jni.cpp
)

if (TARGET whisper)
    # Make sure Accelerate is NOT considered (it's Apple-only).
    # Important: do NOT define GGML_USE_ACCELERATE at all. If it's defined (even =0),
    # ggml uses #if defined(GGML_USE_ACCELERATE) and will include Accelerate headers.
    target_compile_options(whisper PRIVATE -UGGML_USE_ACCELERATE)

    # Optional: explicitly turn off OpenMP for Android builds.
    target_compile_definitions(whisper PRIVATE GGML_USE_OPENMP=0)
endif()

# Include dirs if you need headers directly
if (whispercpp_SOURCE_DIR)
    target_include_directories(whisper_jni PRIVATE
            ${whispercpp_SOURCE_DIR}
            ${whispercpp_SOURCE_DIR}/examples
    )
endif()

# Link against the whisper target that FetchContent created
target_link_libraries(whisper_jni
        whisper        # <- use existing target; DO NOT add_library(whisper …) yourself
        android
        log
        atomic
)
