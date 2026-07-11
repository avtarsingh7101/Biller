package com.avtar.cabbilling.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "New Bill", Icons.Filled.ReceiptLong),
    LEDGER("ledger", "Ledger", Icons.Filled.ListAlt),
    CALENDAR("calendar", "Calendar", Icons.Filled.CalendarMonth),
    LOGBOOK("logbook", "Logbook", Icons.Filled.Route),
    SETTINGS("settings", "Settings", Icons.Filled.Settings)
}

object Routes {
    const val BILL_DETAIL = "bill/{billId}"
    fun billDetail(billId: Long) = "bill/$billId"
    const val ARG_BILL_ID = "billId"
    const val FAQ = "faq"
}
