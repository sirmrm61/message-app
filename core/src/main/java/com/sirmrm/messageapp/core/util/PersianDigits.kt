package com.sirmrm.messageapp.core.util

import com.sirmrm.messageapp.domain.model.DigitStyle

/** Converts between Latin and Persian digits for display. */
object PersianDigits {

    private val FA = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun String.toPersianDigits(): String {
        val sb = StringBuilder(length)
        for (c in this) sb.append(if (c in '0'..'9') FA[c - '0'] else c)
        return sb.toString()
    }

    fun format(value: Any, style: DigitStyle): String =
        if (style == DigitStyle.PERSIAN) value.toString().toPersianDigits() else value.toString()
}
