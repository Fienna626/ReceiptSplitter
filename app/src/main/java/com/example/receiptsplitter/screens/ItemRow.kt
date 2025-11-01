package com.example.receiptsplitter.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.receiptsplitter.data.ReceiptItem
import java.util.Locale
import java.util.UUID

@Composable
fun ItemRow(
    item: ReceiptItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        onClick = onClick, // This will open the EditItemDialog
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Clear, "Delete", tint = MaterialTheme.colorScheme.error)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(item.name.ifBlank { "New Item" }, style = MaterialTheme.typography.bodyLarge)

                if (item.assignedPeople.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    val names = item.assignedPeople.joinToString(", ") { it.name.take(4).uppercase() }
                    Text(
                        text = "Split by: $names",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(16.dp))
            Text(
                text = "$${String.format(Locale.US, "%.2f", item.price)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}