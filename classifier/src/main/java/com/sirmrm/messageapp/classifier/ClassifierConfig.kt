package com.sirmrm.messageapp.classifier

/**
 * Tunable knobs of the classifier. Everything that looks like "magic data"
 * (service sender prefixes, thresholds, keyword lists) lives here so it can be
 * edited from the Settings screen instead of being hard-coded in the engine.
 */
data class ClassifierConfig(
    /** Promo score at or above which a message is moved automatically. */
    val autoMoveThreshold: Float = 0.72f,

    /** Promo score at or above which a "probably promo" suggestion is shown. */
    val suggestThreshold: Float = 0.35f,

    /**
     * Sender address prefixes treated as bulk service numbers.
     * Stored as regex fragments matched against the normalized (digits-only) sender.
     */
    val serviceSenderPatterns: List<String> = DEFAULT_SERVICE_SENDER_PATTERNS,

    /** Regexes for short / tracking links. */
    val shortLinkPatterns: List<String> = DEFAULT_SHORT_LINK_PATTERNS,
) {
    companion object {
        val DEFAULT_SERVICE_SENDER_PATTERNS = listOf(
            "^3000", "^2000", "^5000", "^1000", "^4000", "^6000",
            "^9810", "^9830", "^9820", "^9850",
        )

        val DEFAULT_SHORT_LINK_PATTERNS = listOf(
            "bit\\.ly", "tinyurl", "t\\.co/", "goo\\.gl", "lilink\\.ir", "opizo",
            "yon\\.ir", "b2n\\.ir", "fa\\..{0,4}link", "z\\.ir",
        )
    }
}
