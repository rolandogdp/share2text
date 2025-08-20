package com.share2text.share.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.share2text.share.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(nav: NavHostController, vm: SettingsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = state.language ?: "",
            onValueChange = { vm.setLanguage(it.ifBlank { null }) },
            label = { Text("Language (empty for auto)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.threads.toString(),
            onValueChange = { vm.setThreads(it.toIntOrNull() ?: state.threads) },
            label = { Text("Threads") },
            modifier = Modifier.fillMaxWidth()
        )
        Row {
            Checkbox(checked = state.wifiOnly, onCheckedChange = { vm.setWifiOnly(it) })
            Text("Download over Wi‑Fi only", modifier = Modifier.padding(start = 8.dp))
        }
    }
}
