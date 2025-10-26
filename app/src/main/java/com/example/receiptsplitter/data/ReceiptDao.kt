package com.example.receiptsplitter.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow // For observing changes

@Dao
interface ReceiptDao {
    // Returns a Flow of Entities from the database
    @Query("SELECT * FROM saved_receipts ORDER BY timestamp DESC")
    fun getAllReceipts(): Flow<List<SavedReceiptEntity>> // <-- Use Entity

    // Inserts an Entity into the database
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: SavedReceiptEntity) // <-- Use Entity

    // Deletes an Entity from the database
    @Delete
    suspend fun deleteReceipt(receipt: SavedReceiptEntity) // <-- Use Entity
}