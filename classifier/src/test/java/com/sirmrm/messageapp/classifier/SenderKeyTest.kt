package com.sirmrm.messageapp.classifier

import org.junit.Assert.assertEquals
import org.junit.Test


class SenderKeyTest {

    @Test
    fun `iranian mobile formats collapse to one key`() {
        val expected = "+989121234567"
        assertEquals(expected, SenderKey.of("+989121234567"))
        assertEquals(expected, SenderKey.of("00989121234567"))
        assertEquals(expected, SenderKey.of("989121234567"))
        assertEquals(expected, SenderKey.of("09121234567"))
        assertEquals(expected, SenderKey.of("0912 123 4567"))
        assertEquals(expected, SenderKey.of("0912-123-4567"))
        assertEquals(expected, SenderKey.of("9121234567"))
    }

    @Test
    fun `alphabetic senders are case-insensitive`() {
        assertEquals("bankmelli", SenderKey.of("BankMelli"))
        assertEquals("bankmelli", SenderKey.of("BANKMELLI"))
        assertEquals("hamraheavval", SenderKey.of("HamraheAvval"))
    }

    @Test
    fun `short codes are kept as digits`() {
        assertEquals("30003305", SenderKey.of("30003305"))
        assertEquals("1200", SenderKey.of("1200"))
    }

    @Test
    fun `rules take priority over the bank model`() {
        val json = requireNotNull(javaClass.classLoader
            ?.getResourceAsStream("patterns/banking_fa.json")) { "patterns missing" }
            .bufferedReader().use { it.readText() }
        val c = SmsClassifier(bankingPatterns = BankingPatterns.load(json))
        val r = c.classify(SmsInput(
            body = "جشنواره فروش ویژه", sender = "99001122",
            senderRule = SenderRule.ALWAYS_TX,
        ))
        assertEquals(SmsCategory.TRANSACTIONAL, r.category)

        val r2 = c.classify(SmsInput(
            body = "ملت بانک\nواریز 1000 ریال\nمانده 2000", sender = "30003305",
            senderRule = SenderRule.NEVER_TX,
        ))
        assert(r2.category != SmsCategory.TRANSACTIONAL) { "NEVER_TX must exclude tx" }
    }
}
