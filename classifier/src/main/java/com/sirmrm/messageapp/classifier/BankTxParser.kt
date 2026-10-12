package com.sirmrm.messageapp.classifier

/**
 * Weighted bank/financial SMS scorer, driven by [BankingPatterns] (the
 * versioned banking_fa.json). Replaces single-condition detection:
 *
 *  - Structural signals: signed amount (+/−), balance word, masked
 *    card/account, Jalali date/time near amounts, ref/serial numbers.
 *  - Vocabulary: deposit / withdrawal / purchase / transfer / cheque /
 *    loan / OTP / security groups, each adding one weight.
 *  - Sender hints: known bank sender-ids or senders the user taught the
 *    app (≥N bank-structured messages) — a positive weight only, never a
 *    hard decision (bulk promos ride the same sender ids).
 *  - Negative weight: unsubscribe markers («لغو ۱۱») and promo words, unless
 *    the message also carries amount + balance.
 *  - Phishing: bank impersonation + short link / threat / prize words →
 *    suspicious, never transactional.
 *
 * Verdict thresholds come from the JSON: score ≥ transactional → transactional;
 * maybe ≤ score < transactional → flagged «maybe transactional» (kept in
 * inbox, suggestive move); below → not bank.
 */
object BankTxParser {

    enum class Direction { DEPOSIT, WITHDRAWAL, UNKNOWN }

    enum class TxSubtype {
        DEPOSIT, WITHDRAWAL, PURCHASE_PAYMENT, OTP, CHEQUE, LOAN,
        SECURITY, INFO, UNKNOWN
    }

    data class BankTx(
        val direction: Direction,
        /** Amount in rials; null when the amount could not be parsed. */
        val amountRials: Long?,
        val currencyWord: String?,
    )

    data class Verdict(
        val score: Double,
        val isTransactional: Boolean,
        /** True when score sits in the maybe band: keep in inbox, suggest. */
        val maybe: Boolean,
        val suspicious: Boolean,
        val subtype: TxSubtype,
        val direction: Direction,
        val amountRials: Long?,
        val balanceRials: Long?,
        /** fa/en name of the matched bank, if any. */
        val bankName: String?,
        val reasons: List<String>,
    )

    /** Static patterns — set once by [SmsClassifier] construction. */
    @Volatile
    var patterns: BankingPatterns = BankingPatterns.fallback()

    /** True when the body looks like a bank transaction message. */
    fun isBankMessage(body: String): Boolean =
        evaluate(body).score >= patterns.thresholds.maybe

    fun parse(body: String): BankTx? {
        val v = evaluate(body)
        if (!v.isTransactional && !v.maybe) return null
        return BankTx(v.direction, v.amountRials, v.currencyWord())
    }

    private fun Verdict.currencyWord(): String? = null // superseded by amount parse

    /** Extracts the OTP/verification digits from a message body, if present. */
    fun extractOtp(body: String): String? {
        val t = TextNormalizer.normalize(body)
        val m = patterns.structure.otpCode.find(t) ?: return null
        // Prefer the numeric run inside the match.
        val digits = Regex("\\d{4,8}").find(m.value)?.value
            ?: Regex("\\d{4,8}").find(t)?.value
        return digits
    }

    // -- evaluation -----------------------------------------------------------

