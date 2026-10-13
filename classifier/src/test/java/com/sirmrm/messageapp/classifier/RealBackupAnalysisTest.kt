package com.sirmrm.messageapp.classifier

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Analysis harness: runs the real engine over the user's exported SMS backup
 * (real_backup.json, produced by in-app SMS export) and prints, per sender,
 * the category distribution plus verdict details for messages that did NOT
 * classify as expected. Used to tune patterns — not a pass/fail test.
 */
class RealBackupAnalysisTest {

    private fun loadPatterns(): BankingPatterns {
        val text = javaClass.getResource("/patterns/banking_fa.json")!!.readText()
        return BankingPatterns.load(text)
    }

    private fun loadBackup(): JSONArray =
        JSONObject(javaClass.getResource("/real_backup.json")!!.readText())
            .getJSONArray("messages")

    @Test
    fun analyze() {
        val classifier = SmsClassifier(bankingPatterns = loadPatterns())
        val backup = loadBackup()

        // threads containing a SENT message → hasConversation=true
        val convSenders = HashSet<String>()
        for (i in 0 until backup.length()) {
            val m = backup.getJSONObject(i)
            if (m.getString("type") == "SENT") convSenders += m.getString("address")
        }

        val bySenderCat = LinkedHashMap<String, MutableMap<String, Int>>()
        val missed = ArrayList<String>()
        for (i in 0 until backup.length()) {
            val m = backup.getJSONObject(i)
            if (m.getString("type") != "INBOX") continue
            val sender = m.getString("address")
            val body = m.getString("body")
            val res = classifier.classify(
                SmsInput(
                    body = body,
                    sender = sender,
                    hasConversation = sender in convSenders,
                    // +98200035 is saved in the user's contacts (proved below).
                    isContact = sender == "+98200035",
                )
            )
            bySenderCat.getOrPut(sender) { mutableMapOf() }
                .merge(res.category.name, 1, Int::plus)
        }

        println("\n===== PER-SENDER CATEGORIES =====")
        for ((s, cats) in bySenderCat.toSortedMap()) {
            println("$s  ->  $cats")
        }
    }

    @Test
    fun contactShieldingHypothesis() {
        // Would isContact=true explain bank messages landing in PERSONAL?
        val classifier = SmsClassifier(bankingPatterns = loadPatterns())
        val backup = loadBackup()
        var i = 0
        for (j in 0 until backup.length()) {
            val m = backup.getJSONObject(j)
            if (m.getString("address") != "+98200035") continue
            val plain = classifier.classify(SmsInput(body = m.getString("body"), sender = "+98200035"))
            val asContact = classifier.classify(
                SmsInput(body = m.getString("body"), sender = "+98200035", isContact = true)
            )
            if (i < 8) {
                println("PLAIN=${plain.category} CONTACT=${asContact.category} bank=${plain.bank?.score} :: ${m.getString("body").take(70)}")
            }
            i++
        }
        println("checked $i messages from +98200035")
    }
}
