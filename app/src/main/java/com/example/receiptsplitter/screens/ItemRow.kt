package com.example.receiptsplitter.screens


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.receiptsplitter.data.ReceiptItem
import java.util.Locale
import java.util.UUID


// --- UI for a single item row ---
@Composable
fun ItemRow(
    item: ReceiptItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    selectedPersonId: UUID? // <-- NEW
) {
    // Check if the currently selected person is assigned to this item
    val isAssignedToSelected = item.assignedPeople.any { it.id == selectedPersonId }

    Card( // Use a Card for better click feedback
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selectedPersonId != null && isAssignedToSelected) {
                MaterialTheme.colorScheme.tertiaryContainer // Highlight if assigned
            } else if (selectedPersonId != null) {
                MaterialTheme.colorScheme.surfaceVariant // Dim if not assigned (in selection mode)
            } else {
                MaterialTheme.colorScheme.surface // Default
            }
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
                Text(item.name, style = MaterialTheme.typography.bodyLarge)

                // Show initials of assigned people
                if (item.assignedPeople.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    val initials = item.assignedPeople.joinToString(", ") { it.name.take(4).uppercase() }
                    Text(
                        text = "Split by: $initials",
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
