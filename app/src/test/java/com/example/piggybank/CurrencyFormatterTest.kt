package com.example.piggybank

import com.example.piggybank.util.formatDisplayDate
import com.example.piggybank.util.toRupiah
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun formatsPositiveAmountWithIndonesianGrouping() {
        assertEquals("Rp 2.450.000", 2_450_000L.toRupiah())
        assertEquals("Rp 0", 0L.toRupiah())
    }

    @Test
    fun formatsWithExplicitPrefix() {
        assertEquals("-Rp 18.000", 18_000L.toRupiah("-"))
        assertEquals("+Rp 2.000.000", 2_000_000L.toRupiah("+"))
    }

    @Test
    fun formatsNegativeAmountCleanly() {
        assertEquals("-Rp 50.000", (-50_000L).toRupiah())
    }

    @Test
    fun formatsIsoDateIntoReadableIndonesianDate() {
        assertEquals("22 Nov 2026", formatDisplayDate("2026-11-22"))
        assertEquals("5 Mei 2026", formatDisplayDate("2026-05-05"))
        assertEquals("invalid-date", formatDisplayDate("invalid-date"))
    }
}
