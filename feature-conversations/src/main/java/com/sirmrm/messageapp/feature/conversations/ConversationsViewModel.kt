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
    /** Subtype chip currently filtering the transactions tab (null = all). */
    val txSubtypeFilter: BankTxParser.TxSubtype? = null,
    /** Inbox messages scored as possible bank SMS — shown as suggestions. */
    val maybeTransactions: List<BankTxItem> = emptyList(),
    /** Show the optional monthly deposit/withdrawal summary. */
    val showMonthlySums: Boolean = false,
) {
    val selectionMode: Boolean get() = selection.isNotEmpty()
    val txSelectionMode: Boolean get() = txSelection.isNotEmpty()

    val depositCount: Int get() = transactions.count { it.direction == BankTxParser.Direction.DEPOSIT }
    val withdrawalCount: Int get() = transactions.count { it.direction == BankTxParser.Direction.WITHDRAWAL }

    fun selectedSum(dir: BankTxParser.Direction): Long = transactions
        .filter { it.id in txSelection && it.direction == dir }
        .sumOf { it.amountRials ?: 0L }

    /** Deposit/withdrawal totals for the Jalali month of `day`. */
    fun monthlySums(day: Long): Pair<Long, Long> {
        var dep = 0L; var wdr = 0L
        for (t in transactions) {
            val d = com.sirmrm.messageapp.core.util.JalaliDate.gregorianToJalali(t.message.date)
            val ref = com.sirmrm.messageapp.core.util.JalaliDate.gregorianToJalali(day)
            if (d.year == ref.year && d.month == ref.month) {
                val amt = t.amountRials ?: continue
                if (t.direction == BankTxParser.Direction.DEPOSIT) dep += amt
                else if (t.direction == BankTxParser.Direction.WITHDRAWAL) wdr += amt
            }
        }
        return dep to wdr
    }
}

