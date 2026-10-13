package com.sirmrm.messageapp.classifier

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsClassifierTest {

    /** Loads the real versioned banking patterns JSON for tests. */
    private fun freshClassifier(): SmsClassifier {
        val json = requireNotNull(javaClass.classLoader
            ?.getResourceAsStream("patterns/banking_fa.json")) { "patterns missing" }
            .bufferedReader().use { it.readText() }
        return SmsClassifier(bankingPatterns = BankingPatterns.load(json))
    }

    private fun classify(c: SmsClassifier, s: TestCorpus.Sample): ClassificationResult =
        c.classify(
            SmsInput(
                body = s.body,
                sender = s.sender,
                isContact = s.isContact,
                hasConversation = s.hasConversation,
                knownBankSender = s.learnedBankSender,
            )
        )

    @Test
    fun `corpus satisfies spec quotas`() {
        val s = TestCorpus.samples()
        assertTrue("corpus >= 300", s.size >= 300)
        assertTrue("bank >= 150", s.count { it.expected == SmsCategory.TRANSACTIONAL } >= 150)
        assertTrue("promo >= 75", s.count { it.expected == SmsCategory.PROMO } >= 75)
        assertTrue("phishing >= 40", s.count { it.expectSuspicious } >= 40)
        val personal = s.count { it.expected == SmsCategory.PERSONAL && !it.expectSuspicious }
        assertTrue("personal >= 35", personal >= 35)
    }

    @Test
    fun `transactional recall is at least 98 percent`() {
        val classifier = freshClassifier()
        var txTotal = 0; var txHit = 0
        val misses = ArrayList<String>()
        for (s in TestCorpus.samples()) {
            if (s.expected != SmsCategory.TRANSACTIONAL) continue
            txTotal++
            val r = classify(classifier, s)
            if ((r.category == SmsCategory.TRANSACTIONAL || r.category == SmsCategory.OTP)
                && r.decision == Decision.PROTECTED) txHit++
            else misses += "${s.sender}: ${s.body.take(60)} -> ${r.category}/${r.decision}"
        }
        val recall = txHit.toDouble() / txTotal
        println("tx recall=%.3f (%d/%d) misses=%s".format(recall, txHit, txTotal, misses.take(10)))
        assertTrue("tx recall $recall < 0.98; misses=$misses", recall >= 0.98)
    }

    @Test
    fun `phishing is never transactional and flagged suspicious`() {
        val classifier = freshClassifier()
        for (s in TestCorpus.samples().filter { it.expectSuspicious }) {
            val r = classify(classifier, s)
            assertTrue("phishing not flagged: ${s.body.take(50)}", r.suspicious)
            assertFalse(r.category == SmsCategory.TRANSACTIONAL)
            assertFalse(r.decision == Decision.AUTO_MOVE || r.decision == Decision.SUGGEST)
            assertFalse(r.decision == Decision.PROTECTED)
        }
    }

    @Test
    fun `confusion matrix report`() {
        val classifier = freshClassifier()
        val cats = SmsCategory.values()
        val matrix = HashMap<SmsCategory, HashMap<SmsCategory, Int>>()
        val errors = ArrayList<String>()
        for (s in TestCorpus.samples()) {
            if (s.expectSuspicious) continue
            val r = classify(classifier, s)
            val predicted = when {
                r.decision == Decision.AUTO_MOVE || r.decision == Decision.SUGGEST -> SmsCategory.PROMO
                else -> r.category
            }
            matrix.getOrPut(s.expected) { HashMap() }
                .merge(predicted, 1, Int::plus)
            if (predicted != s.expected && predicted != SmsCategory.UNKNOWN) {
                errors += "${s.expected}->$predicted ${s.sender}: ${s.body.take(50)}"
            }
        }
        val sb = StringBuilder("confusion matrix (rows=expected):\n")
        for (e in cats) {
            val row = matrix[e] ?: continue
            sb.append("%-14s %s\n".format(e, row))
        }
        println(sb.toString())
        println("errors (${errors.size}):\n" + errors.take(20).joinToString("\n"))
    }

    @Test
    fun `no OTP or transactional message is ever auto-moved or suggested`() {
        val classifier = freshClassifier()
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
        val classifier = freshClassifier()
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
        val classifier = freshClassifier()
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
        val c = freshClassifier()
        val promo = SmsInput("جشنواره تخفیف ۹۰٪! لغو ۱۱", sender = "3000123", senderRule = SenderRule.NEVER_PROMO)
        assertEquals(Decision.PROTECTED, c.classify(promo).decision)

        val ham = SmsInput("سلام خوبی", sender = "+989120000000", senderRule = SenderRule.ALWAYS_PROMO)
        assertEquals(Decision.AUTO_MOVE, c.classify(ham).decision)
    }

    @Test
    fun `contacts and prior conversations are whitelisted`() {
        val c = freshClassifier()
        val contact = SmsInput("تخفیف ویژه برای شما", sender = "+989120000001", isContact = true)
        assertEquals(Decision.PROTECTED, c.classify(contact).decision)
        val convo = SmsInput("تخفیف ویژه برای شما", sender = "+989120000002", hasConversation = true)
        assertEquals(Decision.PROTECTED, c.classify(convo).decision)
    }

    @Test
    fun `learning improves borderline classification`() {
        val c = freshClassifier()
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

    // ---- unit tests: normalization / amount / sign (spec section 7) ----

    @Test
    fun `amount extraction handles fa digits thousands and toman`() {
        assertEquals(1_250_000L, BankTxParser.evaluate("برداشت: ۱٬۲۵۰٬۰۰۰ ریال").amountRials)
        assertEquals(5_000_000L, BankTxParser.evaluate("واریز ۵٬۰۰۰٬۰۰۰ ریال").amountRials)
        assertEquals(10L * 250_000, BankTxParser.evaluate("برداشت 250000 تومان").amountRials)
        assertEquals(3_000_000L, BankTxParser.evaluate("حواله پایا 3,000,000 ریال").amountRials)
    }

    @Test
    fun `sign drives direction`() {
        val dep = BankTxParser.evaluate("واریز +4,500,000 ریال. مانده 9,000,000")
        assertEquals(BankTxParser.Direction.DEPOSIT, dep.direction)
        val wd = BankTxParser.evaluate("برداشت -800,000 ریال. مانده 1,000,000")
        assertEquals(BankTxParser.Direction.WITHDRAWAL, wd.direction)
    }

    @Test
    fun `known bank sender adds weight but does not force verdict`() {
        freshClassifier() // pins JSON patterns
        val promo = BankTxParser.evaluate("وام ویژه! ثبت نام کنید. لغو ۱۱", senderNormalized = "30003305")
        assertFalse("bank promo must not be transactional", promo.isTransactional)
        val tx = BankTxParser.evaluate("واریز 1,000,000 ریال. مانده 5,000,000", senderNormalized = "30003305")
        assertTrue(tx.isTransactional)
    }

    @Test
    fun `maybe transactional for borderline bank-ish messages`() {
        freshClassifier() // pins JSON patterns
        val v = BankTxParser.evaluate("مانده: 4,500,000 ریال", senderNormalized = "unknown-x")
        assertTrue(v.isTransactional || v.maybe)
    }

    @Test
    fun `amount and sign unit tests run on json patterns`() {
        freshClassifier()
        assertEquals(1_250_000L, BankTxParser.evaluate("برداشت: ۱٬۲۵۰٬۰۰۰ ریال").amountRials)
        assertEquals(BankTxParser.Direction.WITHDRAWAL,
            BankTxParser.evaluate("برداشت -800,000 ریال").direction)
    }

    @Test
    fun `bank SMS from a saved contact is still transactional`() {
        val c = SmsClassifier()
        val r = c.classify(SmsInput(
            body = "*بانک شهر* خريد با کارت برداشت از:700796887209 مبلغ:3,803,800ريال موجودي:18,885,299 ريال 1405/04/29 14:57:57",
            sender = "+98200035",
            isContact = true,
        ))
        assertEquals(SmsCategory.TRANSACTIONAL, r.category)
    }

    @Test
    fun `OTP from a saved contact is still OTP`() {
        val c = SmsClassifier()
        val r = c.classify(SmsInput(
            body = "کد تایید ورود: 384215",
            sender = "+989111111111",
            isContact = true,
        ))
        assertEquals(SmsCategory.OTP, r.category)
    }

    @Test
    fun `ordinary text from a contact stays personal`() {
        val c = SmsClassifier()
        val r = c.classify(SmsInput(
            body = "سلام چطوری؟ فردا میای سر کار؟",
            sender = "+989111111111",
            isContact = true,
        ))
        assertEquals(SmsCategory.PERSONAL, r.category)
    }

    @Test
    fun `bank login notification classifies as transactional security`() {
        val c = SmsClassifier()
        val r = c.classify(SmsInput(
            body = "*بانک شهر* ورود به همراه شهر تاريخ : 1405/6/3 ساعت : 17:59:54",
            sender = "Bank Shahr",
        ))
        assertEquals(SmsCategory.TRANSACTIONAL, r.category)
    }

    @Test
    fun `date-time fragment is not parsed as amount`() {
        val v = BankTxParser.evaluate(
            "بانک ملت واريز:25,000,000 ريال مانده:40,000,000 ريال 1405/07/20-14:30",
        )
        // 25,000,000 ريال — not "14" from the timestamp tail.
        assertEquals(25_000_000L, v.amountRials)
    }
}