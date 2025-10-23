package com.example.receiptsplitter.data


data class PersonTotal(
    val person: Person,
    val subtotal: Double,
    val taxShare: Double,
    val tipShare: Double,
    val totalOwed: Double
)