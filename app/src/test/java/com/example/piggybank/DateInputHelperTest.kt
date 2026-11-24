package com.example.piggybank

import androidx.compose.ui.text.AnnotatedString
import com.example.piggybank.util.DateIsoVisualTransformation
import com.example.piggybank.util.formatDateDigitsToIso
import com.example.piggybank.util.isValidCalendarDate
import com.example.piggybank.util.isoDateToUtcMillis
import com.example.piggybank.util.sanitizeDateDigits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateInputHelperTest {

    private val transformation = DateIsoVisualTransformation()

    @Test
    fun sanitizeDateDigitsStripsLettersSymbolsAndCapsAtEight() {
        assertEquals("", sanitizeDateDigits("abc!@#"))
        assertEquals("20261124", sanitizeDateDigits("2026-11-24"))
        assertEquals("20261124", sanitizeDateDigits("20a26/11-24xyz99"))
        assertEquals("202611", sanitizeDateDigits("2026-11"))
    }

    @Test
    fun formatDateDigitsToIsoInsertsDashesProgressively() {
        assertEquals("", formatDateDigitsToIso(""))
        assertEquals("2026", formatDateDigitsToIso("2026"))
        assertEquals("2026-1", formatDateDigitsToIso("20261"))
        assertEquals("2026-11", formatDateDigitsToIso("202611"))
        assertEquals("2026-11-24", formatDateDigitsToIso("20261124"))
    }

    @Test
    fun validatesRealCalendarDatesStrictlyIncludingLeapYears() {
        assertTrue(isValidCalendarDate("2026-11-24"))
        assertTrue(isValidCalendarDate("2024-02-29"))
        assertFalse(isValidCalendarDate("2025-02-29"))
        assertFalse(isValidCalendarDate("2026-02-30"))
        assertFalse(isValidCalendarDate("2026-04-31"))
        assertFalse(isValidCalendarDate("2026-13-01"))
        assertFalse(isValidCalendarDate("2026-00-10"))
        assertFalse(isValidCalendarDate("2026-11-00"))
        assertFalse(isValidCalendarDate("2026-11"))
        assertFalse(isValidCalendarDate("abcd-ef-gh"))
        assertFalse(isValidCalendarDate("1899-12-31"))

        assertNotNull(isoDateToUtcMillis("2026-11-24"))
        assertNull(isoDateToUtcMillis("2026-02-30"))
    }

    @Test
    fun dateVisualTransformationOffsetMappingIsMonotonicAndBounded() {
        val samples = listOf("", "2", "2026", "20261", "202611", "2026112", "20261124")
        samples.forEach { raw ->
            val result = transformation.filter(AnnotatedString(raw))
            val transformed = result.text.text
            val mapping = result.offsetMapping

            assertEquals(formatDateDigitsToIso(raw), transformed)

            var prevTrans = -1
            for (origOffset in 0..raw.length) {
                val mapped = mapping.originalToTransformed(origOffset)
                assertTrue(mapped in 0..transformed.length)
                assertTrue(mapped >= prevTrans)
                prevTrans = mapped
            }
            assertEquals(transformed.length, mapping.originalToTransformed(raw.length))

            var prevOrig = -1
            for (transOffset in 0..transformed.length) {
                val mapped = mapping.transformedToOriginal(transOffset)
                assertTrue(mapped in 0..raw.length)
                assertTrue(mapped >= prevOrig)
                prevOrig = mapped
            }
            assertEquals(raw.length, mapping.transformedToOriginal(transformed.length))
        }
    }
}
