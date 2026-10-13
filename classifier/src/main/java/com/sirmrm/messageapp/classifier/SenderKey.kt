package com.sirmrm.messageapp.classifier

/**
 * Canonical sender key used for user rules and learned data:
 *  - MSISDNs -> E.164-style "+98..." (strips a leading 0, spaces and dashes;
 *    a bare 10-digit Iranian mobile becomes +989xxxxxxxxx).
 *  - Alphabetic / service senders -> lowercased, whitespace stripped.
 *  - Short codes (3-6 digits) are kept as-is.
 */
object SenderKey {
    fun of(raw: String): String {
        val t = raw.trim()
        if (t.isEmpty()) return t
        if (t.any { it.isLetter() }) {
            return t.filter { it.isLetterOrDigit() }.lowercase()
        }
        var d = t.filter { it.isDigit() }
        when {
            d.startsWith("0098") -> d = "+" + d.removePrefix("00")
            d.startsWith("98") && d.length == 12 -> d = "+$d"
            d.startsWith("0") && d.length >= 10 -> d = "+98" + d.drop(1) // 09xx -> +989xx
            d.startsWith("9") && d.length == 10 -> d = "+98$d"           // 9xx -> +989xx
        }
        return d
    }
}
