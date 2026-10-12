package com.sirmrm.messageapp.classifier

/**
 * Normalizes SMS text before matching so Persian / Arabic / English variants
 * compare equal.
 *
 * - Arabic ی (U+064A) and ك (U+0643) become Persian ی (U+06CC) and ک (U+06A9)
 * - Persian (۰-۹) and Arabic-Indic (٠-٩) digits become ASCII digits
 * - Tatweel (U+0640), ZWNJ (U+200C) and ZWJ (U+200D) are removed
 *   (so «ثبت‌نام» == «ثبت نام» == «ثبتنام»)
 * - Alef / heh / yeh variants are folded to their plain form
 * - Latin letters are lower-cased and whitespace is collapsed
 */
object TextNormalizer {

    private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private val ARABIC_INDIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    private const val ARABIC_YEH = 'ي'      // ی
    private const val ARABIC_KEHEH = 'ك'    // ک
    private const val ZWNJ = '‌'            // نیم‌فاصله
    private const val ZWJ = '‍'             // joiner
    private const val TATWEEL = 'ـ'         // کشیده

    fun normalize(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            val persianIdx = PERSIAN_DIGITS.indexOf(c)
            val arabicIdx = ARABIC_INDIC_DIGITS.indexOf(c)
            when {
                persianIdx >= 0 -> sb.append('0' + persianIdx)
                arabicIdx >= 0 -> sb.append('0' + arabicIdx)
                c == ARABIC_YEH -> sb.append('ی')
                c == ARABIC_KEHEH -> sb.append('ک')
                c == ZWNJ || c == ZWJ || c == TATWEEL -> { /* drop */ }
                c == 'أ' || c == 'إ' -> sb.append('ا')
                c == 'ة' -> sb.append('ه')
                c == 'ى' -> sb.append('ی')
                c == 'ؤ' -> sb.append('و')
                c == 'ئ' -> sb.append('ی')
                else -> sb.append(c.lowercaseChar())
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    /** Normalize a sender id to digits only (+ stripped, 0098 -> 98). */
    fun normalizeSender(sender: String): String {
        val b = StringBuilder(sender.length)
        for (c in sender) {
            val persianIdx = PERSIAN_DIGITS.indexOf(c)
            val arabicIdx = ARABIC_INDIC_DIGITS.indexOf(c)
            when {
                persianIdx >= 0 -> b.append('0' + persianIdx)
                arabicIdx >= 0 -> b.append('0' + arabicIdx)
                c.isDigit() -> b.append(c)
                // Alphabetic sender ids exist too (e.g. "BankMeli"): keep
                // latin letters so bank-name senders can be matched.
                c in 'a'..'z' || c in 'A'..'Z' -> b.append(c.lowercaseChar())
            }
        }
        var s = b.toString()
        if (s.startsWith("0098")) s = s.removePrefix("00")
        return s
    }
}
