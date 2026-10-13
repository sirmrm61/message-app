package com.sirmrm.messageapp.classifier.tts

/**
 * Normalizes SMS text for the Persian TTS engine:
 * digits & amounts → words, Jalali dates → spoken form, masked card/account
 * numbers → digit-by-digit, links → «یک پیوند», emoji/extra marks stripped,
 * ی/ک + ZWNJ unified, and a trailing pause appended per sentence
 * (Piper fa voices clip the last syllable otherwise).
 */
object TtsTextPreprocessor {

    private val FA_DIGITS = "۰۱۲۳۴۵۶۷۸۹"
    private val AR_DIGITS = "٠١٢٣٤٥٦٧٨٩"
    private val URL = Regex("""https?://\S+|www\.\S+""")
    private val MASKED = Regex("""[*xXـ]{2,}\d+|\d+[*xX]{2,}\d*|\*{4,}\d{3,4}""")
    private val JALALI_DATE = Regex("""\b(13|14)\d{2}/(\d{1,2})/(\d{1,2})\b""")
    private val AMOUNT = Regex("""(\+|-)?\s*(\d[\d,٬،]*)\s*(ریال|تومان|rial|toman|تومن)""", RegexOption.IGNORE_CASE)
    private val NUMBER = Regex("""\b\d[\d,٬،]*\b""")
    private val EMOJI = Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+|[\\u2600-\\u27BF]+|[\\uFE0F]")
    private val MONTHS_FA = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    /** Single pass — same normalization used by preprocessing and tests. */
    fun normalize(text: String): String {
        var t = text
        // unify ی/ک + digits → latin for matching
        t = t.replace('ي', 'ی').replace('ك', 'ک').replace('‌', ' ')
        t = t.map { c ->
            val i = FA_DIGITS.indexOf(c)
            if (i >= 0) '0' + i else {
                val j = AR_DIGITS.indexOf(c)
                if (j >= 0) '0' + j else c
            }
        }.joinToString("")
        t = EMOJI.replace(t, " ")
        t = URL.replace(t, "یک پیوند")
        t = MASKED.replace(t) { m ->
            m.value.filter { it.isDigit() }.toCharArray().joinToString(" ") { "$it" }
        }
        t = JALALI_DATE.replace(t) { m ->
            val y = m.groupValues[1] + m.value.substring(1, 4)
            val mo = m.groupValues[2].toIntOrNull() ?: 0
            val d = m.groupValues[3].toIntOrNull() ?: 0
            val month = MONTHS_FA.getOrElse(mo - 1) { "ماه " + FaNumberToWords.of(mo.toLong()) }
            "${FaNumberToWords.of(d.toLong())} $month ${FaNumberToWords.of(y.toLong())}"
        }
        t = AMOUNT.replace(t) { m ->
            val sign = if (m.groupValues[1] == "+") "به‌علاوه " else if (m.groupValues[1] == "-") "منفی " else ""
            val num = m.groupValues[2].replace(Regex("[,٬،]"), "").toLongOrNull() ?: 0L
            val unit = if (m.groupValues[3].startsWith("ریال", true) || m.groupValues[3].equals("rial", true)) "ریال" else "تومان"
            "$sign${FaNumberToWords.of(num)} $unit"
        }
        t = NUMBER.replace(t) { m ->
            val num = m.value.replace(Regex("[,٬،]"), "")
            runCatching { FaNumberToWords.of(num.toLong()) }.getOrDefault(m.value)
        }
        t = t.replace(Regex("[!؟?]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return t
    }

    /**
     * Full preprocessing for synthesis: normalize, chunk long sentences, append
     * a full stop + a short silence marker per sentence (Piper fa clips the
     * final syllable otherwise).
     */
    fun preprocess(text: String): List<String> {
        val normalized = normalize(text)
        if (normalized.isEmpty()) return emptyList()
        val sentences = normalized.split(Regex("(?<=[.؛\\n])"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val chunks = mutableListOf<String>()
        for (s in sentences) {
            // chunk very long sentences for a fast streaming start
            if (s.length > 200) {
                var i = 0
                while (i < s.length) {
                    val end = minOf(i + 200, s.length)
                    var cut = s.lastIndexOf(' ', end - 1)
                    if (cut <= i) cut = end
                    chunks.add(s.substring(i, cut).trim())
                    i = cut
                }
            } else chunks.add(s)
        }
        return chunks.map { if (it.endsWith('.')) it else "$it." }
    }
}