    fun evaluate(
        body: String,
        senderNormalized: String = "",
        knownBankSenders: Set<String> = emptySet(),
        customBankSenders: Collection<String> = emptyList(),
    ): Verdict {
        val t = TextNormalizer.normalize(body)
        val p = patterns
        var score = 0.0
        val reasons = ArrayList<String>()

        // ---------- structure ----------
        val signedAmount = p.structure.amountSigned.find(t)
        val amount = extractAmount(t, signedAmount)
        if (signedAmount != null) { score += p.weights.amountSigned; reasons += "signed_amount" }
        else if (amount != null) { score += p.weights.amountWorded; reasons += "amount" }

        val balanceM = p.structure.balance.find(t)
        val balance = balanceM?.let { extractNumberAfter(t, it.range.last + 1) }
        if (balanceM != null) { score += p.weights.balanceWord; reasons += "balance" }

        if (p.structure.maskedCardOrAccount.containsMatchIn(t)) {
            score += p.weights.maskedCardOrAccount; reasons += "card_or_account"
        }
        if (p.structure.jalaliDatetime.containsMatchIn(t) && amount != null) {
            score += p.weights.jalaliDatetime; reasons += "jalali_datetime"
        }
        if (p.structure.refOrSerial.containsMatchIn(t)) {
            score += p.weights.refOrSerial; reasons += "ref_or_serial"
        }
        if (p.structure.securityEvent.containsMatchIn(t)) {
            score += p.weights.securityEventHit; reasons += "security_event"
        }

        // ---------- vocabulary ----------
        val matchedGroups = mutableMapOf<String, Int>()
        for ((group, words) in p.vocabulary) {
            for (w in words) if (t.contains(w)) { matchedGroups[group] = (matchedGroups[group] ?: 0) + 1 }
        }
        val vocabScore = (matchedGroups.size * p.weights.vocabularyHit).coerceAtMost(p.weights.vocabularyMax)
        score += vocabScore
        reasons += matchedGroups.keys

        // ---------- sender ----------
        val s = senderNormalized.lowercase()
        val bankBySender = if (s.isNotEmpty()) p.bankForSender(s) else null
        val learned = s.isNotEmpty() && s in knownBankSenders
        val custom = s.isNotEmpty() && customBankSenders.any { s.contains(it.lowercase()) }
        when {
            bankBySender != null -> { score += p.weights.knownBankSender; reasons += "known_bank_sender" }
            custom -> { score += p.weights.learnedBankSender; reasons += "custom_bank_sender" }
            learned -> { score += p.weights.learnedBankSender; reasons += "learned_bank_sender" }
        }

        // Bank name inside the text ("بانک ملت:", "پاسارگاد:") is a
        // sender-level signal even when the sender id is unknown.
        val bankByText = if (bankBySender == null) p.bankForText(t) else null
        if (bankByText != null) { score += p.weights.bankNameInText; reasons += "bank_name_in_text" }

        // ---------- promo negative ----------
        val unsub = p.promoNegative.unsubscribe.containsMatchIn(t)
        val promoWord = p.promoNegative.promoWords.containsMatchIn(t)
        val exempt = p.weights.promoExemptIfAmountAndBalance && amount != null && balanceM != null
        if (!exempt) {
            if (unsub) { score += p.weights.promoSignalPenalty; reasons += "unsub_penalty" }
            if (promoWord) { score += p.weights.promoWordPenalty; reasons += "promo_word_penalty" }
        }

        // ---------- phishing ----------
        val strongStructure = amount != null || balanceM != null
        val suspicious = isSuspicious(t, s, bankBySender != null || learned || custom, strongStructure)
        if (suspicious) reasons += "phishing"

        // ---------- direction / subtype ----------
        val direction = directionOf(t, signedAmount)
        val subtype = subtypeOf(t, direction, matchedGroups.keys)

        val bankMatch = bankBySender ?: bankByText
        val bankName = bankMatch?.let { "${it.fa} (${it.en})" }

        val final = score.coerceAtLeast(0.0)
        val isTransactional = !suspicious && final >= p.thresholds.transactional &&
            (amount != null || balanceM != null || matchedGroups.isNotEmpty() || bankMatch != null)
        val maybe = !suspicious && !isTransactional && final >= p.thresholds.maybe

        return Verdict(
            score = final,
            isTransactional = isTransactional,
            maybe = maybe,
            suspicious = suspicious,
            subtype = subtype,
            direction = direction,
            amountRials = amount,
            balanceRials = balance,
            bankName = bankName,
            reasons = reasons,
        )
    }

    // -- phishing -------------------------------------------------------------

