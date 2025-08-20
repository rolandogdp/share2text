package com.share2text.share.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.share2text.share.download.ModelRepository
import com.share2text.share.viewmodel.ModelViewModel

@Composable
fun ModelPickerScreen(nav: NavHostController, vm: ModelViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Models", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.presets) { p ->
                ElevatedCard(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(p.displayName)
                        Text(p.url, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.download(p) }) { Text("Download") }
                            if (state.activeId == p.id) {
                                AssistChip(onClick = {}, label = { Text("Active") })
                            } else {
                                OutlinedButton(onClick = { vm.setActive(p) }) { Text("Set active") }
                            }
                        }
                    }
                }
            }
        }
    }
}
