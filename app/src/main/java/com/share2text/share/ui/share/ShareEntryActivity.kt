package com.share2text.share.ui.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.net.toUri
import androidx.navigation.compose.rememberNavController
import com.share2text.share.App
import com.share2text.share.ui.screens.TranscriptionScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareEntryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent?.action
        val type = intent?.type

        val uris: List<Uri> = when {
            Intent.ACTION_SEND == action && type != null -> {
                val u: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                listOfNotNull(u)
            }
            Intent.ACTION_SEND_MULTIPLE == action && type != null -> {
                val list: ArrayList<Uri>? = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                list?.toList() ?: emptyList()
            }
            else -> emptyList()
        }

        setContent {
            // Route into main TranscriptionScreen and pass URIs via a singleton holder
            com.share2text.share.ui.screens.SharedUrisHolder.uris = uris
            App()
        }
    }
}
