package com.sirmrm.messageapp.data.repo

import com.sirmrm.messageapp.data.db.ConversationDao
import com.sirmrm.messageapp.data.db.MoveLogDao
import com.sirmrm.messageapp.data.db.MoveLogEntity
import com.sirmrm.messageapp.data.db.UserFolderDao
import com.sirmrm.messageapp.data.db.UserFolderEntity
import com.sirmrm.messageapp.data.db.folderFromDb
import com.sirmrm.messageapp.data.db.toDomain
import com.sirmrm.messageapp.domain.model.Conversation
import com.sirmrm.messageapp.domain.model.MessageFolder
import com.sirmrm.messageapp.domain.model.UserFolder
import com.sirmrm.messageapp.domain.repository.ConversationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val userFolderDao: UserFolderDao,
    private val moveLogDao: MoveLogDao,
) : ConversationRepository {

    override fun observeConversations(folder: MessageFolder, query: String): Flow<List<Conversation>> {
        val daoFlow = if (folder == MessageFolder.INBOX) {
            // "All" = every conversation not moved away (archive, trash, promos).
            conversationDao.observeInFolders(ALL_TAB_FOLDERS, query)
        } else {
            conversationDao.observeByFolder(folder.name, query)
        }
        return daoFlow.map { list -> list.map { it.toDomain() } }
    }

    override fun observeConversation(threadId: Long): Flow<Conversation?> =
        conversationDao.observe(threadId).map { it?.toDomain() }

    override suspend fun setPinned(threadId: Long, pinned: Boolean) =
        conversationDao.setPinned(threadId, pinned)

    override suspend fun setMuted(threadId: Long, muted: Boolean) =
        conversationDao.setMuted(threadId, muted)

    override suspend fun markRead(threadId: Long) = conversationDao.markRead(threadId)

    override suspend fun moveToFolder(threadId: Long, folder: MessageFolder, customFolderId: Long?) {
        val current = conversationDao.get(threadId)
        if (current != null) {
            val batch = (moveLogDao.lastBatch() ?: 0) + 1
            moveLogDao.insertAll(
                listOf(
                    MoveLogEntity(
                        batch = batch,
                        messageId = null,
                        threadId = threadId,
                        previousFolder = current.folder,
                        previousCustomFolderId = current.customFolderId,
                    )
                )
            )
        }
        conversationDao.moveToFolder(threadId, folder.name, customFolderId)
    }

    override suspend fun deletePermanently(threadId: Long) {
        conversationDao.deleteThreadMessages(threadId)
        conversationDao.deleteConversation(threadId)
    }

    override suspend fun saveDraft(threadId: Long, draft: String?) =
        conversationDao.saveDraft(threadId, draft)

    override fun observeUserFolders(): Flow<List<UserFolder>> =
        userFolderDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun createFolder(name: String): Long =
        userFolderDao.insert(UserFolderEntity(name = name, createdAt = System.currentTimeMillis()))

    override suspend fun undoLastBulkMove(): Int {
        val batch = moveLogDao.lastBatch() ?: return 0
        val logs = moveLogDao.batch(batch)
        for (log in logs) {
            if (log.threadId != null) {
                conversationDao.moveToFolder(log.threadId, log.previousFolder, log.previousCustomFolderId)
            }
        }
        moveLogDao.clearBatch(batch)
        return logs.size
    }

    companion object {
        private val ALL_TAB_FOLDERS = listOf(
            MessageFolder.INBOX.name,
            MessageFolder.PERSONAL.name,
            MessageFolder.TRANSACTIONAL.name,
            MessageFolder.OTP.name,
        )
    }
}
