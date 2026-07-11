package com.avtar.cabbilling.util

/**
 * Single source of truth for the invoice-numbering rules:
 *  - sequential from 1, zero-padded to at least 4 digits ("0001", "0142");
 *  - capped at [MAX];
 *  - resets to 1 whenever the target year is newer than the last issued year.
 */
object InvoiceSequence {

    const val MAX = 100_000

    fun nextNumber(lastNumber: Int, lastYear: Int, targetYear: Int): Int =
        if (targetYear > lastYear) 1 else lastNumber + 1

    fun isWithinLimit(number: Int): Boolean = number in 1..MAX

    fun code(number: Int): String = number.toString().padStart(4, '0')
}
