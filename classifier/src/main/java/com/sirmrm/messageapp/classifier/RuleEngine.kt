package com.sirmrm.messageapp.classifier

/**
 * Layer 1 of the classifier: deterministic rules, evaluated top to bottom.
 *
 * Priority order:
 *  1. Explicit user sender rules (always / never promo)
 *  2. Whitelist — saved contacts and previously-conversed senders
 *  3. OTP / transactional detectors — these win over every promo signal and
 *     are what keeps the "OTP wrongly marked as promo" error at zero
 *  4. Weighted promo signals (keywords, unsubscribe markers, service sender
 *     ids, short links, marketing-from-unknown-number)
 */
internal class RuleEngine(private val config: ClassifierConfig) {

    data class RuleOutcome(
        val category: SmsCategory,
        val score: Float,
        val protected: Boolean,
        val reasons: List<Reason>,
    )

    private data class Signal(
        val weight: Float,
        val reason: Reason,
        val patterns: List<Regex>,
    )

    fun evaluate(input: SmsInput): RuleOutcome {
        val text = TextNormalizer.normalize(input.body)
        val sender = TextNormalizer.normalizeSender(input.sender)

        // 1 — user rules
        when (input.senderRule) {
            SenderRule.NEVER_PROMO -> return RuleOutcome(
                SmsCategory.PERSONAL, 0f, true,
                listOf(Reason("user_never", "شما این فرستنده را «تبلیغات نیست» علامت زده‌اید", "You marked this sender as not promo")),
            )
            SenderRule.ALWAYS_PROMO -> return RuleOutcome(
                SmsCategory.PROMO, 1f, false,
                listOf(Reason("user_always", "شما این فرستنده را «همیشه تبلیغات» علامت زده‌اید", "You marked this sender as always promo")),
            )
            null -> Unit
        }

        // 2 — whitelist
        if (input.isContact) return RuleOutcome(
            SmsCategory.PERSONAL, 0f, true,
            listOf(Reason("contact", "فرستنده در مخاطبان شماست", "Sender is in your contacts")),
        )

        // 3 — protected classes first: OTP, then bank transactions.
        // Bank formats outrank the conversation whitelist: replying once to a
        // bank thread must not turn later bank SMS into "personal".
        val otpReasons = matchAny(text, OTP_PATTERNS)
        if (otpReasons.isNotEmpty()) {
            return RuleOutcome(SmsCategory.OTP, 0f, true, otpReasons)
        }
        val transactionalReasons = matchAny(text, TRANSACTIONAL_PATTERNS).toMutableList()
        if (transactionalReasons.isEmpty() && BankTxParser.isBankMessage(text)) {
            transactionalReasons += Reason("bank_format", "قالب پیامک بانکی", "Bank SMS format")
        }
        if (transactionalReasons.isNotEmpty()) {
            return RuleOutcome(SmsCategory.TRANSACTIONAL, 0f, true, transactionalReasons)
        }

        if (input.hasConversation) return RuleOutcome(
            SmsCategory.PERSONAL, 0f, true,
            listOf(Reason("conversation", "با این فرستنده قبلاً پیام ردوبدل کرده‌اید", "You have exchanged messages with this sender")),
        )

        // 4 — promo signals
        var score = 0f
        val reasons = ArrayList<Reason>()

        for (signal in PROMO_SIGNALS) {
            if (signal.patterns.any { it.containsMatchIn(text) }) {
                score += signal.weight
                reasons += signal.reason
            }
        }

        val isServiceSender = config.serviceSenderPatterns.any { Regex(it).containsMatchIn(sender) }
        if (isServiceSender) {
            score += 0.30f
            reasons += Reason("service_sender", "سرشماره‌ی خدماتی/انبوه", "Bulk service sender id")
        }

        if (config.shortLinkPatterns.any { Regex(it).containsMatchIn(text) }) {
            score += 0.15f
            reasons += Reason("short_link", "حاوی لینک کوتاه", "Contains a short link")
        }

        if (GENERIC_LINK.containsMatchIn(text)) {
            score += 0.08f
        }

        // Unknown 11-digit Iranian mobile + marketing wording
        if (MOBILE_MSISDN.matches(sender) && reasons.isNotEmpty()) {
            score += 0.10f
            reasons += Reason("unknown_mobile", "شماره‌ی همراه ناشناس با متن بازاریابی", "Unknown mobile number with marketing text")
        }

        val category = if (score > 0f) SmsCategory.PROMO else SmsCategory.UNKNOWN
        return RuleOutcome(category, score.coerceIn(0f, 1f), false, reasons)
    }

    private fun matchAny(text: String, table: List<Pair<Regex, Reason>>): List<Reason> =
        table.filter { it.first.containsMatchIn(text) }.map { it.second }

