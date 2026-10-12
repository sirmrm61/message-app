package com.sirmrm.messageapp.classifier

/** What the app should do with the message after classification. */
enum class Decision {
    /** Confident promo — move to the Promotions folder automatically. */
    AUTO_MOVE,

    /** Medium confidence — keep in inbox, show a "probably promo" badge with a one-tap move action. */
    SUGGEST,

    /** Ordinary message — leave alone. */
    KEEP,

    /** OTP / transactional / whitelisted — never move, never auto-delete. */
    PROTECTED,
}

/** A single human-readable explanation for the verdict, in both app languages. */
data class Reason(
    val code: String,
    val messageFa: String,
    val messageEn: String,
)

/** Output of the classifier for one message. */
data class ClassificationResult(
    /** Promo probability in [0, 1]. */
    val score: Float,
    val category: SmsCategory,
    val decision: Decision,
    val reasons: List<Reason>,
    /** Bank-verdict details when the message evaluated as bank-ish. */
    val bank: BankTxParser.Verdict? = null,
    /** Bank-imitating phishing cues — never transactional, show red warning. */
    val suspicious: Boolean = false,
    /** Borderline bank score — keep in inbox with a «maybe transactional» badge. */
    val maybeTransactional: Boolean = false,
) {
    val isPromo: Boolean get() = category == SmsCategory.PROMO
}
