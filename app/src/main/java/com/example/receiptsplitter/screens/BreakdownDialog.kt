package com.example.receiptsplitter.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.receiptsplitter.data.PersonTotal
import com.example.receiptsplitter.data.ReceiptItem
import java.util.Locale

@Composable
fun BreakdownDialog(
    personTotal: PersonTotal,
    allItems: List<ReceiptItem>,
    onDismiss: () -> Unit
) {
    val itemsForThisPerson = allItems.filter { it.assignedPeople.contains(personTotal.person) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "${personTotal.person.name}'s Breakdown",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                // --- Itemized list ---
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                        .heightIn(max = 200.dp) // Constrain height for scrolling
                ) {
                    itemsForThisPerson.forEach { item ->
                        val pricePerPerson = item.price / item.assignedPeople.size
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "$${String.format(Locale.US, "%.2f", pricePerPerson)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // --- Subtotal ---
                SummaryRow("Subtotal",
                    personTotal.subtotal,
                    MaterialTheme.colorScheme.onSurfaceVariant)

                // Separate Tax and Tip into two rows
                SummaryRow(
                    label = "Tax Share:",
                    amount = personTotal.taxShare,
                    MaterialTheme.colorScheme.onSurfaceVariant
                )

                SummaryRow(
                    label = "Tip Share:",
                    amount = personTotal.tipShare,
                    MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // --- Grand Total ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Owed:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "$${String.format(Locale.US, "%.2f", personTotal.totalOwed)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, amount: Double, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Note: The color parameter is now used for the label text color
        Text(label, style = MaterialTheme.typography.bodyMedium, color = color) // again took an hour
        Text(
            "$${String.format(Locale.US, "%.2f", amount)}",
            style = MaterialTheme.typography.bodyMedium,
            // Keep amount text dark (onSurface) or use the label color (color)
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}