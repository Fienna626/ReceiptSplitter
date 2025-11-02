package com.example.receiptsplitter.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.receiptsplitter.data.PersonTotal
import com.example.receiptsplitter.data.ReceiptItem
import java.util.Locale

// ---  UI for the Person Breakdown dialog ---
@Composable
fun BreakdownDialog(
    personTotal: PersonTotal,
    allItems: List<ReceiptItem>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface // Or surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // --- Title ---
                    Text(
                        text = "${personTotal.person.name}'s Bill",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    // --- List of their items ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp) // Make it scrollable if too long
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Find all items this person is assigned to
                        allItems.forEach { item ->
                            if (item.assignedPeople.contains(personTotal.person)) {
                                // Calculate this person's share of the item
                                val share = item.price / item.assignedPeople.size
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.name)
                                    Text("$${String.format(Locale.US, "%.2f", share)}")
                                }
                            }
                        }
                    }

                    // --- Subtotal ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "$${String.format(Locale.US, "%.2f", personTotal.subtotal)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // --- Tax + Tip ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tax Share", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "$${String.format(Locale.US, "%.2f", personTotal.taxShare)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tip Share", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "$${String.format(Locale.US, "%.2f", personTotal.tipShare)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // --- Final Total ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Owed", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "$${String.format(Locale.US, "%.2f", personTotal.totalOwed)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // --- Dismiss Button ---
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}