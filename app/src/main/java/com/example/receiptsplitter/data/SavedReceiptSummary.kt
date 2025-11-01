package com.example.receiptsplitter.data // Or the correct package you chose

// Defines the data structure for displaying a saved receipt in the UI
data class SavedReceiptSummary(
    val id: String = java.util.UUID.randomUUID().toString(),
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val grandTotal: Double,
    val personTotals: List<PersonTotal>,
    val items: List<ReceiptItem>
)