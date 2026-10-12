package com.sirmrm.messageapp.classifier

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsClassifierTest {

    private fun classify(c: SmsClassifier, s: TestCorpus.Sample): ClassificationResult =
        c.classify(
            SmsInput(
                body = s.body,
                sender = s.sender,
                isContact = s.isContact,
                hasConversation = s.hasConversation,
            )
        )

    @Test
    fun `corpus has at least 200 samples`() {
        assertTrue(TestCorpus.samples().size >= 200)
    }

    @Test
    fun `no OTP or transactional message is ever auto-moved or suggested`() {
        val classifier = SmsClassifier()
        for (s in TestCorpus.samples()) {
            if (s.expected == SmsCategory.OTP || s.expected == SmsCategory.TRANSACTIONAL) {
                val r = classify(classifier, s)
                assertEquals(
                    "protected message must stay PROTECTED: ${s.body.take(40)}",
                    Decision.PROTECTED, r.decision,
                )
                assertEquals(0f, r.score)
            }
        }
    }

    @Test
    fun `otp error rate is exactly zero`() {
        val classifier = SmsClassifier()
        val otps = TestCorpus.samples().filter { it.expected == SmsCategory.OTP }
        assertTrue(otps.isNotEmpty())
        for (s in otps) {
            val r = classify(classifier, s)
            assertFalse("OTP wrongly promo: ${s.body}", r.isPromo)
            assertFalse(r.decision == Decision.AUTO_MOVE || r.decision == Decision.SUGGEST)
        }
    }

    @Test
    fun `precision and recall report`() {
        val classifier = SmsClassifier()
        var tp = 0; var fp = 0; var fn = 0; var tn = 0
        for (s in TestCorpus.samples()) {
            val r = classify(classifier, s)
            val predictedPromo = r.decision == Decision.AUTO_MOVE || r.decision == Decision.SUGGEST
            when {
                predictedPromo && s.expected == SmsCategory.PROMO -> tp++
                predictedPromo && s.expected != SmsCategory.PROMO -> fp++
                !predictedPromo && s.expected == SmsCategory.PROMO -> fn++
                else -> tn++
            }
        }
        val precision = if (tp + fp == 0) 1.0 else tp.toDouble() / (tp + fp)
        val recall = if (tp + fn == 0) 1.0 else tp.toDouble() / (tp + fn)
        println("precision=%.3f recall=%.3f (tp=%d fp=%d fn=%d tn=%d)".format(precision, recall, tp, fp, fn, tn))
        assertTrue("precision too low: $precision", precision >= 0.90)
        assertTrue("recall too low: $recall", recall >= 0.80)
    }

    @Test
    fun `sender rules override everything`() {
        val c = SmsClassifier()
        val promo = SmsInput("جشنواره تخفیف ۹۰٪! لغو ۱۱", sender = "3000123", senderRule = SenderRule.NEVER_PROMO)
        assertEquals(Decision.PROTECTED, c.classify(promo).decision)

        val ham = SmsInput("سلام خوبی", sender = "+989120000000", senderRule = SenderRule.ALWAYS_PROMO)
        assertEquals(Decision.AUTO_MOVE, c.classify(ham).decision)
    }

    @Test
    fun `contacts and prior conversations are whitelisted`() {
        val c = SmsClassifier()
        val contact = SmsInput("تخفیف ویژه برای شما", sender = "+989120000001", isContact = true)
        assertEquals(Decision.PROTECTED, c.classify(contact).decision)
        val convo = SmsInput("تخفیف ویژه برای شما", sender = "+989120000002", hasConversation = true)
        assertEquals(Decision.PROTECTED, c.classify(convo).decision)
    }

    @Test
    fun `learning improves borderline classification`() {
        val c = SmsClassifier()
        val tricky = SmsInput("فروش اقساطی زمستانه، برای خرید عدد ۳ را بفرستید", sender = "09123334444")
        val before = c.classify(tricky).score
        repeat(5) { c.learn(SmsInput("فروش اقساطی، برای خرید عدد $it را بفرستید", sender = "x"), isPromo = true) }
        repeat(5) { c.learn(SmsInput("سلام چه خبرا عزیز $it", sender = "y"), isPromo = false) }
        val after = c.classify(tricky).score
        assertTrue("learned model should raise promo score ($before -> $after)", after >= before)
    }

    @Test
    fun `normalization unifies arabic persian digits and zwnj`() {
        assertEquals("کد تایید 1234", TextNormalizer.normalize("کد تاييد ۱۲۳۴"))
        assertEquals(TextNormalizer.normalize("ثبت‌نام"), TextNormalizer.normalize("ثبتنام"))
        assertEquals("989123456789", TextNormalizer.normalizeSender("+۹۸۹۱۲۳۴۵۶۷۸۹"))
        assertEquals("989123456789", TextNormalizer.normalizeSender("+98-912-345-6789"))
    }
}
