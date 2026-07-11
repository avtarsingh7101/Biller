package com.avtar.cabbilling.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class Faq(val question: String, val answer: String)

private val FAQS = listOf(
    Faq("How do I create a new invoice?", "Go to the \"New Bill\" tab, fill in the trip details (from, to, car, amount), and tap the Save button. Your invoice is generated instantly with an auto-incrementing number."),
    Faq("Where are my invoices stored?", "All data is stored locally on your device in a secure database. No cloud, no server — your data never leaves your phone."),
    Faq("Can I share invoices on WhatsApp?", "Yes! Open any invoice from the Ledger or Calendar, then tap the green \"Share on WhatsApp\" button. You can also share as PDF or plain text."),
    Faq("How does the invoice numbering work?", "Invoices are numbered sequentially starting from 0001. The counter resets automatically at the start of each new year."),
    Faq("Can I change my company name later?", "Yes. Go to Settings > Company Profile and update your company name, phone number, or currency symbol at any time."),
    Faq("How do I add or remove cars?", "Go to Settings > Manage Fleet. You can add new cars with their number plates and remove existing ones."),
    Faq("What is the Trip Logbook?", "The Logbook tab lets you record trips with GPS. It tracks your speed, distance, and route in real-time. Tap Start Trip to begin and Stop Trip when you arrive."),
    Faq("Does the app need internet?", "No. Core billing works fully offline. Internet is only used for the location search feature (finding cities/places) and is completely optional."),
    Faq("How do I change my PIN?", "Go to Settings > Security > Change PIN. Enter your new 4-6 digit PIN and confirm it."),
    Faq("Can I recover deleted invoices?", "No. Deleted invoices are permanently removed. Please be careful when deleting."),
    Faq("How do I reset the app?", "Go to Settings > Danger Zone > Reset App. This erases ALL data including invoices, profile, and PIN. This cannot be undone."),
    Faq("Does the app track my location?", "Location is only used when you actively start a trip in the Logbook, or when you tap \"Use current location\" in the location picker. The app never tracks you in the background.")
)

@Composable
fun FaqScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("FAQs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Frequently Asked Questions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))

            FAQS.forEachIndexed { index, faq ->
                FaqItem(faq)
                if (index < FAQS.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FaqItem(faq: Faq) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                faq.question,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                faq.answer,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
