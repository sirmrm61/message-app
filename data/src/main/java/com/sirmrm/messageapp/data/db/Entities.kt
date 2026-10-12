package com.sirmrm.messageapp.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val threadId: Long,
    val address: String,
    val displayName: String,
    val photoUri: String?,
    val snippet: String,
    val lastMessageDate: Long,
    val unreadCount: Int,
    val pinned: Boolean,
    val muted: Boolean,
    val blocked: Boolean,
    val folder: String,
    val customFolderId: Long?,
    val promoScore: Float,
    val maybePromo: Boolean,
    val draft: String?,
)

@Entity(
    tableName = "messages",
    indices = [Index("threadId"), Index("folder"), Index("date"), Index("deletedAt")],
)
data class MessageEntity(
    /** SMS provider _id when the row came from Telephony.Sms. */
    @PrimaryKey val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: String,
    val read: Boolean,
    val seen: Boolean,
    val subscriptionId: Int,
    val folder: String,
    val customFolderId: Long?,
    val category: String,
    val promoScore: Float,
    /** Reason codes joined with ';'. */
    val reasonCodes: String,
    val deletedAt: Long?,
)

@Entity(tableName = "user_folders")
data class UserFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "sender_rules")
data class SenderRuleEntity(
    @PrimaryKey val address: String,
    val alwaysPromo: Boolean,
    val blocked: Boolean,
)

/** Single-level undo: the last folder change applied to messages/threads. */
@Entity(tableName = "move_log")
data class MoveLogEntity(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0,
    val batch: Long,
    val messageId: Long?,
    val threadId: Long?,
    val previousFolder: String,
    val previousCustomFolderId: Long?,
)

/** Persisted Naive Bayes token counts (one row per token). */
@Entity(tableName = "nb_tokens")
data class NbTokenEntity(
    @PrimaryKey val token: String,
    val promoCount: Int,
    val hamCount: Int,
)

/** Persisted Naive Bayes document counts. */
@Entity(tableName = "nb_meta")
data class NbMetaEntity(
    @PrimaryKey val id: Int = 0,
    val promoDocs: Int,
    val hamDocs: Int,
)
