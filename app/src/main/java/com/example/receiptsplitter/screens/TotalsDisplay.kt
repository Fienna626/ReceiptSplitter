package com.example.receiptsplitter.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.receiptsplitter.data.PersonTotal
import java.util.Locale

@Composable
fun TotalsDisplay(
    totals: List<PersonTotal>,
    onPersonClick: (PersonTotal) -> Unit,
    modifier: Modifier = Modifier
) {
    // The Card expands due to the 'modifier' (which contains weight(1f))
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize() // Column now fills the expanded Card
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Fixed Header Content
            Text(
                text = "Totals Per Person",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

            // 2. Scrollable List of Person Totals
            // This LazyColumn takes the remaining vertical space (- header - footer)
            // and provides scrolling.
            LazyColumn(
                modifier = Modifier.weight(1f), // Critical: Takes available space
                verticalArrangement = Arrangement.spacedBy(8.dp),
                // Padding is added here to avoid clipping the end of the scrollable list
                contentPadding = PaddingValues(bottom = 4.dp)
            ) {
                items(totals) { personTotal -> // Use items for proper LazyColumn usage
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPersonClick(personTotal) } // Clickable row
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Left Column: Name and Details
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = personTotal.person.name,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Sub: $${String.format(Locale.US, "%.2f", personTotal.subtotal)} | " +
                                        "Tax: $${String.format(Locale.US, "%.2f", personTotal.taxShare)} | " +
                                        "Tip: $${String.format(Locale.US, "%.2f", personTotal.tipShare)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Right Side: Total Owed
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", personTotal.totalOwed)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } // End LazyColumn

            // 3. Fixed Footer Content
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            val grandTotal = totals.sumOf { it.totalOwed }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Grand Total",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "$${String.format(Locale.US, "%.2f", grandTotal)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}