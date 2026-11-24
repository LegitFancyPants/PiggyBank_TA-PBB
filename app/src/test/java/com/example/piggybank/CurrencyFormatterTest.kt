package com.example.piggybank

import androidx.compose.ui.text.AnnotatedString
import com.example.piggybank.util.ThousandSeparatorVisualTransformation
import com.example.piggybank.util.sanitizeNominalInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyFormatterTest {

    private val transformation = ThousandSeparatorVisualTransformation()

    @Test
    fun sanitizeNominalInputFiltersNonDigitsAndLeadingZeros() {
        assertEquals("", sanitizeNominalInput(""))
        assertEquals("0", sanitizeNominalInput("0"))
        assertEquals("0", sanitizeNominalInput("000"))
        assertEquals("5", sanitizeNominalInput("05"))
        assertEquals("1500000", sanitizeNominalInput("Rp 1.500.000"))
        assertEquals("123456789012345", sanitizeNominalInput("123456789012345678"))
    }

    @Test
    fun formatsThousandsMillionsAndBillionsWithDots() {
        val cases = mapOf(
            "" to "",
            "0" to "0",
            "50" to "50",
            "500" to "500",
            "1000" to "1.000",
            "15000" to "15.000",
            "150000" to "150.000",
            "1500000" to "1.500.000",
            "25000000" to "25.000.000",
            "1000000000" to "1.000.000.000"
        )

        cases.forEach { (raw, expected) ->
            val result = transformation.filter(AnnotatedString(raw))
            assertEquals(expected, result.text.text)
        }
    }

    @Test
    fun offsetMappingIsMonotonicAndBoundedAcrossAllOffsets() {
        val samples = listOf("", "5", "100", "1000", "15000", "150000", "1500000", "1234567890")
        samples.forEach { raw ->
            val result = transformation.filter(AnnotatedString(raw))
            val transformed = result.text.text
            val mapping = result.offsetMapping

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
