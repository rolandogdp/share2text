package com.share2text.share.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
): ViewModel() {

    data class State(
        val language: String? = null,
        val threads: Int = Runtime.getRuntime().availableProcessors(),
        val wifiOnly: Boolean = true
    )
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private val langKey = stringPreferencesKey("language")
    private val threadsKey = stringPreferencesKey("threads")
    private val wifiOnlyKey = stringPreferencesKey("wifi_only")

    init {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                _state.value = _state.value.copy(
                    language = prefs[langKey],
                    threads = prefs[threadsKey]?.toIntOrNull() ?: _state.value.threads,
                    wifiOnly = prefs[wifiOnlyKey]?.toBooleanStrictOrNull() ?: _state.value.wifiOnly
                )
            }
        }
    }

    fun setLanguage(value: String?) { viewModelScope.launch { dataStore.edit { prefs ->
        if (value == null) prefs.remove(langKey) else prefs[langKey] = value
    } } }

    fun setThreads(value: Int) { viewModelScope.launch { dataStore.edit { prefs ->
        prefs[threadsKey] = value.toString()
    } } }

    fun setWifiOnly(value: Boolean) { viewModelScope.launch { dataStore.edit { prefs ->
        prefs[wifiOnlyKey] = value.toString()
    } } }
}
