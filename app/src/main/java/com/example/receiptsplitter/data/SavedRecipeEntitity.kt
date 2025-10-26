package com.example.receiptsplitter.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.receiptsplitter.data.converters.Converters // Import converter

@Entity(tableName = "saved_receipts") // Define table name
@TypeConverters(Converters::class) // Tell Room to use our converter
data class SavedReceiptEntity(
    @PrimaryKey val id: String, // Use the existing String ID as primary key
    val description: String,
    val timestamp: Long,
    val grandTotal: Double,
    val personTotals: List<PersonTotal> // Room will use the converter for this
)