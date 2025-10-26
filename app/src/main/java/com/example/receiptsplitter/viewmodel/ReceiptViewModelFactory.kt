package com.example.receiptsplitter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.receiptsplitter.data.ReceiptDao

class ReceiptViewModelFactory(private val receiptDao: ReceiptDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReceiptViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReceiptViewModel(receiptDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}