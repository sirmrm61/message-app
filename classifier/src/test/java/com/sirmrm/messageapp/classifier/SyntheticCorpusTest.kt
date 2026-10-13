package com.sirmrm.messageapp.classifier

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs the NLP-specialist synthetic corpus (synthetic_spam_corpus.json —
 * 10 promo/spam + 10 ham edge cases covering shop, real-estate, clinic,
 * investment, phishing, OTP, bank, postal, appointment and personal texts)
 * through the real engine and asserts the required labels:
 *
 *  - spam → PROMO, except phishing entries which must be flagged suspicious
 *  - ham  → OTP → OTP; Bank/SecurityEvent → TRANSACTIONAL; every other ham
 *           must never land in PROMO.
 */
class SyntheticCorpusTest {

    private val patterns: BankingPatterns by lazy {
        BankingPatterns.load(
            javaClass.getResource("/patterns/banking_fa.json")!!.readText()
        )
    }

    private val corpus: JSONArray by lazy {
        JSONArray(
            javaClass.getResource("/synthetic_spam_corpus.json")!!.readText()
        )
    }

    private fun classifyAll(): List<Triple<String, String, ClassificationResult>> {
        val c = SmsClassifier(bankingPatterns = patterns)
        return (0 until corpus.length()).map { i ->
            val o = corpus.getJSONObject(i)
            val label = o.getString("label")
            val cat = o.getString("category")
            val r = c.classify(
                SmsInput(
                    body = o.getString("text"),
                    sender = if (label == "spam") "30009999999" else "+989121234567",
                    isContact = false,
                    hasConversation = cat == "Personal",
                )
            )
            Triple("$label/$cat", o.getString("text").take(50), r)
        }
    }

    @Test
    fun `spam samples classify as promo or flagged suspicious`() {
        val wrong = classifyAll().filter { (tag, _, r) ->
            tag.startsWith("spam/") &&
                !(r.category == SmsCategory.PROMO || r.suspicious)
        }
        assertTrue("spam misses: $wrong", wrong.isEmpty())
    }

    @Test
    fun `phishing samples are flagged suspicious`() {
        val wrong = classifyAll().filter { (tag, _, r) ->
            tag == "spam/Phishing" && !r.suspicious
        }
        assertTrue("unflagged phishing: $wrong", wrong.isEmpty())
    }

    @Test
    fun `ham samples never become promo`() {
        val wrong = classifyAll().filter { (tag, _, r) ->
            tag.startsWith("ham/") && r.category == SmsCategory.PROMO
        }
        assertTrue("ham-as-promo: $wrong", wrong.isEmpty())
    }

    @Test
    fun `ham protected classes land correctly`() {
        for ((tag, text, r) in classifyAll()) {
            when {
                tag == "ham/OTP" -> assertEquals("$tag: $text", SmsCategory.OTP, r.category)
                tag == "ham/Bank" || tag == "ham/SecurityEvent" ->
                    assertEquals("$tag: $text", SmsCategory.TRANSACTIONAL, r.category)
            }
        }
    }
}
