package com.example.receiptsplitter.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.receiptsplitter.data.PersonTotal // Import needed data class
import java.util.Date // For timestamp
import java.text.SimpleDateFormat
import java.util.Locale

// Simple data class to represent a saved receipt summary
// In a real app, this would come from a database
data class SavedReceiptSummary(
    val id: String = java.util.UUID.randomUUID().toString(), // Unique ID
    val description: String, // e.g., "Dinner at..." or a timestamp
    val timestamp: Long = System.currentTimeMillis(),
    val grandTotal: Double,
    val personTotals: List<PersonTotal> // Store the calculated breakdown
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    savedReceipts: List<SavedReceiptSummary>, // List of saved receipts
    onNavigateToSplitter: () -> Unit, // Action to go to the splitting screen
    onDeleteReceipt: (SavedReceiptSummary) -> Unit, // Action to delete a saved receipt
    onReceiptClick: (SavedReceiptSummary) -> Unit // Action when a receipt is clicked (for viewing later)
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Receipt Splitter") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToSplitter) {
                Icon(Icons.Filled.Add, contentDescription = "Split New Receipt")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (savedReceipts.isEmpty()) {
                item {
                    Text(
                        "No saved receipts. Tap '+' to add one!",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(savedReceipts, key = { it.id }) { receipt ->
                    SavedReceiptCard(
                        receipt = receipt,
                        onDelete = { onDeleteReceipt(receipt) },
                        onClick = { onReceiptClick(receipt) }
                    )
                }
            }
        }
    }
}

@Composable
fun SavedReceiptCard(
    receipt: SavedReceiptSummary,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    val dateString = dateFormat.format(Date(receipt.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // Make the card clickable
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(receipt.description.ifEmpty { "Receipt from $dateString" }, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Total: $${String.format(Locale.US, "%.2f", receipt.grandTotal)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "People: ${receipt.personTotals.joinToString { it.person.name }}", // Show who split it
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Clear, contentDescription = "Delete Receipt", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}