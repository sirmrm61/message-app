package com.sirmrm.messageapp.domain.model

/** Built-in folders every conversation/message lives in. */
enum class MessageFolder {
    /** Ordinary inbox. */
    INBOX,

    /** Person-to-person conversations. */
    PERSONAL,

    /** Bank/postal/billing traffic (protected). */
    TRANSACTIONAL,

    /** One-time passwords / verification codes (protected). */
    OTP,

    /** Advertising messages — auto-moved or user-moved. */
    PROMOTIONS,

    /** User-archived conversations. */
    ARCHIVE,

    /** 30-day recycle bin before permanent deletion. */
    TRASH,
}
