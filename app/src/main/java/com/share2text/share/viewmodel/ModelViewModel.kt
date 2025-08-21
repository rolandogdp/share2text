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
import androidx.work.WorkInfo
import javax.inject.Inject

@HiltViewModel
class ModelViewModel @Inject constructor(
    private val repo: ModelRepository,
    private val dataStore: DataStore<Preferences>
): ViewModel() {

    data class State(
        val presets: List<ModelRepository.ModelPreset> = emptyList(),
        val activeId: String? = null,
        val downloaded: Set<String> = emptySet(),
        val progress: Map<String, Int> = emptyMap()
    )

    private val _state = MutableStateFlow(
        State(
            presets = repo.presets,
            downloaded = repo.presets.filter { repo.isDownloaded(it) }.map { it.id }.toSet()
        )
    )
    val state: StateFlow<State> = _state

    init {
        viewModelScope.launch {
            repo.activeModelFlow(dataStore).collect { id ->
                _state.value = _state.value.copy(activeId = id)
            }
        }

        repo.presets.forEach { p ->
            viewModelScope.launch {
                repo.downloadStatus(p).collect { status ->
                    val prog = _state.value.progress.toMutableMap()
                    val down = _state.value.downloaded.toMutableSet()
                    when (status.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            prog.remove(p.id)
                            down.add(p.id)
                        }
                        WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> {
                            prog[p.id] = status.progress
                        }
                        else -> {
                            prog.remove(p.id)
                        }
                    }
                    _state.value = _state.value.copy(progress = prog, downloaded = down)
                }
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
