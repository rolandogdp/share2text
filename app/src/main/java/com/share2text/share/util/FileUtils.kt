package com.share2text.share.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.Charset

suspend fun Context.copyContentUriToCache(uri: Uri, subdir: String = "shared"): File? =
    withContext(Dispatchers.IO) {
        try {
            val dir = File(cacheDir, subdir).apply { mkdirs() }
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "shared_${System.currentTimeMillis()}"
            val outFile = File(dir, name)
            contentResolver.openInputStream(uri)?.use { inp ->
                FileOutputStream(outFile).use { out ->
                    inp.copyTo(out)
                }
            }
            outFile
        } catch (t: Throwable) {
            null
        }
    }

suspend fun Context.saveTextToDocuments(filename: String, text: String, charset: Charset = Charsets.UTF_8): Uri? =
    withContext(Dispatchers.IO) {
        try {
            val resolver: ContentResolver = contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            }
            val uri = if (Build.VERSION.SDK_INT >= 29) {
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            } else null
            uri?.let {
                resolver.openOutputStream(it)?.use { os ->
                    os.write(text.toByteArray(charset))
                }
            }
            uri
        } catch (t: Throwable) { null }
    }

fun shareTextIntent(text: String): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
