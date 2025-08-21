package com.share2text.share.download

import android.app.Notification
import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.asFlow
import androidx.work.*
import com.share2text.share.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.io.File
import java.security.MessageDigest

class ModelRepository(
    private val context: Context,
    private val client: OkHttpClient
) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
    private val activeKey = stringPreferencesKey("active_model")

    @Serializable
    data class ModelPreset(
        val id: String,
        val displayName: String,
        val sizeMB: Int,
        val url: String,
        val sha256: String
    )

    val presets = listOf(
        ModelPreset(
            id = "tiny",
            displayName = "tiny (256MB)",
            sizeMB = 256,
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
            sha256 = "3b85a86d93e8d4bc8b3bfc34a9f6f3f0c7c7aef9d4d86a5f5b76d3b0a1d0f5c7"
        ),
        ModelPreset(
            id = "base",
            displayName = "base (512MB)",
            sizeMB = 512,
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
            sha256 = "66b45a28b68e4d3d3d9b66df7c2a5c1f1b5d37b3c2b2a4ad44525fbb5c1c2a89"
        ),
        ModelPreset(
            id = "small",
            displayName = "small (1.3GB)",
            sizeMB = 1300,
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",
            sha256 = "6f25e3fb376d4caa9d38c8925c403e0b3a8c4027b151cfdb0a8d2f23d0b4aaee"
        ),
        ModelPreset(
            id = "medium",
            displayName = "medium (2.6GB)",
            sizeMB = 2600,
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium.bin",
            sha256 = "b9fca6cd3a9c90c1d4a8f1b9a6c3e1d04e73067f0a8d2d5a116fcde78f2e6d5c"
        ),
        ModelPreset(
            id = "large-v3",
            displayName = "large-v3 (3.8GB)",
            sizeMB = 3800,
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3.bin",
            sha256 = "0b5660d5b3c9d3f2a2d0f48a89ed7cb4d7b0d60a329fbbf2a7b0a111a8c2e3b2"
        )
    )

    fun modelsDir(): File = modelsDir

    fun fileForPreset(p: ModelPreset): File = File(modelsDir, "${p.id}.bin")

    fun isDownloaded(p: ModelPreset): Boolean = fileForPreset(p).exists()

    suspend fun setActiveModel(dataStore: DataStore<Preferences>, id: String) {
        dataStore.edit { it[activeKey] = id }
    }

    fun activeModelFlow(dataStore: DataStore<Preferences>): Flow<String?> =
        dataStore.data.map { it[activeKey] }

    fun enqueueDownload(preset: ModelPreset, wifiOnly: Boolean): String {
        val req = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(
                workDataOf(
                    "url" to preset.url,
                    "dest" to fileForPreset(preset).absolutePath,
                    "sha256" to preset.sha256,
                    "displayName" to preset.displayName
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(
                        if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
                    ).build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "download-${preset.id}",
            ExistingWorkPolicy.KEEP,
            req
        )
        return req.id.toString()
    }

    data class DownloadStatus(val state: WorkInfo.State?, val progress: Int)

    fun downloadStatus(p: ModelPreset): Flow<DownloadStatus> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData("download-${p.id}")
            .asFlow()
            .map { infos ->
                val info = infos.firstOrNull()
                val progress = info?.progress?.getInt("progress", -1) ?: -1
                DownloadStatus(info?.state, progress)
            }
    }
}

class ModelDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val client = OkHttpClient()

    private fun notification(progress: Int, text: String): Notification {
        return NotificationCompat.Builder(applicationContext, "downloads")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(applicationContext.getString(R.string.notify_downloading))
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, progress, progress < 0)
            .build()
    }

    override suspend fun doWork(): Result {
        val url = inputData.getString("url") ?: return Result.failure()
        val destPath = inputData.getString("dest") ?: return Result.failure()
        val expectHash = inputData.getString("sha256") ?: return Result.failure()
        val displayName = inputData.getString("displayName") ?: "model"

        val dest = File(destPath)
        dest.parentFile?.mkdirs()

        // Progress notify
        NotificationManagerCompat.from(applicationContext).notify(
            id.hashCode(), notification(0, "Starting $displayName")
        )

        try {
            val tmp = File(dest.absolutePath + ".part")
            var downloaded = if (tmp.exists()) tmp.length() else 0L

            val reqBuilder = okhttp3.Request.Builder().url(url)
            if (downloaded > 0) {
                reqBuilder.header("Range", "bytes=$downloaded-")
            }
            val call = client.newCall(reqBuilder.build())
            val resp = call.execute()
            if (!resp.isSuccessful) return Result.retry()

            val body = resp.body ?: return Result.retry()
            val total = (resp.headers["Content-Length"]?.toLongOrNull() ?: -1L) + downloaded

            tmp.outputStream().use { out ->
                body.byteStream().use { inp ->
                    val buf = ByteArray(8192)
                    var read: Int
                    var last = System.currentTimeMillis()
                    while (inp.read(buf).also { read = it } != -1) {
                        out.write(buf, 0, read)
                        downloaded += read
                        if (System.currentTimeMillis() - last > 500) {
                            val pct = if (total > 0) ((downloaded * 100) / total).toInt() else -1
                            setProgress(workDataOf("progress" to pct))
                            NotificationManagerCompat.from(applicationContext)
                                .notify(id.hashCode(), notification(pct, "$displayName: $pct%"))
                            last = System.currentTimeMillis()
                        }
                    }
                }
            }

            // Verify hash
            val sha = MessageDigest.getInstance("SHA-256")
            tmp.inputStream().use { ins ->
                val b = ByteArray(8192)
                var r: Int
                while (ins.read(b).also { r = it } != -1) {
                    sha.update(b, 0, r)
                }
            }
            val hash = sha.digest().joinToString("") { "%02x".format(it) }
            if (!hash.equals(expectHash, ignoreCase = true)) {
                tmp.delete()
                return Result.failure(workDataOf("error" to "SHA-256 mismatch"))
            }
            tmp.renameTo(dest)

            NotificationManagerCompat.from(applicationContext).cancel(id.hashCode())
            return Result.success()
        } catch (t: Throwable) {
            return Result.retry()
        }
    }
}
