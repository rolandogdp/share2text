package com.share2text.share.repo

import android.content.Context
import android.net.Uri
import com.share2text.share.audio.AudioDecoder
import com.share2text.share.nativebridge.WhisperBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

class TranscriptionRepository(
    private val context: Context,
    private val whisper: WhisperBridge
) {
    data class Result(
        val text: String,
        val modelId: String,
        val language: String?,
        val durationSec: Float,
        val processingMs: Long
    )

    private val _liveText = MutableStateFlow("")
    val liveText: StateFlow<String> = _liveText.asStateFlow()

    suspend fun transcribeUri(
        uri: Uri,
        modelPath: String,
        language: String?,
        threads: Int = Runtime.getRuntime().availableProcessors()
    ): Result = withContext(Dispatchers.IO) {
        whisper.init(modelPath, threads)

        val pcm = AudioDecoder.decodeToPCM16Mono16k(context, uri)
        val wavFile = File.createTempFile("a_", ".wav", context.cacheDir)
        AudioDecoder.writeWav16LE(wavFile, pcm.data, pcm.sampleRate, pcm.channels)

        val samples = pcm.data.size
        val seconds = samples / pcm.sampleRate.toFloat()

        _liveText.value = ""

        val chunkSec = 30f
        var start = 0f
        val sb = StringBuilder()

        val t0 = System.currentTimeMillis()
        while (start < seconds) {
            val end = min(start + chunkSec, seconds)
            val part = whisper.transcribeRange(wavFile.absolutePath, start, end, language)
            if (part.isNotBlank()) {
                sb.append(part.trim()).append('
')
                _liveText.value = sb.toString()
            }
            start = end
        }
        val elapsed = System.currentTimeMillis() - t0
        val out = Result(
            text = sb.toString().trim(),
            modelId = File(modelPath).nameWithoutExtension,
            language = language,
            durationSec = seconds,
            processingMs = elapsed
        )
        wavFile.delete()
        out
    }
}
