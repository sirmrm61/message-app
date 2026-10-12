package com.sirmrm.messageapp.classifier

/**
 * Parses Iranian bank SMS formats into a transaction record.
 *
 * Typical bodies (fa), e.g. بانک ملت/ملی/پاسارگاد/سامان/سپه/صادرات:
 *   "واریز:12,500,000 ريال  حساب:12345  موجودی:30,000,000"
 *   "برداشت از حساب ۵۰۰,۰۰۰ تومان  مانده ۱,۲۰۰,۰۰۰"
 *   "خريد موفق 75,000 ريال کارت 6037**"
 * English: "Debit:1,000,000 IRR", "Credit 50000 Toman"
 */
object BankTxParser {

    enum class Direction { DEPOSIT, WITHDRAWAL, UNKNOWN }

    data class BankTx(
        val direction: Direction,
        /** Amount in rials; null when the amount could not be parsed. */
        val amountRials: Long?,
        val currencyWord: String?,
    )

    /** True when the body looks like a bank transaction message. */
    fun isBankMessage(body: String): Boolean {
        val t = TextNormalizer.normalize(body)
        return BANK_HINTS.any { it.containsMatchIn(t) }
    }

    fun parse(body: String): BankTx? {
        val t = TextNormalizer.normalize(body)
        if (!BANK_HINTS.any { it.containsMatchIn(t) }) return null

        val direction = when {
            DEPOSIT_WORDS.containsMatchIn(t) -> Direction.DEPOSIT
            WITHDRAWAL_WORDS.containsMatchIn(t) -> Direction.WITHDRAWAL
            else -> Direction.UNKNOWN
        }

        val raw = extractAmount(t, direction)
        val currencyWord = CURRENCY.find(t)?.value
        // «تومان» = 10 rials.
        val amountRials = if (raw != null && currencyWord != null &&
            (currencyWord.contains("تومان") || currencyWord.contains("تومن") ||
                currencyWord.equals("toman", ignoreCase = true))
        ) raw * 10 else raw
        return BankTx(direction, amountRials, currencyWord)
    }

    /** Amount belongs to the direction clause, not the trailing balance. */
    private fun extractAmount(t: String, direction: Direction): Long? {
        val balanceIdx = BALANCE_WORD.find(t)?.range?.first ?: t.length
        val dirMatch = when (direction) {
            Direction.DEPOSIT -> DEPOSIT_WORDS.find(t)
            Direction.WITHDRAWAL -> WITHDRAWAL_WORDS.find(t)
            Direction.UNKNOWN -> null
        }

        fun toLong(m: MatchResult): Long? {
            val digits = m.value.filter { it.isDigit() }
            return digits.toLongOrNull()
        }

        // Prefer a number right after the direction keyword, before مانده.
        if (dirMatch != null) {
            val from = dirMatch.range.last + 1
            if (from < balanceIdx) {
                val after = t.substring(from, balanceIdx)
                AMOUNT.find(after)?.let { return toLong(it) }
            }
        }
        // Otherwise any number before the balance clause.
        AMOUNT.find(t.substring(0, balanceIdx))?.let { return toLong(it) }
        return null
    }

    /** Parses numeric tokens like 12,500,000 / ۵۰۰,۰۰۰ / 50000. */
    private val AMOUNT = Regex("""\d[\d,\u066C\u066B\u060C\s]*\d""")

    private val CURRENCY = Regex("""ريال|ریال|تومان|تومن|IRR|toman|rial""")

    private val BALANCE_WORD = Regex("""مانده\s*(حساب|کارت|مبلغ|ریال|تومان|[:؛=\-]|\d)|موجودي|موجودی|balance|باقيمانده\s*(حساب|مبلغ|[:؛])|باقیمانده\s*(حساب|مبلغ|[:؛])""")

    private val DEPOSIT_WORDS = Regex(
        """واريز|واریز|افزایش\s*(اعتبار|موجودی|موجودي)?|دریافت\s*(موفق)?\s*[:؛]?\s*\d|deposit|credit\b"""
    )

    private val WITHDRAWAL_WORDS = Regex(
        """برداشت|کسر\s*(از|مبلغ)|خرید\s*(موفق|مبلغ|به\s*مبلغ|کارت|از\s*حساب|[:؛])|خروج\s*وجه|پرداخت\s*(موفق)?\s*[:؛]?\s*\d|debit|withdraw|purchase"""
    )

    private val BANK_HINTS = listOf(
        DEPOSIT_WORDS,
        WITHDRAWAL_WORDS,
        BALANCE_WORD,
        Regex("""حساب\s*[:#]?\s*\d|کارت\s*[*\d]{4,}|شتاب|ساتنا|پایا|حواله|کارت\s*به\s*کارت"""),
    )
}
