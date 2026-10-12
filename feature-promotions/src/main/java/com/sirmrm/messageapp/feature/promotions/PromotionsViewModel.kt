package com.sirmrm.messageapp.feature.promotions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirmrm.messageapp.domain.model.AppSettings
import com.sirmrm.messageapp.domain.model.Conversation
import com.sirmrm.messageapp.domain.model.MessageFolder
import com.sirmrm.messageapp.domain.repository.ConversationRepository
import com.sirmrm.messageapp.domain.repository.MessageRepository
import com.sirmrm.messageapp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PromotionsUiState(
    val promoConversations: List<Conversation> = emptyList(),
    val suggestions: List<Conversation> = emptyList(),
    val checked: Set<Long> = emptySet(),
    val settings: AppSettings? = null,
)

@HiltViewModel
class PromotionsViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val checked = kotlinx.coroutines.flow.MutableStateFlow<Set<Long>>(emptySet())

    private val promos = conversationRepository.observeConversations(MessageFolder.PROMOTIONS, "")
    private val inboxSuggestions = conversationRepository.observeConversations(MessageFolder.INBOX, "")

    val state: StateFlow<PromotionsUiState> = combine(
        promos, inboxSuggestions, settingsRepository.settings, checked,
    ) { p, inbox, settings, sel ->
        PromotionsUiState(
            promoConversations = p,
            suggestions = inbox.filter { it.maybePromo },
            settings = settings,
            checked = sel,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PromotionsUiState())

    fun toggleChecked(threadId: Long) {
        checked.value = checked.value.let {
            if (threadId in it) it - threadId else it + threadId
        }
    }

    fun confirmSelected() = viewModelScope.launch {
        checked.value.forEach {
            conversationRepository.moveToFolder(it, MessageFolder.PROMOTIONS)
        }
        checked.value = emptySet()
    }

    fun setSensitivity(v: Float) = viewModelScope.launch {
        settingsRepository.update { it.copy(sensitivity = v) }
    }
}
