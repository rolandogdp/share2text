package com.share2text.share.audio

import android.media.*
import android.net.Uri
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.roundToInt

object AudioDecoder {

    data class PCM(val data: ShortArray, val sampleRate: Int, val channels: Int)

    suspend fun decodeToPCM16Mono16k(context: Context, uri: Uri): PCM = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)
        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                trackIndex = i
                format = f
                break
            }
        }
        if (trackIndex == -1 || format == null) throw IllegalArgumentException("No audio track found")
        extractor.selectTrack(trackIndex)

        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val outData = ArrayList<Short>()
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        var srcSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        while (!outputDone) {
            if (!inputDone) {
                val inputBufIndex = codec.dequeueInputBuffer(10000)
                if (inputBufIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufIndex)!!
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        val presentationTimeUs = extractor.sampleTime
                        codec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, 0)
                        extractor.advance()
                    }
                }
            }

            val outputBufIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            when {
                outputBufIndex >= 0 -> {
                    val outputBuffer = codec.getOutputBuffer(outputBufIndex)!!
                    val chunk = ByteArray(bufferInfo.size)
                    outputBuffer.get(chunk)
                    outputBuffer.clear()

                    // Convert bytes to 16-bit PCM if needed
                    // Most decoders output PCM 16-bit if not using raw format; assume 16-bit here.
                    val shorts = ShortArray(chunk.size / 2)
                    ByteBuffer.wrap(chunk).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)

                    // Downmix to mono if needed
                    if (channels > 1) {
                        var i = 0
                        while (i < shorts.size) {
                            var sum = 0
                            for (c in 0 until channels) {
                                sum += shorts[i + c].toInt()
                            }
                            outData.add((sum / channels).toShort())
                            i += channels
                        }
                    } else {
                        for (s in shorts) outData.add(s)
                    }

                    codec.releaseOutputBuffer(outputBufIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                }
                outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val newFormat = codec.outputFormat
                    srcSampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    Timber.d("Output format changed: $newFormat")
                }
                outputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> { /* no-op */ }
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        val pcmMono = outData.toShortArray()
        if (srcSampleRate == 16000) {
            PCM(pcmMono, 16000, 1)
        } else {
            val resampled = resampleLinear(pcmMono, srcSampleRate, 16000)
            PCM(resampled, 16000, 1)
        }
    }

    private fun resampleLinear(input: ShortArray, srcRate: Int, dstRate: Int): ShortArray {
        val ratio = dstRate.toDouble() / srcRate
        val outLen = (input.size * ratio).roundToInt()
        val out = ShortArray(outLen)
        var srcPos = 0.0
        for (i in 0 until outLen) {
            val p = srcPos
            val i0 = p.toInt()
            val frac = p - i0
            val s0 = input.getOrElse(i0) { 0 }
            val s1 = input.getOrElse(i0 + 1) { 0 }
            out[i] = ((s0 * (1 - frac) + s1 * frac)).toInt().toShort()
            srcPos += 1.0 / ratio
        }
        return out
    }

    fun writeWav16LE(file: File, data: ShortArray, sampleRate: Int = 16000, channels: Int = 1) {
        val byteData = ByteArray(data.size * 2)
        var idx = 0
        for (s in data) {
            byteData[idx++] = (s.toInt() and 0xff).toByte()
            byteData[idx++] = ((s.toInt() shr 8) and 0xff).toByte()
        }
        val totalDataLen = 36 + byteData.size
        val byteRate = sampleRate * channels * 2
        file.outputStream().use { out ->
            // RIFF header
            out.write("RIFF".toByteArray())
            out.write(intToLE(totalDataLen))
            out.write("WAVE".toByteArray())
            // fmt chunk
            out.write("fmt ".toByteArray())
            out.write(intToLE(16)) // PCM chunk size
            out.write(shortToLE(1)) // PCM format
            out.write(shortToLE(channels.toShort()))
            out.write(intToLE(sampleRate))
            out.write(intToLE(byteRate))
            out.write(shortToLE((channels * 2).toShort())) // block align
            out.write(shortToLE(16)) // bits per sample
            // data chunk
            out.write("data".toByteArray())
            out.write(intToLE(byteData.size))
            out.write(byteData)
        }
    }

    private fun intToLE(v: Int): ByteArray = byteArrayOf(
        (v and 0xff).toByte(),
        ((v shr 8) and 0xff).toByte(),
        ((v shr 16) and 0xff).toByte(),
        ((v shr 24) and 0xff).toByte(),
    )

    private fun shortToLE(v: Short): ByteArray = byteArrayOf(
        (v.toInt() and 0xff).toByte(),
        ((v.toInt() shr 8) and 0xff).toByte(),
    )
}
