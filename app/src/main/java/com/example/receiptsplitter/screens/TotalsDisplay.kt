package com.example.receiptsplitter.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.receiptsplitter.data.PersonTotal
import java.util.Locale


// --- UI for the Final Totals card ---
@Composable
fun TotalsDisplay(totals: List<PersonTotal>, onPersonClick: (PersonTotal) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Totals Per Person",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // A row for each person
            totals.forEach { personTotal ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onPersonClick(personTotal) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) { // <-- Add Column for detail
                        Text(
                            text = personTotal.person.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Sub: $${String.format(Locale.US, "%.2f", personTotal.subtotal)} " +
                                    "Tax: $${String.format(Locale.US, "%.2f", personTotal.taxShare)} " +
                                    "Tip: $${String.format(Locale.US, "%.2f", personTotal.tipShare)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", personTotal.totalOwed)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // --- Grand Total ---
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            val grandTotal = totals.sumOf { it.totalOwed }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Grand Total",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$${String.format(Locale.US, "%.2f", grandTotal)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}