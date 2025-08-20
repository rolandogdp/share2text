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

# If you want to force some ggml/whisper defines, do it on the existing target:
if (TARGET whisper)
    # Disable Apple Accelerate, OpenMP etc. as needed for Android
    target_compile_definitions(whisper PRIVATE
            GGML_USE_ACCELERATE=0
            GGML_USE_OPENMP=0
    )
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
