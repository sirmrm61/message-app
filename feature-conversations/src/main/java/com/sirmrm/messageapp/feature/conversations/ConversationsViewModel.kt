package com.sirmrm.messageapp.feature.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirmrm.messageapp.data.classifier.ClassifierManager
import com.sirmrm.messageapp.domain.model.AppSettings
import com.sirmrm.messageapp.domain.model.Conversation
import com.sirmrm.messageapp.domain.model.MessageFolder
import com.sirmrm.messageapp.domain.repository.ConversationRepository
import com.sirmrm.messageapp.domain.repository.MessageRepository
import com.sirmrm.messageapp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConversationsUiState(
    val folder: MessageFolder = MessageFolder.INBOX,
    val query: String = "",
    val conversations: List<Conversation> = emptyList(),
    val promosCleanedThisWeek: Int = 0,
    val settings: AppSettings? = null,
    val selection: Set<Long> = emptySet(),
) {
    val selectionMode: Boolean get() = selection.isNotEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val settingsRepository: SettingsRepository,
    classifierManager: ClassifierManager,
) : ViewModel() {

    init {
        // Backfill: classify imported/uncategorized rows so folders and tabs
        // reflect the classifier (idempotent, IO-bound).
        viewModelScope.launch { classifierManager.classifyUnclassified() }
    }

    private val folder = MutableStateFlow(MessageFolder.INBOX)
    private val query = MutableStateFlow("")
    private val selection = MutableStateFlow<Set<Long>>(emptySet())

    private val conversations = combine(folder, query) { f, q -> f to q }
        .flatMapLatest { (f, q) -> conversationRepository.observeConversations(f, q) }

    val state: StateFlow<ConversationsUiState> = combine(
        conversations, settingsRepository.settings, folder, query, selection,
    ) { convs, settings, f, q, sel ->
        ConversationsUiState(
            folder = f,
            query = q,
            conversations = convs,
            promosCleanedThisWeek = settings.promosCleanedThisWeek,
            settings = settings,
            selection = sel,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConversationsUiState())

    fun selectFolder(f: MessageFolder) {
        folder.value = f
        clearSelection()
    }

    fun setQuery(q: String) {
        query.value = q
    }

    fun toggleSelection(threadId: Long) {
        selection.value = selection.value.let {
            if (threadId in it) it - threadId else it + threadId
        }
    }

    fun clearSelection() {
        selection.value = emptySet()
    }

    fun archive(threadId: Long) = viewModelScope.launch {
        conversationRepository.moveToFolder(threadId, MessageFolder.ARCHIVE)
    }

    fun trash(threadId: Long) = viewModelScope.launch {
        conversationRepository.moveToFolder(threadId, MessageFolder.TRASH)
    }

    fun togglePin(conversation: Conversation) = viewModelScope.launch {
        conversationRepository.setPinned(conversation.id, !conversation.pinned)
    }

    fun markRead(threadId: Long) = viewModelScope.launch {
        conversationRepository.markRead(threadId)
    }

    fun archiveSelected() = viewModelScope.launch {
        selection.value.forEach { conversationRepository.moveToFolder(it, MessageFolder.ARCHIVE) }
        clearSelection()
    }

    fun trashSelected() = viewModelScope.launch {
        selection.value.forEach { conversationRepository.moveToFolder(it, MessageFolder.TRASH) }
        clearSelection()
    }

    fun markSelectedRead() = viewModelScope.launch {
        selection.value.forEach { conversationRepository.markRead(it) }
        clearSelection()
    }

    fun undo() = viewModelScope.launch {
        conversationRepository.undoLastBulkMove()
        messageRepository.undoLastMove()
    }
}
