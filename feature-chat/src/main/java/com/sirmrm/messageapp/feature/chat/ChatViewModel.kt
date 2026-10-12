package com.sirmrm.messageapp.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirmrm.messageapp.data.classifier.ClassifierManager
import com.sirmrm.messageapp.domain.model.AppSettings
import com.sirmrm.messageapp.domain.model.Conversation
import com.sirmrm.messageapp.domain.model.MessageFolder
import com.sirmrm.messageapp.domain.model.SmsMessage
import com.sirmrm.messageapp.domain.repository.ConversationRepository
import com.sirmrm.messageapp.domain.repository.MessageRepository
import com.sirmrm.messageapp.domain.repository.SenderRuleRepository
import com.sirmrm.messageapp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val threadId: Long = -1L,
    val messages: List<SmsMessage> = emptyList(),
    val conversation: Conversation? = null,
    val settings: AppSettings? = null,
    val sending: Boolean = false,
    val sendError: String? = null,
    val draft: String = "",
    val selectedSim: Int = -1,
    val searchQuery: String = "",
    /** Day filter (millis of the selected day); null = no filter. */
    val filterDate: Long? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val senderRuleRepository: SenderRuleRepository,
    private val settingsRepository: SettingsRepository,
    private val classifierManager: ClassifierManager,
) : ViewModel() {

    private val threadId = MutableStateFlow(-1L)
    private val sending = MutableStateFlow(false)
    private val sendError = MutableStateFlow<String?>(null)
    private val draft = MutableStateFlow("")
    private val selectedSim = MutableStateFlow(-1)
    private val searchQuery = MutableStateFlow("")
    private val filterDate = MutableStateFlow<Long?>(null)

    private val messages = threadId.flatMapLatest { id ->
        if (id > 0) messageRepository.observeMessages(id) else flowOf(emptyList())
    }
    private val conversation = threadId.flatMapLatest { id ->
        if (id > 0) conversationRepository.observeConversation(id) else flowOf(null)
    }

    val state: StateFlow<ChatUiState> = combine(
        messages, conversation, settingsRepository.settings,
        sending, sendError, draft, selectedSim, threadId,
        searchQuery, filterDate,
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        ChatUiState(
            messages = arr[0] as List<SmsMessage>,
            conversation = arr[1] as Conversation?,
            settings = arr[2] as AppSettings,
            sending = arr[3] as Boolean,
            sendError = arr[4] as String?,
            draft = arr[5] as String,
            selectedSim = arr[6] as Int,
            threadId = arr[7] as Long,
            searchQuery = arr[8] as String,
            filterDate = arr[9] as Long?,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatUiState())

    fun setSearchQuery(q: String) { searchQuery.value = q }
    fun setFilterDate(ts: Long?) { filterDate.value = ts }

    fun init(id: Long) {
        threadId.value = id
        if (id > 0) {
            viewModelScope.launch { conversationRepository.markRead(id) }
        }
    }

    fun setDraft(text: String) {
        draft.value = text
        viewModelScope.launch {
            if (threadId.value > 0) {
                conversationRepository.saveDraft(threadId.value, text.ifBlank { null })
            }
        }
    }

    fun selectSim(subId: Int) {
        selectedSim.value = subId
    }

    fun send(to: String, body: String) = viewModelScope.launch {
        if (body.isBlank() || sending.value) return@launch
        sending.value = true
        sendError.value = null
        messageRepository.sendMessage(to, body, selectedSim.value)
            .onSuccess { draft.value = "" }
            .onFailure { sendError.value = it.message }
        sending.value = false
    }

    /** User feedback: mark this message promo (or not) + learn + sender rule. */
    fun markPromo(message: SmsMessage, isPromo: Boolean) = viewModelScope.launch {
        classifierManager.learn(message.address, message.body, isPromo)
        senderRuleRepository.setRule(message.address, isPromo)
        messageRepository.moveMessage(
            message.id,
            if (isPromo) MessageFolder.PROMOTIONS else MessageFolder.INBOX,
        )
    }

    fun clearError() {
        sendError.value = null
    }
}
