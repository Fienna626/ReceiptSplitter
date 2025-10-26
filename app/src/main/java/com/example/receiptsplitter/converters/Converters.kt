package com.example.receiptsplitter.data.converters

import androidx.room.TypeConverter
import com.example.receiptsplitter.data.PersonTotal
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromPersonTotalList(value: List<PersonTotal>?): String? {
        if (value == null) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<List<PersonTotal>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toPersonTotalList(value: String?): List<PersonTotal>? {
        if (value == null) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<List<PersonTotal>>() {}.type
        return gson.fromJson(value, type)
    }
}