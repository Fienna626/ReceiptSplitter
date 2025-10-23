package com.example.receiptsplitter.data
import com.example.receiptsplitter.data.Person

data class ReceiptItem(
    val id: java.util.UUID = java.util.UUID.randomUUID(),
    var name: String,
    var price: Double,
    // A list of people who are splitting this item
    val assignedPeople: MutableList<Person> = mutableListOf()
)

