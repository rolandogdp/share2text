package com.share2text.share.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.share2text.share.download.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.io.File

@HiltViewModel
class TranscriptionViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val repo: ModelRepository
): ViewModel() {

    data class State(
        val text: String = "",
        val progress: Int = 0,
        val done: Boolean = false,
        val error: String? = null,
        val started: Boolean = false
    )
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private val langKey = stringPreferencesKey("language")
    private val threadsKey = stringPreferencesKey("threads")
    private val activeKey = stringPreferencesKey("active_model")

    fun markStarted() { _state.value = _state.value.copy(started = true) }

    fun updateLive(text: String, progress: Int, done: Boolean, error: String?) {
        _state.value = _state.value.copy(text = text, progress = progress, done = done, error = error)
    }

    suspend fun ensureActiveModelOrPrompt(nav: androidx.navigation.NavHostController): String? {
        var id: String? = null
        dataStore.data.collect { prefs ->
            id = prefs[activeKey]
            return@collect
        }
        val preset = repo.presets.find { it.id == id }
        val file = preset?.let { repo.fileForPreset(it) }
        return if (file != null && file.exists()) file.absolutePath else null
    }

    fun language(): String? = null // read from DataStore if needed
    fun threads(): Int = Runtime.getRuntime().availableProcessors()

    fun copyToClipboard(ctx: Context) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("transcript", state.value.text))
    }

    fun onSaved(ok: Boolean) {
        // Snackbar could be shown via UI; no-op here.
    }
}
