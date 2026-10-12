package com.sirmrm.messageapp.classifier

import kotlin.math.ln

/**
 * Incremental multinomial Naive Bayes over character n-grams and word unigrams.
 *
 * Every time the user marks a message "promo" or "not promo" the model updates
 * its counts on-device. A small exported snapshot (token counts per class) can
 * be persisted as JSON by the data layer.
 */
class NaiveBayesLearner(
    private val minGram: Int = 3,
    private val maxGram: Int = 4,
) {
    private val promoTokenCounts = HashMap<String, Int>()
    private val hamTokenCounts = HashMap<String, Int>()
    private var promoDocs = 0
    private var hamDocs = 0
    private var promoTokens = 0
    private var hamTokens = 0

    /** Total labelled examples seen. */
    val trainingSamples: Int get() = promoDocs + hamDocs

    fun tokens(text: String): List<String> {
        val normalized = TextNormalizer.normalize(text)
        val words = normalized.split(' ').filter { it.isNotBlank() }
        val out = ArrayList<String>(words.size * 4)
        out += words.map { "w:$it" }
        val compact = normalized.replace(" ", "_")
        for (n in minGram..maxGram) {
            if (compact.length < n) continue
            var i = 0
            while (i + n <= compact.length) {
                out += "g$n:" + compact.substring(i, i + n)
                i++
            }
        }
        return out
    }

    /** Fold one labelled example into the model. */
    fun learn(text: String, isPromo: Boolean) {
        val counts = if (isPromo) promoTokenCounts else hamTokenCounts
        for (t in tokens(text)) {
            counts.merge(t, 1, Int::plus)
            if (isPromo) promoTokens++ else hamTokens++
        }
        if (isPromo) promoDocs++ else hamDocs++
    }

    /**
     * Promo probability in [0,1]. Returns 0.5 when the model has seen too few
     * examples to say anything meaningful.
     */
    fun promoProbability(text: String): Float {
        if (promoDocs == 0 || hamDocs == 0) return 0.5f
        val vocab = promoTokenCounts.keys + hamTokenCounts.keys
        val vocabSize = vocab.size.coerceAtLeast(1)

        var logPromo = ln(promoDocs.toDouble() / trainingSamples)
        var logHam = ln(hamDocs.toDouble() / trainingSamples)

        for (t in tokens(text)) {
            val pc = (promoTokenCounts[t] ?: 0) + 1.0
            val hc = (hamTokenCounts[t] ?: 0) + 1.0
            logPromo += ln(pc / (promoTokens + vocabSize))
            logHam += ln(hc / (hamTokens + vocabSize))
        }

        val max = maxOf(logPromo, logHam)
        val p = kotlin.math.exp(logPromo - max)
        val h = kotlin.math.exp(logHam - max)
        return (p / (p + h)).toFloat()
    }

    /** Serializable snapshot for the data layer to persist. */
    fun snapshot(): Snapshot = Snapshot(
        promoTokenCounts = HashMap(promoTokenCounts),
        hamTokenCounts = HashMap(hamTokenCounts),
        promoDocs = promoDocs,
        hamDocs = hamDocs,
    )

    fun restore(snapshot: Snapshot) {
        promoTokenCounts.clear(); promoTokenCounts.putAll(snapshot.promoTokenCounts)
        hamTokenCounts.clear(); hamTokenCounts.putAll(snapshot.hamTokenCounts)
        promoDocs = snapshot.promoDocs
        hamDocs = snapshot.hamDocs
        promoTokens = promoTokenCounts.values.sum()
        hamTokens = hamTokenCounts.values.sum()
    }

    data class Snapshot(
        val promoTokenCounts: Map<String, Int>,
        val hamTokenCounts: Map<String, Int>,
        val promoDocs: Int,
        val hamDocs: Int,
    )
}
