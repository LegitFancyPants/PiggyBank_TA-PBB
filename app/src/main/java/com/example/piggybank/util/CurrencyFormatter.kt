package com.example.piggybank.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Suppress("DEPRECATION")
fun Long.toRupiah(prefix: String = ""): String {
    val formatter = NumberFormat.getNumberInstance(Locale("in", "ID")).apply {
        isGroupingUsed = true
        maximumFractionDigits = 0
    }
    val formattedNumber = formatter.format(abs(this))
    val effectivePrefix = if (prefix.isEmpty() && this < 0L) "-" else prefix
    return "${effectivePrefix}Rp $formattedNumber"
}

fun sanitizeNominalInput(input: String, maxDigits: Int = 15): String {
    val digits = input.filter { it.isDigit() }
    if (digits.isEmpty()) return ""
    val normalized = digits.trimStart('0').ifEmpty { "0" }
    return normalized.take(maxDigits)
}

class ThousandSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val sb = StringBuilder(original.length + original.length / 3)
        val origToTrans = IntArray(original.length + 1)
        for (i in original.indices) {
            origToTrans[i] = sb.length
            if (i > 0 && (original.length - i) % 3 == 0) {
                sb.append('.')
            }
            sb.append(original[i])
        }
        val transformed = sb.toString()
        origToTrans[original.length] = transformed.length

        val transToOrig = IntArray(transformed.length + 1)
        var digitCount = 0
        for (i in transformed.indices) {
            transToOrig[i] = digitCount
            if (transformed[i] != '.') {
                digitCount++
            }
        }
        transToOrig[transformed.length] = original.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                origToTrans[offset.coerceIn(0, original.length)]

            override fun transformedToOriginal(offset: Int): Int =
                transToOrig[offset.coerceIn(0, transformed.length)]
        }

        return TransformedText(AnnotatedString(transformed), offsetMapping)
    }
}
