package com.sirmrm.messageapp.classifier.tts

import com.sirmrm.messageapp.classifier.SmsCategory

/**
 * Pure decision for «should this incoming message be read aloud automatically?»
 * Unit-tested; the app passes in environment flags it measured itself.
 */
object AutoReadDecider {

    fun shouldAutoRead(
        ttsEnabled: Boolean,
        isKeyContact: Boolean,
        keyAutoRead: Boolean,
        category: SmsCategory,
        allowOtp: Boolean,
        allowTransactional: Boolean,
        /** true when silent/DND or required-output conditions block playback. */
        environmentBlocks: Boolean,
    ): Boolean {
        if (!ttsEnabled) return false
        if (!isKeyContact || !keyAutoRead) return false
        if (environmentBlocks) return false
        // Privacy: OTP/transactional are never auto-read without explicit opt-in.
        when (category) {
            SmsCategory.OTP -> if (!allowOtp) return false
            SmsCategory.TRANSACTIONAL -> if (!allowTransactional) return false
            else -> {}
        }
        return true
    }
}