/** A transactional message enriched with its stored bank verdict. */
data class BankTxItem(
    val id: Long,
    val message: SmsMessage,
    val direction: BankTxParser.Direction,
    val amountRials: Long?,
    val subtype: BankTxParser.TxSubtype = BankTxParser.TxSubtype.UNKNOWN,
    val balanceRials: Long? = null,
    val bankName: String? = null,
    val suspicious: Boolean = false,
) {
    /** OTP message received within the last 3 minutes — pinned to the top. */
    val isRecentOtp: Boolean
        get() = subtype == BankTxParser.TxSubtype.OTP &&
            System.currentTimeMillis() - message.date < 3 * 60_000
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val settingsRepository: SettingsRepository,
    private val classifierManager: ClassifierManager,
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

    private val txSubtypeFilter = MutableStateFlow<BankTxParser.TxSubtype?>(null)
    private val showMonthlySums = MutableStateFlow(false)

    private val transactions = messageDao.observeTransactional().map { list ->
        list.mapNotNull { e ->
            runCatching {
                val m = e.toDomain()
                // Prefer the verdict stored at classification time; fall back
                // to a fresh parse for rows written by older versions.
                val storedSubtype = runCatching { BankTxParser.TxSubtype.valueOf(m.txSubtype ?: "") }
                    .getOrDefault(BankTxParser.TxSubtype.UNKNOWN)
                val parsed = BankTxParser.parse(m.body)
                if (storedSubtype == BankTxParser.TxSubtype.UNKNOWN && parsed == null) return@runCatching null
                BankTxItem(
                    id = m.id,
                    message = m,
                    direction = parsed?.direction ?: BankTxParser.Direction.UNKNOWN,
                    amountRials = m.txAmountRials ?: parsed?.amountRials,
                    subtype = storedSubtype,
                    balanceRials = m.txBalanceRials,
                    bankName = m.bankName,
                    suspicious = m.suspicious,
                )
            }.getOrNull()
        }
    }.flowOn(Dispatchers.Default)

    private val conversations = combine(folder, query) { f, q -> f to q }
        .flatMapLatest { (f, q) -> conversationRepository.observeConversations(f, q) }

    private val filteredConversations = combine(conversations, filterDate) { list, day ->
        if (day == null) list
        else list.filter { com.sirmrm.messageapp.core.util.JalaliDate.sameDay(it.lastMessageDate, day) }
    }

    private val maybeTransactions = messageDao.observeMaybeTransactional().map { list ->
        list.mapNotNull { e ->
            runCatching {
                val m = e.toDomain()
                val parsed = BankTxParser.parse(m.body)
                BankTxItem(
                    id = m.id, message = m,
                    direction = parsed?.direction ?: BankTxParser.Direction.UNKNOWN,
                    amountRials = m.txAmountRials ?: parsed?.amountRials,
                    subtype = runCatching { BankTxParser.TxSubtype.valueOf(m.txSubtype ?: "") }
                        .getOrDefault(BankTxParser.TxSubtype.UNKNOWN),
                    balanceRials = m.txBalanceRials,
                    bankName = m.bankName,
                )
            }.getOrNull()
        }
    }.flowOn(Dispatchers.Default)

    private val filteredTransactions = combine(transactions, txSubtypeFilter) { list, sub ->
        val filtered = if (sub == null) list else list.filter { it.subtype == sub }
        // OTP codes younger than 3 minutes stay pinned on top.
        filtered.sortedWith(compareByDescending<BankTxItem> { it.isRecentOtp }.thenByDescending { it.message.date })
    }

    val state: StateFlow<ConversationsUiState> = combine(
        filteredConversations, settingsRepository.settings, folder, query, selection,
        filteredTransactions, txSelection, filterDate, txSubtypeFilter, showMonthlySums,
        maybeTransactions,
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
            txSubtypeFilter = arr[8] as BankTxParser.TxSubtype?,
            showMonthlySums = arr[9] as Boolean,
            maybeTransactions = arr[10] as List<BankTxItem>,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConversationsUiState())

    fun setTxSubtypeFilter(sub: BankTxParser.TxSubtype?) { txSubtypeFilter.value = sub }
    fun toggleMonthlySums() { showMonthlySums.update { !it } }

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
        // Inside the trash tab, "delete" means permanently delete.
        if (folder.value == MessageFolder.TRASH) {
            conversationRepository.deletePermanently(threadId)
        } else {
            conversationRepository.moveToFolder(threadId, MessageFolder.TRASH)
        }
    }

    fun selectAll() {
        selection.value = filteredConversationsCached().map { it.id }.toSet()
    }

    private fun filteredConversationsCached(): List<Conversation> = state.value.conversations

    fun selectAllTx() {
        txSelection.value = transactionsCached().map { it.id }.toSet()
    }

    /**
     * User feedback «this is/isn't transactional»: moves the message and
     * feeds the incremental learner so similar messages score better/worse.
     */
    fun feedbackTransactional(item: BankTxItem, isTx: Boolean) = viewModelScope.launch {
        messageRepository.moveMessage(
            item.id,
            if (isTx) MessageFolder.TRANSACTIONAL else MessageFolder.INBOX,
        )
        runCatching { classifierManager.learn(item.message.address, item.message.body, isPromo = !isTx) }
    }

    private fun transactionsCached(): List<BankTxItem> = state.value.transactions

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
        val permanent = folder.value == MessageFolder.TRASH
        selection.value.forEach {
            if (permanent) conversationRepository.deletePermanently(it)
            else conversationRepository.moveToFolder(it, MessageFolder.TRASH)
        }
        clearSelection()
    }

    fun clearTxSelection() { txSelection.value = emptySet() }

    fun markSelectedRead() = viewModelScope.launch {
        selection.value.forEach { conversationRepository.markRead(it) }
        clearSelection()
    }

    fun undo() = viewModelScope.launch {
        conversationRepository.undoLastBulkMove()
        messageRepository.undoLastMove()
    }
}
