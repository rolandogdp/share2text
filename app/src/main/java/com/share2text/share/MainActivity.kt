package com.share2text.share

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.share2text.share.ui.screens.HomeScreen
import com.share2text.share.ui.screens.ModelPickerScreen
import com.share2text.share.ui.screens.SettingsScreen
import com.share2text.share.ui.screens.TranscriptionScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}

@Composable
fun App() {
    val nav = rememberNavController()
    Surface(color = MaterialTheme.colorScheme.background) {
        NavHost(navController = nav, startDestination = "home") {
            composable("home") { HomeScreen(nav) }
            composable("models") { ModelPickerScreen(nav) }
            composable("settings") { SettingsScreen(nav) }
            composable("transcribe") { TranscriptionScreen(nav) }
        }
    }
}
