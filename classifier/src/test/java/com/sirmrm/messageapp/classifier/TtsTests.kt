package com.sirmrm.messageapp.classifier

import com.sirmrm.messageapp.classifier.tts.AutoReadDecider
import com.sirmrm.messageapp.classifier.tts.FaNumberToWords
import com.sirmrm.messageapp.classifier.tts.TtsTextPreprocessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsTests {

    @Test
    fun `numbers become persian words`() {
        assertEquals("صفر", FaNumberToWords.of(0))
        assertEquals("پنج", FaNumberToWords.of(5))
        assertEquals("بیست و یک", FaNumberToWords.of(21))
        assertEquals("صد", FaNumberToWords.of(100))
        assertEquals("صد و پنجاه هزار", FaNumberToWords.of(150_000))
        assertEquals("دو میلیون و پانصد هزار", FaNumberToWords.of(2_500_000))
    }

    @Test
    fun `amounts with unit become spoken text`() {
        val out = TtsTextPreprocessor.preprocess("واریز ۱۵۰٬۰۰۰ ریال")
        assertTrue(out.isNotEmpty())
        assertTrue(out[0].contains("صد و پنجاه هزار ریال"))
    }

    @Test
    fun `links are replaced and sentences get trailing stop`() {
        val out = TtsTextPreprocessor.preprocess("سلام https://x.ir/a خداحافظ")
        assertTrue(out.joinToString(" ").contains("یک پیوند"))
        assertTrue(out.all { it.endsWith(".") })
    }

    @Test
    fun `auto-read respects master and privacy gates`() {
        // master off → never
        assertFalse(AutoReadDecider.shouldAutoRead(
            ttsEnabled = false, isKeyContact = true, keyAutoRead = true,
            category = SmsCategory.PERSONAL, allowOtp = true, allowTransactional = true,
            environmentBlocks = false))
        // non-key contact → never
        assertFalse(AutoReadDecider.shouldAutoRead(
            ttsEnabled = true, isKeyContact = false, keyAutoRead = true,
            category = SmsCategory.PERSONAL, allowOtp = false, allowTransactional = false,
            environmentBlocks = false))
        // key + allowed personal → yes
        assertTrue(AutoReadDecider.shouldAutoRead(
            ttsEnabled = true, isKeyContact = true, keyAutoRead = true,
            category = SmsCategory.PERSONAL, allowOtp = false, allowTransactional = false,
            environmentBlocks = false))
        // OTP from key contact → no unless opted in
        assertFalse(AutoReadDecider.shouldAutoRead(
            ttsEnabled = true, isKeyContact = true, keyAutoRead = true,
            category = SmsCategory.OTP, allowOtp = false, allowTransactional = true,
            environmentBlocks = false))
        assertTrue(AutoReadDecider.shouldAutoRead(
            ttsEnabled = true, isKeyContact = true, keyAutoRead = true,
            category = SmsCategory.OTP, allowOtp = true, allowTransactional = false,
            environmentBlocks = false))
        // silent/DND → no
        assertFalse(AutoReadDecider.shouldAutoRead(
            ttsEnabled = true, isKeyContact = true, keyAutoRead = true,
            category = SmsCategory.PERSONAL, allowOtp = true, allowTransactional = true,
            environmentBlocks = true))
    }
}
