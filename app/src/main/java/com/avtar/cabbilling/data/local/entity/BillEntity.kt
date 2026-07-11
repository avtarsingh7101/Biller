package com.avtar.cabbilling.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single permanently-stored invoice.
 *
 * [invoiceNumber] is the raw sequential value (1..100000) and [invoiceCode] is
 * its zero-padded display form (e.g. 5 -> "0005"). Both are stored so the ledger
 * can sort/search on either without re-deriving the padding at read time.
 */
@Entity(
    tableName = "bills",
    indices = [
        Index(value = ["year"]),
        Index(value = ["dateEpochMillis"]),
        Index(value = ["invoiceNumber", "year"], unique = true)
    ]
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val invoiceNumber: Int,
    val invoiceCode: String,

    val companyName: String,
    val carName: String,
    val carPlate: String? = null,

    val fromLocation: String,
    val toLocation: String,
    val passengerName: String = "",

    val amount: Double,
    val notes: String = "",

    /** Trip date, normalised to the start of the day in the device's zone. */
    val dateEpochMillis: Long,

    /** Calendar year of [dateEpochMillis]; drives the yearly-reset sequence. */
    val year: Int,

    /** Wall-clock time the record was actually written. */
    val createdAtMillis: Long
)
