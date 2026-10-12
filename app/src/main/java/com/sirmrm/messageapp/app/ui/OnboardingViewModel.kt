package com.sirmrm.messageapp.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirmrm.messageapp.domain.repository.MessageRepository
import com.sirmrm.messageapp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingState(
    val step: Int = 0,
    val importing: Boolean = false,
    val importDone: Boolean = false,
    val imported: Int = 0,
    val total: Int = 0,
) {
    val importProgress: Float get() = if (total == 0) 0f else imported.toFloat() / total
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state

    fun next() = _state.update { it.copy(step = (it.step + 1).coerceAtMost(2)) }

    /** Permission request happens via the activity; kept as a hook. */
    fun requestPermissions() = Unit

    fun startImport() {
        if (_state.value.importing) return
        _state.update { it.copy(importing = true) }
        viewModelScope.launch {
            val imported = messageRepository.importFromProvider { done, total ->
                _state.update { it.copy(imported = done, total = total) }
            }
            _state.update {
                it.copy(importing = false, importDone = true, imported = imported)
            }
        }
    }

    fun finish() {
        viewModelScope.launch {
            settingsRepository.update { it.copy(onboardingDone = true) }
        }
    }
}
