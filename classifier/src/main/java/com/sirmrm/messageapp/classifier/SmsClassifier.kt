package com.sirmrm.messageapp.classifier

/**
 * On-device promo classifier.
 *
 * Layer 1 ([RuleEngine]) produces deterministic verdicts for protected classes
 * (OTP / transactional / whitelisted) and a weighted rule score for promo
 * signals. Layer 2 ([NaiveBayesLearner]) refines the score for everything the
 * rules left undecided, and keeps learning from user feedback.
 *
 * The combined score drives the decision:
 *   >= autoMoveThreshold  → AUTO_MOVE  (to Promotions folder)
 *   >= suggestThreshold   → SUGGEST    (badge + one-tap move)
 *   protected             → PROTECTED  (never moved, never auto-deleted)
 *   otherwise             → KEEP
 */
class SmsClassifier(
    private val config: ClassifierConfig = ClassifierConfig(),
    private val learner: NaiveBayesLearner = NaiveBayesLearner(),
) {
    private val rules = RuleEngine(config)

    /** Weight of the learned model once it has enough samples. */
    private fun learnerWeight(): Float =
        if (learner.trainingSamples >= 10) 0.35f else 0f

    fun classify(input: SmsInput): ClassificationResult {
        val outcome = rules.evaluate(input)

        if (outcome.protected) {
            return ClassificationResult(0f, outcome.category, Decision.PROTECTED, outcome.reasons)
        }

        val nbProb = learner.promoProbability(input.body)
        val w = learnerWeight()
        // Rules dominate; NB only nudges borderline cases up or down.
        val ruleScore = outcome.score
        val combined = when {
            ruleScore > 0f -> (ruleScore * (1f - w) + nbProb * w).coerceIn(0f, 1f)
            else -> if (w > 0f) (nbProb * w + 0.5f * (1f - w)).coerceIn(0f, 1f) else 0f
        }

        val category = when {
            combined >= config.suggestThreshold -> SmsCategory.PROMO
            outcome.category == SmsCategory.UNKNOWN && combined < 0.1f -> SmsCategory.PERSONAL
            else -> SmsCategory.UNKNOWN
        }

        val decision = when {
            combined >= config.autoMoveThreshold -> Decision.AUTO_MOVE
            combined >= config.suggestThreshold -> Decision.SUGGEST
            else -> Decision.KEEP
        }

        val reasons = outcome.reasons.toMutableList()
        if (w > 0f && nbProb > 0.6f && outcome.category == SmsCategory.UNKNOWN) {
            reasons += Reason("model", "مدل یادگیرنده مشابه پیام‌های تبلیغاتی قبلی", "Similar to promos you marked before")
        }

        return ClassificationResult(combined, category, decision, reasons)
    }

    /** Learn from explicit user feedback. */
    fun learn(input: SmsInput, isPromo: Boolean) = learner.learn(input.body, isPromo)

    fun snapshot(): NaiveBayesLearner.Snapshot = learner.snapshot()
    fun restore(snapshot: NaiveBayesLearner.Snapshot) = learner.restore(snapshot)
}
