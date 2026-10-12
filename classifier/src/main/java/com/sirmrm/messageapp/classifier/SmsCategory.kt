package com.sirmrm.messageapp.classifier

/**
 * Semantic category assigned to an SMS by the classifier.
 */
enum class SmsCategory {
    /** Marketing / advertising message. */
    PROMO,

    /** Bank transaction, delivery tracking, billing — must never be touched. */
    TRANSACTIONAL,

    /** One-time password / verification code — must never be touched. */
    OTP,

    /** Person-to-person traffic. */
    PERSONAL,

    /** Not enough signal to decide. */
    UNKNOWN,
}
