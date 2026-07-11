package com.avtar.cabbilling.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Centralised, thread-safe formatting helpers. Kept off the composables so the
 * UI never builds [NumberFormat]/[DateTimeFormatter] instances during layout.
 */
object Formatters {

    private val INDIA = Locale("en", "IN")

    val dateFull: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    val dateShort: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)
    val dateTime: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy · hh:mm a", Locale.ENGLISH)
    val monthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

    /** e.g. 125000.5 -> "₹1,25,000.50" (Indian digit grouping). */
    fun amount(value: Double, symbol: String = "₹"): String {
        val nf = NumberFormat.getNumberInstance(INDIA).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return symbol + nf.format(value)
    }

    /** Compact form for dense summaries, e.g. "₹1,25,000". */
    fun amountCompact(value: Double, symbol: String = "₹"): String {
        val nf = NumberFormat.getNumberInstance(INDIA).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
        return symbol + nf.format(value)
    }

    /** Abbreviated amount for very tight spaces: ₹450, ₹4.5k, ₹1.2L, ₹3Cr (Indian scale). */
    fun amountShort(value: Double, symbol: String = "₹"): String {
        fun trim(d: Double): String =
            String.format(Locale.ENGLISH, "%.1f", d).removeSuffix(".0")
        return when {
            value >= 1_00_00_000 -> symbol + trim(value / 1_00_00_000) + "Cr"
            value >= 1_00_000 -> symbol + trim(value / 1_00_000) + "L"
            value >= 1_000 -> symbol + trim(value / 1_000) + "k"
            else -> symbol + value.toInt()
        }
    }

    fun localDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()

    fun localDateTime(epochMillis: Long): LocalDateTime =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()

    fun startOfDayMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
