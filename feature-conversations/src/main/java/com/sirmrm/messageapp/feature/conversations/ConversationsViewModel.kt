package com.sirmrm.messageapp.feature.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirmrm.messageapp.classifier.BankTxParser
import com.sirmrm.messageapp.data.classifier.ClassifierManager
import com.sirmrm.messageapp.data.db.MessageDao
import com.sirmrm.messageapp.data.db.toDomain
import com.sirmrm.messageapp.domain.model.AppSettings
import com.sirmrm.messageapp.domain.model.SmsMessage
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class ConversationsUiState(
    val folder: MessageFolder = MessageFolder.INBOX,
    val query: String = "",
    val conversations: List<Conversation> = emptyList(),
    val promosCleanedThisWeek: Int = 0,
    val settings: AppSettings? = null,
    val selection: Set<Long> = emptySet(),
    /** Bank transactions (TRANSACTIONAL messages that parse as bank SMS). */
    val transactions: List<BankTxItem> = emptyList(),
    val txSelection: Set<Long> = emptySet(),
    val filterDate: Long? = null,
) {
    val selectionMode: Boolean get() = selection.isNotEmpty()
    val txSelectionMode: Boolean get() = txSelection.isNotEmpty()

    val depositCount: Int get() = transactions.count { it.direction == BankTxParser.Direction.DEPOSIT }
    val withdrawalCount: Int get() = transactions.count { it.direction == BankTxParser.Direction.WITHDRAWAL }

    fun selectedSum(dir: BankTxParser.Direction): Long = transactions
        .filter { it.id in txSelection && it.direction == dir }
        .sumOf { it.amountRials ?: 0L }
}

/** A transactional message enriched with its parsed bank record. */
data class BankTxItem(
    val id: Long,
    val message: SmsMessage,
    val direction: BankTxParser.Direction,
    val amountRials: Long?,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val settingsRepository: SettingsRepository,
    classifierManager: ClassifierManager,
    messageDao: MessageDao,
) : ViewModel() {

    init {
        // Backfill: classify imported/uncategorized rows so folders and tabs
        // reflect the classifier (idempotent, IO-bound).
        viewModelScope.launch { runCatching { classifierManager.classifyUnclassified() } }
    }

    private val folder = MutableStateFlow(MessageFolder.INBOX)
    private val query = MutableStateFlow("")
    private val selection = MutableStateFlow<Set<Long>>(emptySet())
    private val txSelection = MutableStateFlow<Set<Long>>(emptySet())
    private val filterDate = MutableStateFlow<Long?>(null)

    private val transactions = messageDao.observeTransactional().map { list ->
        list.mapNotNull { e ->
            runCatching {
                val m = e.toDomain()
                BankTxParser.parse(m.body)?.let { tx ->
                    BankTxItem(m.id, m, tx.direction, tx.amountRials)
                }
            }.getOrNull()
        }
    }.flowOn(Dispatchers.Default)

    private val conversations = combine(folder, query) { f, q -> f to q }
        .flatMapLatest { (f, q) -> conversationRepository.observeConversations(f, q) }

    private val filteredConversations = combine(conversations, filterDate) { list, day ->
        if (day == null) list
        else list.filter { com.sirmrm.messageapp.core.util.JalaliDate.sameDay(it.lastMessageDate, day) }
    }

    val state: StateFlow<ConversationsUiState> = combine(
        filteredConversations, settingsRepository.settings, folder, query, selection,
        transactions, txSelection, filterDate,
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        ConversationsUiState(
            folder = arr[2] as MessageFolder,
            query = arr[3] as String,
            conversations = arr[0] as List<Conversation>,
            promosCleanedThisWeek = (arr[1] as AppSettings).promosCleanedThisWeek,
            settings = arr[1] as AppSettings,
            selection = arr[4] as Set<Long>,
            transactions = arr[5] as List<BankTxItem>,
            txSelection = arr[6] as Set<Long>,
            filterDate = arr[7] as Long?,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConversationsUiState())

    fun setFilterDate(ts: Long?) { filterDate.value = ts }

    fun toggleTxSelection(id: Long) = txSelection.update {
        if (id in it) it - id else it + id
    }

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
