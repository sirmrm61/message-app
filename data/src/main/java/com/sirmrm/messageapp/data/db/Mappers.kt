package com.sirmrm.messageapp.data.db

import com.sirmrm.messageapp.classifier.SmsCategory
import com.sirmrm.messageapp.domain.model.Conversation
import com.sirmrm.messageapp.domain.model.MessageFolder
import com.sirmrm.messageapp.domain.model.MessageType
import com.sirmrm.messageapp.domain.model.SmsMessage
import com.sirmrm.messageapp.domain.model.UserFolder

internal fun folderFromDb(v: String): MessageFolder =
    MessageFolder.entries.firstOrNull { it.name == v } ?: MessageFolder.INBOX

internal fun typeFromDb(v: String): MessageType =
    MessageType.entries.firstOrNull { it.name == v } ?: MessageType.INBOX

internal fun categoryFromDb(v: String): SmsCategory =
    SmsCategory.entries.firstOrNull { it.name == v } ?: SmsCategory.UNKNOWN

fun ConversationEntity.toDomain() = Conversation(
    id = threadId,
    address = address,
    displayName = displayName,
    snippet = snippet,
    lastMessageDate = lastMessageDate,
    unreadCount = unreadCount,
    pinned = pinned,
    muted = muted,
    blocked = blocked,
    folder = folderFromDb(folder),
    customFolderId = customFolderId,
    promoScore = promoScore,
    maybePromo = maybePromo,
    draft = draft,
)

fun MessageEntity.toDomain() = SmsMessage(
    id = id,
    threadId = threadId,
    address = address,
    body = body,
    date = date,
    type = typeFromDb(type),
    read = read,
    seen = seen,
    subscriptionId = subscriptionId,
    folder = folderFromDb(folder),
    customFolderId = customFolderId,
    category = categoryFromDb(category),
    promoScore = promoScore,
    reasonCodes = if (reasonCodes.isBlank()) emptyList() else reasonCodes.split(';'),
    deletedAt = deletedAt,
)

fun SmsMessage.toEntity() = MessageEntity(
    id = id,
    threadId = threadId,
    address = address,
    body = body,
    date = date,
    type = type.name,
    read = read,
    seen = seen,
    subscriptionId = subscriptionId,
    folder = folder.name,
    customFolderId = customFolderId,
    category = category.name,
    promoScore = promoScore,
    reasonCodes = reasonCodes.joinToString(";"),
    deletedAt = deletedAt,
)

fun UserFolderEntity.toDomain() = UserFolder(id = id, name = name, createdAt = createdAt)
