package com.sirmrm.messageapp.classifier

import org.json.JSONArray
import org.json.JSONObject

/**
 * Versioned, JSON-driven bank/financial SMS patterns.
 *
 * The canonical file is `data/src/main/assets/patterns/banking_fa.json`.
 * The data layer reads it from Android assets at runtime; unit tests read the
 * same file from the repo checkout. Users can add/remove custom sender rules
 * on top (stored in Room, merged at classification time) — nothing is
 * hard-coded except these defaults.
 */
data class BankEntry(
    val id: String,
    val fa: String,
    val en: String,
    val senders: List<String>,
    val keywords: List<String>,
    val unverified: Boolean,
)

data class Thresholds(
    val transactional: Double,
    val maybe: Double,
    val learnedSenderMinStructured: Int,
)

data class Weights(
    val amountSigned: Double,
    val amountWorded: Double,
    val balanceWord: Double,
    val maskedCardOrAccount: Double,
    val jalaliDatetime: Double,
    val refOrSerial: Double,
    val vocabularyHit: Double,
    val vocabularyMax: Double,
    val knownBankSender: Double,
    val bankNameInText: Double,
    val learnedBankSender: Double,
    val promoSignalPenalty: Double,
    val promoWordPenalty: Double,
    val securityEventHit: Double,
    val promoExemptIfAmountAndBalance: Boolean,
)

data class StructurePatterns(
    val amountSigned: Regex,
    val amountAny: Regex,
    val balance: Regex,
    val maskedCardOrAccount: Regex,
    val jalaliDatetime: Regex,
    val refOrSerial: Regex,
    val otpCode: Regex,
    val securityEvent: Regex,
)

data class PhishingPatterns(
    val shortLinks: Regex,
    val threatOrPrizeWords: Regex,
    val bankImpersonation: Regex,
    val otpWithoutWarning: Boolean,
)

data class PromoNegativePatterns(
    val unsubscribe: Regex,
    val promoWords: Regex,
)

