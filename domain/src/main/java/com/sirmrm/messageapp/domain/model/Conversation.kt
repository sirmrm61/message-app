package com.sirmrm.messageapp.domain.model

data class Conversation(
    /** Thread id in the SMS provider. */
    val id: Long,
    val address: String,
    /** Display name — resolved contact name or raw address. */
    val displayName: String,
    val snippet: String,
    val lastMessageDate: Long,
    val unreadCount: Int = 0,
    val pinned: Boolean = false,
    val muted: Boolean = false,
    val blocked: Boolean = false,
    val folder: MessageFolder = MessageFolder.INBOX,
    val customFolderId: Long? = null,
    /** Latest classification on the thread's newest incoming message. */
    val promoScore: Float = 0f,
    val maybePromo: Boolean = false,
    /** Unsent draft text, if any. */
    val draft: String? = null,
)
