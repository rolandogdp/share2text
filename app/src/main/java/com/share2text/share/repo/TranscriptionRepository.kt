package com.share2text.share.repo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Stub methods so code compiles; replace with real implementation later.
    suspend fun initializeModelIfNeeded(): Boolean = true
    suspend fun transcribe(filePath: String): String = "Transcription placeholder"
}
