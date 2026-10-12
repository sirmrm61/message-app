package com.sirmrm.messageapp.core.util

import com.sirmrm.messageapp.domain.model.CalendarMode
import com.sirmrm.messageapp.domain.model.DigitStyle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Gregorian <-> Jalali (Persian) civil calendar conversion and date display.
 * Algorithm: standard jalaali arithmetic (valid for the years 1178..2241 GH).
 */
object JalaliDate {

    private val MONTH_NAMES = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    fun format(timestamp: Long, mode: CalendarMode, digits: DigitStyle): String {
        return when (mode) {
            CalendarMode.GREGORIAN -> {
                val s = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(Date(timestamp))
                if (digits == DigitStyle.PERSIAN) PersianDigits.run { s.toPersianDigits() } else s
            }
            CalendarMode.JALALI -> {
                val j = gregorianToJalali(timestamp)
                val s = "%d %s %d".format(j.day, MONTH_NAMES[j.month - 1], j.year)
                if (digits == DigitStyle.PERSIAN) PersianDigits.run { s.toPersianDigits() } else s
            }
        }
    }

    fun formatTime(timestamp: Long, digits: DigitStyle): String {
        val s = SimpleDateFormat("HH:mm", Locale.ENGLISH).format(Date(timestamp))
        return if (digits == DigitStyle.PERSIAN) PersianDigits.run { s.toPersianDigits() } else s
    }

    /** Smart list timestamp: time today, weekday this week, date otherwise. */
    fun formatListDate(timestamp: Long, mode: CalendarMode, digits: DigitStyle): String {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = timestamp }
        return when {
            sameDay(now, then) -> formatTime(timestamp, digits)
            now.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR) < 7 &&
                now.get(Calendar.YEAR) == then.get(Calendar.YEAR) -> {
                val s = SimpleDateFormat("EEEE", Locale("fa")).format(Date(timestamp))
                s
            }
            else -> format(timestamp, mode, digits)
        }
    }

    private fun sameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    data class Jalali(val year: Int, val month: Int, val day: Int)

    fun gregorianToJalali(timestamp: Long): Jalali {
        val c = Calendar.getInstance().apply { timeInMillis = timestamp }
        return gregorianToJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Jalali {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        var gy2 = gy - 1600
        var gm2 = gm - 1
        var gd2 = gd - 1

        var gDayNo = 365 * gy2 + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400
        for (i in 0 until gm2) gDayNo += gDaysInMonth[i]
        if (gm2 > 1 && (gy2 % 4 == 0 && gy2 % 100 != 0 || gy2 % 400 == 0)) gDayNo++
        gDayNo += gd2

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461
        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        while (jm < 11 && jDayNo >= jDaysInMonth[jm]) {
            jDayNo -= jDaysInMonth[jm]
            jm++
        }
        return Jalali(jy, jm + 1, jDayNo + 1)
    }
}
