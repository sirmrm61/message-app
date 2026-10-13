package com.sirmrm.messageapp.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JalaliDateTest {

    @Test
    fun `jalali to gregorian round-trips over year edges`() {
        // Includes Esfand (leap/non-leap) and Farvardin boundaries.
        val cases = listOf(
            Triple(1403, 12, 29), Triple(1403, 12, 30),
            Triple(1404, 1, 1), Triple(1404, 6, 31),
            Triple(1404, 7, 1), Triple(1404, 12, 29),
            Triple(1399, 12, 30), Triple(1400, 1, 1),
        )
        for ((y, m, d) in cases) {
            val t = JalaliDate.jalaliToGregorianMillis(y, m, d)
            val back = JalaliDate.gregorianToJalali(t)
            assertEquals("jalali $y/$m/$d", JalaliDate.Jalali(y, m, d), back)
        }
    }

    @Test
    fun `month lengths are sane`() {
        for (y in 1400..1410) {
            for (m in 1..12) {
                val days = JalaliDate.jalaliMonthDays(y, m)
                if (m <= 6) assertEquals(31, days)
                else if (m <= 11) assertEquals(30, days)
                else assertTrue("esfand $y: $days", days == 29 || days == 30)
            }
        }
    }
}
