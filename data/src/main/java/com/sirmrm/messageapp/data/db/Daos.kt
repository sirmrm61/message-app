package com.sirmrm.messageapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query(
        """SELECT * FROM conversations
           WHERE folder = :folder
             AND (:query = '' OR displayName LIKE '%' || :query || '%'
                  OR snippet LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%')
           ORDER BY pinned DESC, lastMessageDate DESC"""
    )
    fun observeByFolder(folder: String, query: String): Flow<List<ConversationEntity>>

    @Query(
        """SELECT * FROM conversations
           WHERE folder IN (:folders)
             AND (:query = '' OR displayName LIKE '%' || :query || '%'
                  OR snippet LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%')
           ORDER BY pinned DESC, lastMessageDate DESC"""
    )
    fun observeInFolders(folders: List<String>, query: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE threadId = :threadId")
    fun observe(threadId: Long): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE threadId = :threadId")
    suspend fun get(threadId: Long): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ConversationEntity)

    @Query("UPDATE conversations SET pinned = :pinned WHERE threadId = :threadId")
    suspend fun setPinned(threadId: Long, pinned: Boolean)

    @Query("UPDATE conversations SET muted = :muted WHERE threadId = :threadId")
    suspend fun setMuted(threadId: Long, muted: Boolean)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE threadId = :threadId")
    suspend fun markRead(threadId: Long)

    @Query("UPDATE conversations SET folder = :folder, customFolderId = :customFolderId WHERE threadId = :threadId")
    suspend fun moveToFolder(threadId: Long, folder: String, customFolderId: Long?)

    @Query("UPDATE conversations SET draft = :draft WHERE threadId = :threadId")
    suspend fun saveDraft(threadId: Long, draft: String?)

    @Query("DELETE FROM conversations WHERE threadId = :threadId")
    suspend fun deleteConversation(threadId: Long)

    @Query("DELETE FROM messages WHERE threadId = :threadId")
    suspend fun deleteThreadMessages(threadId: Long)

    @Query("UPDATE conversations SET snippet = :snippet, lastMessageDate = :date, unreadCount = unreadCount + :unreadDelta, promoScore = :score, maybePromo = :maybePromo WHERE threadId = :threadId")
    suspend fun updateForNewMessage(threadId: Long, snippet: String, date: Long, unreadDelta: Int, score: Float, maybePromo: Boolean)

    @Query("SELECT * FROM conversations WHERE customFolderId = :customFolderId")
    fun observeInCustomFolder(customFolderId: Long): Flow<List<ConversationEntity>>
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE threadId = :threadId ORDER BY date ASC")
    fun observeByThread(threadId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun get(id: Long): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<MessageEntity>)

    @Query("SELECT folder, customFolderId FROM messages WHERE id = :id")
    suspend fun location(id: Long): Location?

    @Query("UPDATE messages SET folder = :folder, customFolderId = :customFolderId, deletedAt = :deletedAt WHERE id = :id")
    suspend fun move(id: Long, folder: String, customFolderId: Long?, deletedAt: Long?)

    @Query("UPDATE messages SET folder = :folder, customFolderId = :customFolderId WHERE id IN (:ids)")
    suspend fun moveAll(ids: List<Long>, folder: String, customFolderId: Long?)

    @Query("SELECT folder, customFolderId FROM messages WHERE id IN (:ids)")
    suspend fun locations(ids: List<Long>): List<Location>

    @Query("UPDATE messages SET folder = :folder, customFolderId = :customFolderId, deletedAt = NULL WHERE id IN (:ids)")
    suspend fun restore(ids: List<Long>, folder: String, customFolderId: Long?)

    @Query("UPDATE messages SET read = 1 WHERE threadId = :threadId")
    suspend fun markThreadRead(threadId: Long)

    @Query("UPDATE messages SET type = :type WHERE id = :id")
    suspend fun setType(id: Long, type: String)

    @Query("DELETE FROM messages WHERE folder = 'TRASH' AND deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun purgeTrashBefore(before: Long): Int

    @Query("SELECT id FROM messages WHERE folder = 'PROMOTIONS' AND date < :before")
    suspend fun promoIdsOlderThan(before: Long): List<Long>

    @Query("UPDATE messages SET folder = 'TRASH', deletedAt = :now WHERE id IN (:ids)")
    suspend fun trash(ids: List<Long>, now: Long)

    @Query("SELECT COUNT(*) FROM messages WHERE folder = 'PROMOTIONS' AND date > :since")
    fun countPromosSince(since: Long): Flow<Int>

    @Query("SELECT * FROM messages WHERE category = 'OTP' OR category = 'TRANSACTIONAL'")
    suspend fun protectedMessages(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE category = 'UNKNOWN'")
    suspend fun unclassifiedMessages(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE category = 'TRANSACTIONAL' ORDER BY date DESC, id DESC")
    fun observeTransactional(): Flow<List<MessageEntity>>

    /** Borderline bank-score messages kept in the inbox («maybe transactional»). */
    @Query("SELECT * FROM messages WHERE maybeTransactional = 1 AND folder != 'TRASH' ORDER BY date DESC, id DESC")
    fun observeMaybeTransactional(): Flow<List<MessageEntity>>

    /** Senders with their transactional-message count (auto-learn bank ids). */
    @Query("SELECT address, COUNT(*) AS cnt FROM messages WHERE category = 'TRANSACTIONAL' GROUP BY address")
    suspend fun transactionalSenders(): List<SenderCount>

    data class SenderCount(val address: String, val cnt: Int)

    @Query("SELECT * FROM messages")
    suspend fun exportAll(): List<MessageEntity>

    data class Location(val folder: String, val customFolderId: Long?)
}

@Dao
interface UserFolderDao {
    @Query("SELECT * FROM user_folders ORDER BY createdAt")
    fun observeAll(): Flow<List<UserFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: UserFolderEntity): Long
}

@Dao
interface SenderRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SenderRuleEntity)

    @Query("DELETE FROM sender_rules WHERE address = :address")
    suspend fun delete(address: String)

    @Query("SELECT * FROM sender_rules WHERE address = :address")
    suspend fun get(address: String): SenderRuleEntity?

    @Query("SELECT address FROM sender_rules WHERE blocked = 1")
    fun observeBlocked(): Flow<List<String>>
}

@Dao
interface MoveLogDao {
    @Insert
    suspend fun insertAll(logs: List<MoveLogEntity>)

    @Query("SELECT MAX(batch) FROM move_log")
    suspend fun lastBatch(): Long?

    @Query("SELECT * FROM move_log WHERE batch = :batch")
    suspend fun batch(batch: Long): List<MoveLogEntity>

    @Query("DELETE FROM move_log WHERE batch = :batch")
    suspend fun clearBatch(batch: Long)
}

@Dao
interface NbDao {
    @Query("SELECT * FROM nb_tokens")
    suspend fun allTokens(): List<NbTokenEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTokens(tokens: List<NbTokenEntity>)

    @Query("SELECT * FROM nb_meta WHERE id = 0")
    suspend fun meta(): NbMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: NbMetaEntity)
}

@Dao
interface CustomBankPatternDao {
    @Query("SELECT * FROM custom_bank_patterns ORDER BY createdAt")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<CustomBankPatternEntity>>

    @Query("SELECT * FROM custom_bank_patterns")
    suspend fun all(): List<CustomBankPatternEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CustomBankPatternEntity): Long

    @Query("DELETE FROM custom_bank_patterns WHERE id = :id")
    suspend fun delete(id: Long)
}
