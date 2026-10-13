package com.sirmrm.messageapp.classifier

/** User-level override learned from explicit feedback. */
enum class SenderRule {
    /** "This sender is always promo" — user marked it. */
    ALWAYS_PROMO,

    /** "This sender is never promo" — user un-marked it. */
    NEVER_PROMO,

    /** "This sender is transactional" — user confirmed the ✓ bar. */
    ALWAYS_TX,

    /** "This sender is not transactional" — user rejected the ✓ bar. */
    NEVER_TX,
}

/** Everything the classifier needs to know about one incoming SMS. */
data class SmsInput(
    val body: String,
    /** Raw sender address (short code, service number or MSISDN). */
    val sender: String,
    /** Sender exists in the user contacts. */
    val isContact: Boolean = false,
    /** There is a prior two-way conversation with this sender. */
    val hasConversation: Boolean = false,
    /** Persisted per-sender override from user feedback, if any. */
    val senderRule: SenderRule? = null,
    /** Sender previously produced ≥N bank-structured messages (auto-learned). */
    val knownBankSender: Boolean = false,
    /** Sender matches a user-added bank pattern from Settings. */
    val customBankSender: Boolean = false,
)
