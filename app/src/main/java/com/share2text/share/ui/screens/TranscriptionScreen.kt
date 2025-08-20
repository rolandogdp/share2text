package com.share2text.share.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.share2text.share.service.TranscriptionService
import com.share2text.share.util.saveTextToDocuments
import com.share2text.share.util.shareTextIntent
import com.share2text.share.viewmodel.TranscriptionViewModel
import kotlinx.coroutines.launch

@Composable
fun TranscriptionScreen(nav: NavHostController, vm: TranscriptionViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()
    val uris = SharedUrisHolder.uris

    var bound by remember { mutableStateOf(false) }
    var service: TranscriptionService? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val lb = binder as? TranscriptionService.LocalBinder
                val svc = lb?.service
                bound = svc != null
                service = svc

                if (svc != null && uris.isNotEmpty() && !state.started) {
                    val uri = uris.first()
                    vm.markStarted()
                    scope.launch {
                        val modelPath = vm.ensureActiveModelOrPrompt(nav)
                        if (modelPath != null) {
                            svc.startTranscription(
                                uri = uri,
                                modelPath = modelPath,
                                language = vm.language(),
                                threads = vm.threads()
                            ) { update ->
                                vm.updateLive(
                                    text = update.text,
                                    progress = update.progress,
                                    done = update.done,
                                    error = update.error
                                )
                            }
                        }
                    }
                }
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                bound = false
                service = null
            }
        }
        val intent = Intent(ctx, TranscriptionService::class.java)
        ctx.startForegroundService(intent)
        ctx.bindService(intent, conn, Context.BIND_AUTO_CREATE)
        onDispose {
            if (bound) ctx.unbindService(conn)
        }
    }


    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Transcription", style = MaterialTheme.typography.headlineMedium)
        LinearProgressIndicator(progress = { (state.progress / 100f).coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Box(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(state.text.ifBlank { "Waiting for transcription..." })
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.copyToClipboard(ctx) }) { Text("Copy") }
            OutlinedButton(onClick = { ctx.startActivity(shareTextIntent(state.text)) }) { Text("Share") }
            OutlinedButton(onClick = {
                scope.launch {
                    val uri = ctx.saveTextToDocuments("transcript.txt", state.text)
                    vm.onSaved(uri != null)
                }
            }) { Text("Save .txt") }
        }
        if (state.error != null) {
            Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
        }
    }
}
