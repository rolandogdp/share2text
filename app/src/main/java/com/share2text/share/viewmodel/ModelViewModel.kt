package com.share2text.share.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.share2text.share.download.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelViewModel @Inject constructor(
    private val repo: ModelRepository,
    private val dataStore: DataStore<Preferences>
): ViewModel() {

    data class State(
        val presets: List<ModelRepository.ModelPreset> = emptyList(),
        val activeId: String? = null
    )

    private val _state = MutableStateFlow(State(presets = repo.presets))
    val state: StateFlow<State> = _state

    init {
        viewModelScope.launch {
            repo.activeModelFlow(dataStore).collect { id ->
                _state.value = _state.value.copy(activeId = id)
            }
        }
    }

    fun download(p: ModelRepository.ModelPreset) {
        repo.enqueueDownload(p, wifiOnly = true)
    }

    fun setActive(p: ModelRepository.ModelPreset) {
        viewModelScope.launch { repo.setActiveModel(dataStore, p.id) }
    }
}
