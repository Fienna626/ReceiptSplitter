package com.example.receiptsplitter.data.converters

import androidx.room.TypeConverter
import com.example.receiptsplitter.data.PersonTotal
import com.example.receiptsplitter.data.ReceiptItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson() // Re-use Gson instance

    // --- Converters for List<PersonTotal> ---
    @TypeConverter
    fun fromPersonTotalList(value: List<PersonTotal>?): String? {
        if (value == null) {
            return null
        }
        val type = object : TypeToken<List<PersonTotal>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toPersonTotalList(value: String?): List<PersonTotal>? {
        if (value == null) {
            return null
        }
        val type = object : TypeToken<List<PersonTotal>>() {}.type
        return gson.fromJson(value, type)
    }

    // --- Converters for List<ReceiptItem> ---
    @TypeConverter
    fun fromReceiptItemList(value: List<ReceiptItem>?): String? {
        if (value == null) {
            return null
        }
        val type = object : TypeToken<List<ReceiptItem>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toReceiptItemList(value: String?): List<ReceiptItem>? {
        if (value == null) {
            return null
        }
        val type = object : TypeToken<List<ReceiptItem>>() {}.type
        return gson.fromJson(value, type)
    }
}