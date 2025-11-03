package com.example.receiptsplitter.data

import com.example.receiptsplitter.data.SavedReceiptSummary // Or .screens.SavedReceiptSummary if you put it there
import com.example.receiptsplitter.data.SavedReceiptEntity

// --- Mapper Functions ---

// Converts a Database Entity TO a UI Model (SavedReceiptSummary)
fun SavedReceiptEntity.toSummary(): SavedReceiptSummary {
    return SavedReceiptSummary(
        id = this.id,
        description = this.description,
        timestamp = this.timestamp,
        grandTotal = this.grandTotal,
        personTotals = this.personTotals, // Direct mapping works because Room handles the list via Converters
        items = this.items
    )
}

// Converts a UI Model (SavedReceiptSummary) TO a Database Entity
// Needed for Deleting or Updating based on the UI model
fun SavedReceiptSummary.toEntity(): SavedReceiptEntity {
    return SavedReceiptEntity(
        id = this.id, // Use the existing ID from the UI model
        description = this.description,
        timestamp = this.timestamp,
        grandTotal = this.grandTotal,
        personTotals = this.personTotals,
        items = this.items
    )
}