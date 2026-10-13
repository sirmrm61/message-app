package com.sirmrm.messageapp.domain.repository

import com.sirmrm.messageapp.domain.model.AppSettings
import com.sirmrm.messageapp.domain.model.Conversation
import com.sirmrm.messageapp.domain.model.MessageFolder
import com.sirmrm.messageapp.domain.model.SmsMessage
import com.sirmrm.messageapp.domain.model.UserFolder
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun observeConversations(folder: MessageFolder, query: String): Flow<List<Conversation>>
    fun observeConversation(threadId: Long): Flow<Conversation?>
    suspend fun setPinned(threadId: Long, pinned: Boolean)
    suspend fun setMuted(threadId: Long, muted: Boolean)
    suspend fun markRead(threadId: Long)
    suspend fun moveToFolder(threadId: Long, folder: MessageFolder, customFolderId: Long? = null)
    /** Permanently delete a conversation and all its messages. */
    suspend fun deletePermanently(threadId: Long)
    suspend fun saveDraft(threadId: Long, draft: String?)
    fun observeUserFolders(): Flow<List<UserFolder>>
    suspend fun createFolder(name: String): Long
    /** Conversations the latest purge moved to trash/archive, for Undo. */
    suspend fun undoLastBulkMove(): Int
}

interface MessageRepository {
    fun observeMessages(threadId: Long): Flow<List<SmsMessage>>
    suspend fun sendMessage(address: String, body: String, subscriptionId: Int): Result<Long>
    suspend fun moveMessage(messageId: Long, folder: MessageFolder)
    suspend fun moveMessages(messageIds: List<Long>, folder: MessageFolder)
    suspend fun markPromo(messageId: Long, isPromo: Boolean)
    /** Import all existing SMS from the Telephony provider. Returns count. */
    suspend fun importFromProvider(onProgress: suspend (done: Int, total: Int) -> Unit): Int
    /** Permanently delete trash items older than the retention window. */
    suspend fun purgeExpiredTrash(): Int
    /** Auto-move old promos to trash when the user enabled auto-delete. */
    suspend fun autoCleanOldPromos(olderThanDays: Int): Int
    /** Revert the last folder move performed by moveMessage(s). */
    suspend fun undoLastMove(): Int
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
}

interface SenderRuleRepository {
    /** ALWAYS_PROMO / NEVER_PROMO overrides per sender. */
    suspend fun setRule(address: String, alwaysPromo: Boolean?)
    suspend fun ruleFor(address: String): Boolean?
    fun observeBlocked(): Flow<Set<String>>
    suspend fun setBlocked(address: String, blocked: Boolean)

    /** ALWAYS_TX / NEVER_TX overrides from the ✓/✗ confirm bar (null clears). */
    suspend fun setTxRule(address: String, decision: String?)
    /** All rules with a promo/tx decision, for the Settings page. */
    fun observeRules(): Flow<List<SenderRuleRow>>
}

data class SenderRuleRow(
    val senderKey: String,
    val decision: String?,
    val alwaysPromo: Boolean,
    val blocked: Boolean,
    val createdAt: Long,
)

interface BackupRepository {
    /** Export messages to SMS Backup & Restore compatible XML. */
    suspend fun exportXml(destination: java.io.OutputStream): Int
    /** Import messages from that XML format. Returns imported count. */
    suspend fun importXml(source: java.io.InputStream): Int
}