    companion object {
        private val GENERIC_LINK = Regex("""https?://|www\.""")

        private val MOBILE_MSISDN = Regex("""^(0?9\d{9}|989\d{9})$""")

        // ---- protected: OTP -------------------------------------------------
        private val OTP_PATTERNS: List<Pair<Regex, Reason>> = listOf(
            Regex("رمز\\s*(دوم\\s*)?پویا|رمز\\s*یک\\s*بار\\s*مصرف|رمز\\s*یکبارمصرف|رمز\\s*(تایید|تأیید)") to
                Reason("otp_fa", "حاوی رمز پویا/یک‌بارمصرف", "Contains a one-time password"),
            Regex("کد\\s*(تایید|تأیید|فعال\\s*سازی|ورود|احراز|امنیتی)") to
                Reason("otp_code_fa", "حاوی کد تأیید", "Contains a verification code"),
            Regex("\\b(otp|otc|verification\\s*code|confirm(ation)?\\s*code|verify\\s*code|one[-\\s]?time\\s*(pass)?code|security\\s*code|code\\s*[:#]\\s*\\d{4,8})\\b") to
                Reason("otp_en", "Contains a verification code", "Contains a verification code"),
            Regex("کد\\s*[:：]?\\s*\\d{4,8}\\b") to
                Reason("otp_numeric", "حاوی کد عددی", "Contains a numeric code"),
        )

        // ---- protected: bank transactions only -------------------------------
        // "Transactional" means bank SMS: deposits, withdrawals, card purchases,
        // balance notices and Iranian bank service messages. Postal/billing/
        // appointment texts are NOT transactional and stay in the inbox flow.
        private val TRANSACTIONAL_PATTERNS: List<Pair<Regex, Reason>> = listOf(
            Regex("برداشت|واريز|واریز|مانده|موجودي|موجودی|تراکنش|کارت\\s*به\\s*کارت|حواله|ساتنا|پایا|شتاب|چک\\s*صیادی") to
                Reason("bank_fa", "پیام تراکنش بانکی", "Bank transaction message"),
            Regex("حساب\\s*[:#]?\\s*\\d{3,}|کارت\\s*[\\d*\\s]{4,}|شماره\\s*کارت|شبا\\s*[:#]?\\s*IR\\d") to
                Reason("bank_account_fa", "ارجاع به حساب/کارت بانکی", "Bank account / card reference"),
            Regex("بانک\\s*(ملت|ملی|سامان|سپه|صادرات|پاسارگاد|تجارت|رفاه|پارسیان|اقتصاد|کشاورزی|مسکن|آینده|دی|شهر|گردشگری|مرکزی|سرمایه|خاورمیانه|قوامین|ایران\\s*زمین|رسالت|توسعه|انصار|مهر\\s*ایران)") to
                Reason("bank_name_fa", "نام بانک در متن", "Bank name in text"),
            Regex("\\b(transaction|deposit|withdraw(al)?|balance|debit|credit\\s*(card)?\\s*(purchase|payment)?|card\\s*(ending|purchase)|account\\s*(no|number|balance))\\b") to
                Reason("bank_en", "Bank transaction message", "Bank transaction message"),
        )

        // ---- promo signals ---------------------------------------------------
        private fun strong(code: String, fa: String, en: String, pattern: String) =
            Signal(0.45f, Reason(code, fa, en), listOf(Regex(pattern)))

        private fun mid(code: String, fa: String, en: String, pattern: String) =
            Signal(0.25f, Reason(code, fa, en), listOf(Regex(pattern)))

        private fun weak(code: String, fa: String, en: String, pattern: String) =
            Signal(0.12f, Reason(code, fa, en), listOf(Regex(pattern)))

        private val PROMO_SIGNALS: List<Signal> = listOf(
            // Iranian unsubscribe marker: «لغو ۱۱» / «لغو11» with any digits
            strong("unsub", "حاوی «لغو شماره» (امضای پیامک تبلیغاتی)", "Unsubscribe marker («لغو N»)",
                "لغو\\s*[\\d]+"),
            strong("festival", "جشنواره/فروش ویژه", "Festival / special sale",
                "جشنواره|فروش\\s*ویژه|حراج"),
            mid("discount", "تخفیف/کد تخفیف", "Discount / coupon code",
                "تخفیف|کد\\s*تخفیف|discount|coupon|%\\s*off|off\\b"),
            mid("cta", "فراخوان اقدام فوری", "Urgent call to action",
                "همین\\s*حالا|فرصت\\s*محدود|اقدام\\s*کنید|ثبت\\s*نام|کلیک\\s*کنید| برای\\s*عضویت|عضویت\\s*در|act\\s*now|limited\\s*time|sign\\s*up|register\\s*now"),
            mid("prize", "جایزه/قرعه‌کشی", "Prize / lottery",
                "جایزه|قرعه\\s*کشی|برنده\\s*شوید|winner|lottery|prize"),
            mid("sale_words", "واژگان بازاریابی", "Marketing vocabulary",
                "خرید\\s*(اینترنتی|آنلاین|ویژه)|پیشنهاد\\s*ویژه|قیمت\\s*استثنایی|ارسال\\s*رایگان|free\\s*(shipping|gift)|special\\s*offer|buy\\s*now|شگفت\\s*انگیز"),
            weak("free_charge", "شارژ/هدیه رایگان", "Free credit / gift",
                "شارژ\\s*رایگان|هدیه\\s*رایگان|رایگان\\s*دریافت|free\\s*charge|free\\s*recharge"),
            weak("visit", "دعوت به بازدید/نصب", "Visit / install invitation",
                "بازدید\\s*کنید|دانلود\\s*کنید|نصب\\s*کنید|visit\\s*us|download\\s*now"),
            weak("percent", "درصد تخفیف عددی", "Numeric percent-off",
                "\\b\\d{1,2}\\s*(%|درصد)\\s*(تخفیف|off)"),
        )
    }
}
