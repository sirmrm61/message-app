package com.sirmrm.messageapp.data.classifier

import com.sirmrm.messageapp.classifier.ClassificationResult
import com.sirmrm.messageapp.classifier.Decision
import com.sirmrm.messageapp.classifier.SmsCategory
import com.sirmrm.messageapp.domain.model.MessageFolder

/**
 * Maps a classification result to the folder the message/conversation should
 * live in. Auto-moved promos go to PROMOTIONS; protected categories get their
 * own folders (still visible under the "All" tab); everything else is INBOX.
 */
private val DISCOUNT_REASONS = setOf(
    "discount", "percent", "festival", "sale_words", "free_charge", "prize",
)

fun folderFor(result: ClassificationResult): MessageFolder = when {
    result.decision == Decision.AUTO_MOVE ->
        if (result.reasons.any { it.code in DISCOUNT_REASONS })
            MessageFolder.DISCOUNT else MessageFolder.PROMOTIONS
    result.category == SmsCategory.OTP -> MessageFolder.OTP
    result.category == SmsCategory.TRANSACTIONAL -> MessageFolder.TRANSACTIONAL
    result.category == SmsCategory.PERSONAL -> MessageFolder.PERSONAL
    else -> MessageFolder.INBOX
}

private val MANUAL_FOLDERS = setOf("ARCHIVE", "TRASH")
private val PROTECTED_FOLDERS = setOf("OTP", "TRANSACTIONAL")
private val PROMO_FOLDERS = setOf("PROMOTIONS", "DISCOUNT")

/**
 * Computes the conversation folder after a newly classified message. User-made
 * moves (ARCHIVE/TRASH/custom) are respected; a thread already holding OTP or
 * transactional traffic is never hidden into PROMOTIONS by a later promo.
 */
fun mergeConversationFolder(existing: String?, incoming: MessageFolder): String = when {
    existing in MANUAL_FOLDERS -> existing!!
    existing in PROTECTED_FOLDERS && incoming in setOf(MessageFolder.PROMOTIONS, MessageFolder.DISCOUNT) -> existing!!
    incoming == MessageFolder.INBOX -> existing ?: MessageFolder.INBOX.name
    existing in PROTECTED_FOLDERS -> incoming.name
    else -> incoming.name
}
