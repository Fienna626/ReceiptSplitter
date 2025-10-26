package com.example.receiptsplitter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.receiptsplitter.data.converters.Converters

// List the Entity class here
@Database(entities = [SavedReceiptEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class) // Reference the converter
abstract class AppDatabase : RoomDatabase() {

    abstract fun receiptDao(): ReceiptDao // Reference the DAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "receipt_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}