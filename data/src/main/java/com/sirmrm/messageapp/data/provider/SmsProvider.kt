package com.sirmrm.messageapp.data.provider

import android.content.ContentValues
import android.content.Context
import android.provider.Telephony
import com.sirmrm.messageapp.data.db.ConversationDao
import com.sirmrm.messageapp.data.db.ConversationEntity
import com.sirmrm.messageapp.data.db.MessageDao
import com.sirmrm.messageapp.data.db.MessageEntity
import com.sirmrm.messageapp.domain.model.MessageType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads/writes the system SMS Provider (Telephony.Sms). Writes only succeed
 * while this app holds the default-SMS role; reads require READ_SMS.
 */
@Singleton
class SmsProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
) {

    private val projection = arrayOf(
        Telephony.Sms._ID,
        Telephony.Sms.THREAD_ID,
        Telephony.Sms.ADDRESS,
        Telephony.Sms.BODY,
        Telephony.Sms.DATE,
        Telephony.Sms.TYPE,
        Telephony.Sms.READ,
        Telephony.Sms.SEEN,
        Telephony.Sms.SUBSCRIPTION_ID,
    )

    /** Copy every SMS in the provider into Room. Returns rows imported. */
    suspend fun importAll(onProgress: suspend (done: Int, total: Int) -> Unit): Int {
        val resolver = context.contentResolver
        val cursor = resolver.query(
            Telephony.Sms.CONTENT_URI, projection, null, null,
            Telephony.Sms.DEFAULT_SORT_ORDER,
        ) ?: return 0

        var imported = 0
        cursor.use { c ->
            val total = c.count
            val batch = ArrayList<MessageEntity>(200)
            while (c.moveToNext()) {
                val threadId = c.getLong(1)
                val address = c.getString(2) ?: ""
                val body = c.getString(3) ?: ""
                batch += MessageEntity(
                    id = c.getLong(0),
                    threadId = threadId,
                    address = address,
                    body = body,
                    date = c.getLong(4),
                    type = providerType(c.getInt(5)).name,
                    read = c.getInt(6) == 1,
                    seen = c.getInt(7) == 1,
                    subscriptionId = c.getInt(8),
                    folder = "INBOX",
                    customFolderId = null,
                    category = "UNKNOWN",
                    promoScore = 0f,
                    reasonCodes = "",
                    deletedAt = null,
                )
                if (batch.size >= 200) {
                    messageDao.upsertAll(batch)
                    rebuildConversation(threadId, batch)
                    imported += batch.size
                    batch.clear()
                    onProgress(imported, total)
                }
            }
            if (batch.isNotEmpty()) {
                messageDao.upsertAll(batch)
                imported += batch.size
                onProgress(imported, total)
            }
        }
        rebuildAllConversations()
        return imported
    }

    private suspend fun rebuildConversation(threadId: Long, batch: List<MessageEntity>) {
        val last = batch.lastOrNull { it.threadId == threadId } ?: return
        upsertConversationShell(threadId, last.address, last.body, last.date, if (last.read) 0 else 1)
    }

    /** Rebuild conversation rows from whatever is in Room. */
    suspend fun rebuildAllConversations() {
        // A single pass over messages grouped per thread is enough here.
        val resolver = context.contentResolver
        val cursor = resolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.THREAD_ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.READ),
            null, null, "date DESC",
        ) ?: return
        val seen = HashSet<Long>()
        cursor.use { c ->
            while (c.moveToNext()) {
                val threadId = c.getLong(0)
                if (!seen.add(threadId)) continue
                upsertConversationShell(
                    threadId = threadId,
                    address = c.getString(1) ?: "",
                    snippet = c.getString(2) ?: "",
                    date = c.getLong(3),
                    unreadDelta = if (c.getInt(4) == 1) 0 else 1,
                )
            }
        }
    }

    private suspend fun upsertConversationShell(
        threadId: Long,
        address: String,
        snippet: String,
        date: Long,
        unreadDelta: Int,
    ) {
        val existing = conversationDao.get(threadId)
        if (existing != null && existing.lastMessageDate >= date) return
        conversationDao.upsert(
            ConversationEntity(
                threadId = threadId,
                address = address,
                displayName = address,
                snippet = snippet.take(140),
                lastMessageDate = date,
                unreadCount = (existing?.unreadCount ?: 0) + unreadDelta,
                pinned = existing?.pinned ?: false,
                muted = existing?.muted ?: false,
                blocked = existing?.blocked ?: false,
                folder = existing?.folder ?: "INBOX",
                customFolderId = existing?.customFolderId,
                promoScore = existing?.promoScore ?: 0f,
                maybePromo = existing?.maybePromo ?: false,
                draft = existing?.draft,
            )
        )
    }

    /** Insert a received SMS into the system provider. Returns provider id. */
    fun insertReceived(address: String, body: String, date: Long, subscriptionId: Int): Long? {
        // No local default-app check: the provider rejects writes from
        // non-default apps anyway, and getDefaultSmsPackage() can misreport
        // on builds where package visibility resolves nothing.
        val threadId = Telephony.Threads.getOrCreateThreadId(context, address)
        val values = ContentValues().apply {
            put(Telephony.Sms.THREAD_ID, threadId)
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, date)
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.SEEN, 0)
            put(Telephony.Sms.SUBSCRIPTION_ID, subscriptionId)
        }
        return runCatching {
            context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
        }.getOrNull()?.lastPathSegment?.toLongOrNull()
    }

    /** Insert a sent SMS into the system provider. Returns provider id. */
    fun insertSent(address: String, body: String, date: Long, subscriptionId: Int): Long? {
        val threadId = Telephony.Threads.getOrCreateThreadId(context, address)
        val values = ContentValues().apply {
            put(Telephony.Sms.THREAD_ID, threadId)
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, date)
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
            put(Telephony.Sms.SUBSCRIPTION_ID, subscriptionId)
        }
        return context.contentResolver
            .insert(Telephony.Sms.Sent.CONTENT_URI, values)
            ?.lastPathSegment?.toLongOrNull()
    }

    fun isDefaultSmsApp(): Boolean =
        Telephony.Sms.getDefaultSmsPackage(context) == context.packageName

    companion object {
        fun providerType(t: Int): MessageType = when (t) {
            Telephony.Sms.MESSAGE_TYPE_INBOX -> MessageType.INBOX
            Telephony.Sms.MESSAGE_TYPE_SENT -> MessageType.SENT
            Telephony.Sms.MESSAGE_TYPE_DRAFT -> MessageType.DRAFT
            Telephony.Sms.MESSAGE_TYPE_OUTBOX -> MessageType.OUTBOX
            Telephony.Sms.MESSAGE_TYPE_FAILED -> MessageType.FAILED
            Telephony.Sms.MESSAGE_TYPE_QUEUED -> MessageType.QUEUED
            else -> MessageType.INBOX
        }
    }
}
