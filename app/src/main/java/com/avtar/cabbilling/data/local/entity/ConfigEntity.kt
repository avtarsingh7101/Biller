package com.avtar.cabbilling.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table (id is pinned to [SINGLETON_ID]) holding the one-time setup
 * data, the PIN credential, the active theme and the invoice sequence counters.
 */
@Entity(tableName = "config")
data class ConfigEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,

    val companyName: String = "",

    // Nullable so the v1→v2 migration can add it with no default (existing rows = NULL).
    val companyPhone: String? = null,

    val carNames: List<String> = emptyList(),
    val currencySymbol: String = "₹",

    /** PBKDF2 hash of the PIN (Base64) and its per-user salt (Base64). */
    val pinHash: String = "",
    val pinSalt: String = "",

    val theme: String = "AUTO",
    val isSetupComplete: Boolean = false,

    /** Last issued invoice number and the year it was issued in. */
    val lastInvoiceNumber: Int = 0,
    val lastInvoiceYear: Int = 0
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
