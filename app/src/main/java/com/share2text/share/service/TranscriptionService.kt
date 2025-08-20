package com.share2text.share.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.share2text.share.R
import com.share2text.share.repo.TranscriptionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class TranscriptionService : LifecycleService() {

    @Inject lateinit var repo: TranscriptionRepository

    private val binder = LocalBinder()
    private var job: Job? = null

    data class State(
        val text: String = "",
        val progress: Int = 0,
        val done: Boolean = false,
        val error: String? = null
    )

    private val channelId = "transcribe"
    private val notifId = 1002

    inner class LocalBinder : Binder() {
        fun service(): TranscriptionService = this@TranscriptionService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    private fun createChannel() {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(channelId, getString(R.string.notify_channel_transcribe),
            NotificationManager.IMPORTANCE_LOW
        )
        mgr.createNotificationChannel(ch)
    }

    fun startTranscription(uri: Uri, modelPath: String, language: String?, threads: Int, onUpdate: (State) -> Unit) {
        val n = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(getString(R.string.notify_transcribing))
            .setContentText("Preparing…")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        startForeground(notifId, n)

        job?.cancel()
        job = lifecycleScope.launch(Dispatchers.IO) {
            try {
                var lastLen = 0
                val collectJob = launch(Dispatchers.Main) {
                    repo.liveText.collect { t ->
                        val prog = if (t.length > 0) (t.length % 90) + 10 else 5
                        onUpdate(State(text = t, progress = prog))
                        updateNotification("Transcribing…", prog)
                    }
                }
                val res = repo.transcribeUri(uri, modelPath, language, threads)
                collectJob.cancel()
                onUpdate(State(text = res.text, progress = 100, done = true))
                updateNotification("Done", 100, done = true)
                stopForeground(STOP_FOREGROUND_DETACH)
            } catch (t: Throwable) {
                onUpdate(State(error = t.message ?: "Error"))
                updateNotification("Error", 0, done = true)
                stopForeground(STOP_FOREGROUND_DETACH)
            }
        }
    }

    private fun updateNotification(text: String, progress: Int, done: Boolean = false) {
        val n = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(getString(R.string.notify_transcribing))
            .setContentText(text)
            .setOngoing(!done)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, progress <= 0)
            .build()
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(notifId, n)
    }
}
