package com.example.receiptsplitter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters // <-- Import
import com.example.receiptsplitter.data.converters.Converters // <-- Import

@Database(entities = [SavedReceiptEntity::class], version = 2, exportSchema = false) // Use version 2
@TypeConverters(Converters::class) // <-- THIS ANNOTATION IS REQUIRED HERE TOO
abstract class AppDatabase : RoomDatabase() {

    abstract fun receiptDao(): ReceiptDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "receipt_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}