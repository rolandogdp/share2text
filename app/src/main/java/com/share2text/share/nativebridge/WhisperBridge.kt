package com.share2text.share.nativebridge

import android.content.Context
import java.io.File

class WhisperBridge(private val context: Context) {

    companion object {
        init {
            System.loadLibrary("whisper_jni")
        }
    }

    private external fun nativeInit(modelPath: String, threads: Int): Boolean
    private external fun nativeRelease()
    private external fun nativeTranscribeRange(
        wavPath: String,
        startSec: Float,
        endSec: Float,
        language: String?
    ): String

    @Volatile private var initialized = false

    fun init(modelPath: String, threads: Int = Runtime.getRuntime().availableProcessors()): Boolean {
        if (initialized) return true
        initialized = nativeInit(modelPath, threads)
        return initialized
    }

    fun transcribeRange(wavPath: String, start: Float, end: Float, language: String?): String {
        check(initialized) { "Whisper not initialized" }
        return nativeTranscribeRange(wavPath, start, end, language)
    }

    fun release() { nativeRelease(); initialized = false }
}
