package com.sirmrm.messageapp.data.repo

import com.sirmrm.messageapp.data.db.MessageDao
import com.sirmrm.messageapp.data.db.MoveLogDao
import com.sirmrm.messageapp.data.db.MoveLogEntity
import com.sirmrm.messageapp.data.db.toDomain
import com.sirmrm.messageapp.data.provider.SmsProvider
import com.sirmrm.messageapp.data.sms.SmsSender
import com.sirmrm.messageapp.domain.model.MessageFolder
import com.sirmrm.messageapp.domain.model.SmsMessage
import com.sirmrm.messageapp.domain.repository.MessageRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val moveLogDao: MoveLogDao,
    private val smsSender: SmsSender,
    private val smsProvider: SmsProvider,
) : MessageRepository {

    override fun observeMessages(threadId: Long): Flow<List<SmsMessage>> =
        messageDao.observeByThread(threadId).map { list -> list.map { it.toDomain() } }

    override suspend fun sendMessage(address: String, body: String, subscriptionId: Int): Result<Long> =
        smsSender.send(address, body, subscriptionId)

    override suspend fun moveMessage(messageId: Long, folder: MessageFolder) =
        moveMessages(listOf(messageId), folder)

    override suspend fun moveMessages(messageIds: List<Long>, folder: MessageFolder) {
        val locations = messageDao.locations(messageIds)
        if (locations.isEmpty()) return
        val batch = (moveLogDao.lastBatch() ?: 0) + 1
        moveLogDao.insertAll(
            messageIds.zip(locations).map { (id, loc) ->
                MoveLogEntity(
                    batch = batch,
                    messageId = id,
                    threadId = null,
                    previousFolder = loc.folder,
                    previousCustomFolderId = loc.customFolderId,
                )
            }
        )
        val deletedAt = if (folder == MessageFolder.TRASH) System.currentTimeMillis() else null
        for (id in messageIds) {
            messageDao.move(id, folder.name, null, deletedAt)
        }
    }

    override suspend fun markPromo(messageId: Long, isPromo: Boolean) {
        moveMessage(messageId, if (isPromo) MessageFolder.PROMOTIONS else MessageFolder.INBOX)
    }

    override suspend fun importFromProvider(onProgress: suspend (done: Int, total: Int) -> Unit): Int =
        smsProvider.importAll(onProgress)

    override suspend fun purgeExpiredTrash(): Int {
        val retentionMs = TimeUnit.DAYS.toMillis(30)
        return messageDao.purgeTrashBefore(System.currentTimeMillis() - retentionMs)
    }

    override suspend fun autoCleanOldPromos(olderThanDays: Int): Int {
        val before = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(olderThanDays.toLong())
        val ids = messageDao.promoIdsOlderThan(before)
        if (ids.isEmpty()) return 0
        messageDao.trash(ids, System.currentTimeMillis())
        return ids.size
    }

    override suspend fun undoLastMove(): Int {
        val batch = moveLogDao.lastBatch() ?: return 0
        val logs = moveLogDao.batch(batch).filter { it.messageId != null }
        for (log in logs) {
            messageDao.restore(listOf(log.messageId!!), log.previousFolder, log.previousCustomFolderId)
        }
        moveLogDao.clearBatch(batch)
        return logs.size
    }
}