    private fun isSuspicious(text: String, sender: String, trustedSender: Boolean, strongStructure: Boolean): Boolean {
        val p = patterns.phishing
        val hasShortLink = p.shortLinks.containsMatchIn(text)
        val impersonates = p.bankImpersonation.containsMatchIn(text)
        val threat = p.threatOrPrizeWords.containsMatchIn(text)
        // Bank-imitating message with a short link, or explicit threat/prize bait.
        // Threat/prize wording is only a phishing signal when the message
        // lacks real transaction structure (real payout notices exist too).
        if ((hasShortLink && impersonates) || (threat && impersonates && !strongStructure)) return true
        // OTP code with no «don't share» warning from an untrusted sender,
        // wrapped in a lure (short link or threat/prize wording).
        if (p.otpWithoutWarning && !trustedSender &&
            patterns.structure.otpCode.containsMatchIn(text) &&
            !text.contains("به کسی ندهید") && !text.contains("افشا") &&
            !text.contains("اشتراک") && sender.isNotEmpty() &&
            sender.any { it.isDigit() } &&
            (hasShortLink || threat)
        ) return true
        return false
    }

    // -- extraction -----------------------------------------------------------

    /** Signed/unsigned first amount (in rials). تومان counts ×10. */
    internal fun extractAmount(t: String, signedMatch: MatchResult?): Long? {
        // Prefer the signed amount; otherwise first amount before balance clause.
        val p = patterns
        val balanceIdx = p.structure.balance.find(t)?.range?.first ?: t.length
        val span = if (signedMatch != null) signedMatch.value
        else p.structure.amountAny.find(t.substring(0, balanceIdx))?.value ?: return null
        val raw = span.filter { it.isDigit() }.toLongOrNull() ?: return null
        val unit = CurrencyUnit.of(t) ?: CurrencyUnit.of(span)
        return if (unit == CurrencyUnit.TOMAN) raw * 10 else raw
    }

    private fun extractNumberAfter(t: String, from: Int): Long? {
        if (from >= t.length) return null
        val m = Regex("""\d[\d,٬،\s]{0,14}\d""").find(t.substring(from)) ?: return null
        val raw = m.value.filter { it.isDigit() }.toLongOrNull() ?: return null
        val unit = CurrencyUnit.of(t.substring(from))
        return if (unit == CurrencyUnit.TOMAN) raw * 10 else raw
    }

    private enum class CurrencyUnit { RIAL, TOMAN;
        companion object {
            fun of(t: String): CurrencyUnit? = when {
                t.contains("تومان") || t.contains("تومن") || t.contains("toman") -> TOMAN
                t.contains("ريال") || t.contains("ریال") || t.contains("rial") || t.contains("irr") -> RIAL
                else -> null
            }
        }
    }

    // -- direction & subtype ---------------------------------------------------

    private fun directionOf(t: String, signed: MatchResult?): Direction {
        if (signed != null) {
            val sign = signed.value.trimStart()[0]
            if (sign == '-') return Direction.WITHDRAWAL
            if (sign == '+') return Direction.DEPOSIT
        }
        val dep = patterns.vocabulary["deposit"].orEmpty().any { t.contains(it) }
        val wd = patterns.vocabulary["withdrawal"].orEmpty().any { t.contains(it) }
        val pp = patterns.vocabulary["purchase_payment"].orEmpty().any { t.contains(it) }
        return when {
            dep && !wd && !pp -> Direction.DEPOSIT
            wd || pp -> Direction.WITHDRAWAL
            else -> Direction.UNKNOWN
        }
    }

    private fun subtypeOf(t: String, dir: Direction, groups: Set<String>): TxSubtype = when {
        patterns.structure.otpCode.containsMatchIn(t) -> TxSubtype.OTP
        "security" in groups -> TxSubtype.SECURITY
        "cheque" in groups -> TxSubtype.CHEQUE
        "loan" in groups -> TxSubtype.LOAN
        "purchase_payment" in groups -> TxSubtype.PURCHASE_PAYMENT
        dir == Direction.DEPOSIT || "deposit" in groups -> TxSubtype.DEPOSIT
        dir == Direction.WITHDRAWAL || "withdrawal" in groups -> TxSubtype.WITHDRAWAL
        groups.isNotEmpty() -> TxSubtype.INFO
        else -> TxSubtype.UNKNOWN
    }
}
