package com.example.util

import kotlin.math.abs

object CurrencyUtils {

    /**
     * Formats integer paise into Indian Rupee format with standard Indian grouping:
     * e.g. 12500000 paise -> ₹1,25,000.00
     *      5000 paise -> ₹50.00
     * Handles negative values if any.
     */
    fun formatInr(paise: Long, showSign: Boolean = false): String {
        val isNegative = paise < 0
        val absPaise = abs(paise)
        val rupees = absPaise / 100
        val paiseRemainder = absPaise % 100

        val formattedRupees = formatIndianGrouping(rupees)
        val formattedPaise = String.format("%02d", paiseRemainder)

        val sign = when {
            isNegative -> "- "
            showSign && paise > 0 -> "+ "
            else -> ""
        }

        return "${sign}₹$formattedRupees.$formattedPaise"
    }

    /**
     * Indian grouping rule:
     * Last 3 digits grouped together, then preceding digits grouped by 2.
     * e.g. 1234567 -> 12,34,567
     *      100000 -> 1,00,000
     */
    fun formatIndianGrouping(value: Long): String {
        val s = value.toString()
        if (s.length <= 3) return s

        val last3 = s.takeLast(3)
        val remaining = s.dropLast(3)

        val result = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            result.insert(0, remaining[i])
            count++
            if (count % 2 == 0 && i != 0) {
                result.insert(0, ',')
            }
        }
        return "$result,$last3"
    }

    /**
     * Parses a string input (e.g. "1250", "1250.50", "1,250.75") into integer paise.
     * Returns null if invalid or <= 0.
     */
    fun parseAmountToPaise(input: String): Long? {
        val clean = input.trim().replace(",", "").replace("₹", "").trim()
        if (clean.isEmpty()) return null

        return try {
            val parts = clean.split(".")
            if (parts.size > 2) return null

            val rupees = parts[0].toLongOrNull() ?: return null
            if (rupees < 0) return null

            val paise = if (parts.size == 2) {
                val pStr = parts[1].padEnd(2, '0').take(2)
                pStr.toLongOrNull() ?: return null
            } else {
                0L
            }

            val totalPaise = rupees * 100 + paise
            if (totalPaise <= 0) null else totalPaise
        } catch (_: Exception) {
            null
        }
    }

    fun paiseToRupeeString(paise: Long): String {
        val rupees = paise / 100
        val rem = abs(paise % 100)
        return if (rem == 0L) {
            rupees.toString()
        } else {
            "$rupees.${String.format("%02d", rem)}"
        }
    }
}
