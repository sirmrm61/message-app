package com.sirmrm.messageapp.classifier.tts

/** Converts non-negative integers to spoken Persian words. */
object FaNumberToWords {

    private val ONES = arrayOf(
        "", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه",
        "ده", "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده",
        "هفده", "هجده", "نوزده",
    )
    private val TENS = arrayOf(
        "", "", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود",
    )
    private val HUNDREDS = arrayOf(
        "", "صد", "دویست", "سیصد", "چهارصد", "پانصد",
        "ششصد", "هفتصد", "هشتصد", "نهصد",
    )
    private val SCALES = arrayOf("", " هزار", " میلیون", " میلیارد", " بیلیون")

    /** e.g. 150000 → «صد و پنجاه هزار» */
    fun of(value: Long): String {
        if (value == 0L) return "صفر"
        if (value < 0) return "منفی " + of(-value)
        val parts = mutableListOf<String>()
        var v = value
        var scale = 0
        while (v > 0 && scale < SCALES.size) {
            val chunk = (v % 1000).toInt()
            if (chunk > 0) parts.add(0, under1000(chunk) + SCALES[scale])
            v /= 1000
            scale++
        }
        return parts.joinToString(" و ")
    }

    private fun under1000(n: Int): String {
        val parts = mutableListOf<String>()
        val h = n / 100
        if (h > 0) parts.add(HUNDREDS[h])
        val rem = n % 100
        when {
            rem == 0 -> {}
            rem < 20 -> parts.add(ONES[rem])
            else -> {
                val t = rem / 10
                val o = rem % 10
                parts.add(if (o > 0) TENS[t] + " و " + ONES[o] else TENS[t])
            }
        }
        return parts.joinToString(" و ")
    }
}