data class BankingPatterns(
    val version: Int,
    val thresholds: Thresholds,
    val weights: Weights,
    val banks: List<BankEntry>,
    val structure: StructurePatterns,
    val vocabulary: Map<String, List<String>>,
    val phishing: PhishingPatterns,
    val promoNegative: PromoNegativePatterns,
) {
    /** Sender-id → bank lookup (senders are normalized digits or words). */
    fun bankForSender(normalizedSender: String): BankEntry? {
        val s = normalizedSender.lowercase()
        return banks.firstOrNull { b ->
            b.senders.any { it.lowercase() == s || (it.length >= 4 && s.contains(it.lowercase())) }
        }
    }

    /**
     * Bank found via its name inside the message text. Short keywords
     * («دی», «ملت», «سپه»…) require word boundaries so ordinary words like
     * «گفتی» or «دادی» never match.
     */
    fun bankForText(normalizedText: String): BankEntry? =
        banks.firstOrNull { b ->
            b.keywords.any { kw ->
                if (kw.length >= 4) normalizedText.contains(kw)
                else Regex("(^|[^\\p{L}])${Regex.escape(kw)}([^\\p{L}]|$)")
                    .containsMatchIn(normalizedText)
            }
        }

    companion object {
        fun load(json: String): BankingPatterns = parse(JSONObject(json))

        private fun JSONObject.strList(key: String): List<String> {
            val a = optJSONArray(key) ?: return emptyList()
            return List(a.length()) { a.getString(it) }
        }

        private fun JSONArray.toStrings(): List<String> = List(length()) { getString(it) }

        private fun parse(o: JSONObject): BankingPatterns {
            val t = o.getJSONObject("thresholds")
            val w = o.getJSONObject("weights")
            val s = o.getJSONObject("structure")
            val p = o.getJSONObject("phishing_signals")
            val n = o.getJSONObject("promo_negative")
            val banks = o.getJSONArray("banks").let { arr ->
                (0 until arr.length()).map { i ->
                    val b = arr.getJSONObject(i)
                    BankEntry(
                        id = b.getString("id"),
                        fa = b.getString("fa"),
                        en = b.getString("en"),
                        senders = b.strList("senders"),
                        keywords = b.strList("keywords"),
                        unverified = b.optBoolean("unverified"),
                    )
                }
            }
            val vocab = mutableMapOf<String, List<String>>()
            val v = o.getJSONObject("vocabulary")
            for (k in v.keys()) vocab[k] = v.getJSONArray(k).toStrings()

            return BankingPatterns(
                version = o.getInt("version"),
                thresholds = Thresholds(
                    transactional = t.getDouble("transactional"),
                    maybe = t.getDouble("maybe"),
                    learnedSenderMinStructured = t.getInt("learned_sender_min_structured_messages"),
                ),
                weights = Weights(
                    amountSigned = w.getDouble("amount_signed"),
                    amountWorded = w.getDouble("amount_worded"),
                    balanceWord = w.getDouble("balance_word"),
                    maskedCardOrAccount = w.getDouble("masked_card_or_account"),
                    jalaliDatetime = w.getDouble("jalali_datetime"),
                    refOrSerial = w.getDouble("ref_or_serial"),
                    vocabularyHit = w.getDouble("vocabulary_hit"),
                    vocabularyMax = w.getDouble("vocabulary_max"),
                    knownBankSender = w.getDouble("known_bank_sender"),
                    bankNameInText = w.optDouble("bank_name_in_text", w.getDouble("known_bank_sender")),
                    learnedBankSender = w.getDouble("learned_bank_sender"),
                    promoSignalPenalty = w.getDouble("promo_signal_penalty"),
                    promoWordPenalty = w.getDouble("promo_word_penalty"),
                    securityEventHit = w.optDouble("security_event_hit", 0.20),
                    promoExemptIfAmountAndBalance = w.getBoolean("promo_exempt_if_amount_and_balance"),
                ),
                banks = banks,
                structure = StructurePatterns(
                    amountSigned = Regex(s.getString("amount_signed")),
                    amountAny = Regex(s.getString("amount_any")),
                    balance = Regex(s.getString("balance")),
                    maskedCardOrAccount = Regex(s.getString("masked_card_or_account")),
                    jalaliDatetime = Regex(s.getString("jalali_datetime")),
                    refOrSerial = Regex(s.getString("ref_or_serial")),
                    otpCode = Regex(s.getString("otp_code")),
                    securityEvent = Regex(s.optString("security_event", "@@none@@")),
                ),
                vocabulary = vocab,
                phishing = PhishingPatterns(
                    shortLinks = Regex(p.getString("short_links")),
                    threatOrPrizeWords = Regex(p.getString("threat_or_prize_words")),
                    bankImpersonation = Regex(p.getString("bank_impersonation")),
                    otpWithoutWarning = p.optBoolean("otp_without_warning", true),
                ),
                promoNegative = PromoNegativePatterns(
                    unsubscribe = Regex(n.getString("unsubscribe")),
                    promoWords = Regex(n.getString("promo_words")),
                ),
            )
        }

        /**
         * Minimal built-in fallback used only if the JSON cannot be loaded
         * (e.g. corrupted asset). Every production path loads the real file.
         */
        fun fallback(): BankingPatterns = BankingPatterns(
            version = 0,
            thresholds = Thresholds(0.5, 0.3, 3),
            weights = Weights(0.25, 0.18, 0.22, 0.15, 0.10, 0.10, 0.15, 0.45, 0.30, 0.30, 0.25, -0.50, -0.10, 0.20, true),
            banks = emptyList(),
            structure = StructurePatterns(
                amountSigned = Regex("""[+\-−]\s*\d[\d,٬،\s]*\d"""),
                amountAny = Regex("""\d[\d,٬،\s]{0,14}\d\s*(ريال|ریال|تومان|تومن|irr|toman|rial)"""),
                balance = Regex("""مانده|موجودي|موجودی|balance"""),
                maskedCardOrAccount = Regex("""حساب\s*[:#]?\s*\d|کارت\s*[\d*\s]{4,}|\*+\s*\d{3,4}"""),
                jalaliDatetime = Regex("""14\d\d[/\-.]\d{1,2}[/\-.]\d{1,2}"""),
                refOrSerial = Regex("""پیگیری|مرجع|سریال"""),
                otpCode = Regex("""رمز\s*پویا|رمز\s*یک\s*بار\s*مصرف|کد\s*تایید|otp"""),
                securityEvent = Regex("""ورود به اینترنت\s*بانک|تغییر رمز|فعال.?سازی|رمز دوم|خروج موفق|غیرفعال شد"""),
            ),
            vocabulary = mapOf(
                "deposit" to listOf("واريز", "واریز", "deposit"),
                "withdrawal" to listOf("برداشت", "debit", "withdraw"),
            ),
            phishing = PhishingPatterns(
                shortLinks = Regex("""bit\.ly|tinyurl"""),
                threatOrPrizeWords = Regex("""مسدود|جایزه|برنده"""),
                bankImpersonation = Regex("""بانک|حساب"""),
                otpWithoutWarning = true,
            ),
            promoNegative = PromoNegativePatterns(
                unsubscribe = Regex("""لغو\s*\d+|unsubscribe"""),
                promoWords = Regex("""تخفیف|جشنواره|فروش|قرعه"""),
            ),
        )
    }
}
