package com.sirmrm.messageapp.domain.model

import com.sirmrm.messageapp.classifier.SmsCategory

/** Message direction/type as stored in the SMS provider. */
enum class MessageType {
    INBOX,
    SENT,
    DRAFT,
    OUTBOX,
    FAILED,
    QUEUED,
}

data class SmsMessage(
    val id: Long,
    val threadId: Long,
    /** Sender/recipient address (short code or MSISDN). */
    val address: String,
    val body: String,
    val date: Long,
    val type: MessageType,
    val read: Boolean = false,
    val seen: Boolean = false,
    /** Subscription (SIM) id used for this message. */
    val subscriptionId: Int = -1,
    val folder: MessageFolder = MessageFolder.INBOX,
    /** Id of a user-created folder, when folder == custom. */
    val customFolderId: Long? = null,
    val category: SmsCategory = SmsCategory.UNKNOWN,
    /** Promo score 0..1 from the classifier. */
    val promoScore: Float = 0f,
    /** Human-readable reason codes shown to the user. */
    val reasonCodes: List<String> = emptyList(),
    /** When the message was moved to trash (for the 30-day purge). */
    val deletedAt: Long? = null,
) {
    val isIncoming: Boolean get() = type == MessageType.INBOX
}
