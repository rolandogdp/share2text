package com.share2text.share.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.hilt.navigation.compose.hiltViewModel
import com.share2text.share.viewmodel.ModelViewModel

@Composable
fun HomeScreen(nav: NavHostController, vm: ModelViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val activeName = state.presets.find { it.id == state.activeId }?.displayName ?: "None"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Share2Text", style = MaterialTheme.typography.headlineMedium)
        Text("Local Whisper transcription")
        Text("Active model: $activeName")

        Button(onClick = { nav.navigate("models") }, modifier = Modifier.fillMaxWidth()) {
            Text("Choose / Download Model")
        }
        Button(onClick = { nav.navigate("transcribe") }, modifier = Modifier.fillMaxWidth()) {
            Text("Transcribe shared audio")
        }
        Button(onClick = { nav.navigate("settings") }, modifier = Modifier.fillMaxWidth()) {
            Text("Settings")
        }
        Spacer(Modifier.weight(1f))
        Text("Tip: Share audio from WhatsApp/Telegram to Share2Text")
    }
}
