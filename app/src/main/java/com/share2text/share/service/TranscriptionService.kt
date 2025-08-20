package com.share2text.share.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.share2text.share.R
import com.share2text.share.repo.TranscriptionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TranscriptionService : Service() {

    @Inject lateinit var repo: TranscriptionRepository

    // Expose the service instance via a property (not a function)
    inner class LocalBinder : Binder() {
        val service: TranscriptionService
            get() = this@TranscriptionService
    }
    private val binder = LocalBinder()

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var currentJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification("Idle"))
    }

    override fun onBind(intent: Intent?): IBinder = binder

    // DTO that matches what TranscriptionScreen expects in the callback
    data class Update(
        val text: String,
        val progress: Int,     // 0..100
        val done: Boolean,
        val error: String? = null
    )

    /**
     * Starts a transcription job. Replace the stub body with your real pipeline.
     */
    fun startTranscription(
        uri: Uri,
        modelPath: String,
        language: String?,
        threads: Int,
        onUpdate: (Update) -> Unit
    ) {
        // Cancel any running job
        currentJob?.cancel()

        currentJob = serviceScope.launch {
            try {
                // TODO: call into repo to ensure model ready, decode audio, and run whisper.cpp
                onUpdate(Update(text = "", progress = 1, done = false, error = null))
                updateNotification("Preparing…")

                // --- Stubbed progress demo; replace with real progress from JNI/whisper.cpp ---
                for (p in listOf(5, 15, 35, 60, 80, 95)) {
                    onUpdate(Update(text = "…processing ($p%)", progress = p, done = false))
                    updateNotification("Transcribing… $p%")
                    kotlinx.coroutines.delay(200L)
                }

                // Final result (stub)
                val finalText = "Transcription placeholder"
                onUpdate(Update(text = finalText, progress = 100, done = true, error = null))
                updateNotification("Done")
            } catch (t: Throwable) {
                onUpdate(Update(text = "", progress = 0, done = true, error = t.message))
                updateNotification("Failed")
            }
        }
    }

    private fun buildNotification(content: String): Notification {
        val channelId = "transcription"
        val mgr = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(
                channelId, "Transcription", NotificationManager.IMPORTANCE_LOW
            )
            mgr?.createNotificationChannel(ch)
        }
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification_dot)
            .setOngoing(true)
            .setContentTitle("Share2Text")
            .setContentText(content)
            .build()
    }

    private fun updateNotification(content: String) {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr?.notify(NOTIF_ID, buildNotification(content))
    }

    companion object {
        private const val NOTIF_ID = 42
    }
}
