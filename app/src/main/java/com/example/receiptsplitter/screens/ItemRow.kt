package com.example.receiptsplitter.screens


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.receiptsplitter.data.Person
import com.example.receiptsplitter.data.ReceiptItem




// --- UI for a single item row ---
@Composable
fun ItemRow(
    item: ReceiptItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit // <-- Renamed from onLongClick
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // 1. The Delete Button
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.Clear, // This is the 'X' icon
                contentDescription = "Delete Item",
                tint = MaterialTheme.colorScheme.error // Make it red
            )
        }

        // 2. The Clickable Item Info (for editing)
        Column(
            modifier = Modifier
                .weight(1f) // This makes the clickable area fill the rest of the space
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp), // Padding is now here
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "$${String.format(java.util.Locale.US, "%.2f", item.price)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            // --- NEW: Show assigned people's initials ---
            if (item.assignedPeople.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                val initials = item.assignedPeople.joinToString(", ") {
                    it.name.take(2).uppercase() // Get first 2 letters
                }
                Text(
                    text = "Split by: $initials",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
