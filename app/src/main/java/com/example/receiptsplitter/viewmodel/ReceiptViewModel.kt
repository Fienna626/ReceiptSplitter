package com.example.receiptsplitter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.receiptsplitter.data.*
import com.example.receiptsplitter.data.SavedReceiptSummary
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import com.example.receiptsplitter.data.toSummary

class ReceiptViewModel(private val receiptDao: ReceiptDao) : ViewModel() {

    // --- State ---
    // Holds the items for the *current* bill being split
    private val _receiptItems = MutableStateFlow<List<ReceiptItem>>(emptyList())
    val receiptItems: StateFlow<List<ReceiptItem>> = _receiptItems.asStateFlow()

    // Holds the list of saved receipts for the HomeScreen (mapped to UI model)
    val savedReceipts: StateFlow<List<SavedReceiptSummary>> = receiptDao.getAllReceipts()
        .map { entityList -> entityList.map { it.toSummary() } }
        .stateIn(
            scope = viewModelScope, // Use viewModelScope
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // --- Logic ---

    // This replaces the updateReceiptItem in MainActivity
    fun updateReceiptItem(updatedItem: ReceiptItem) {
        val currentList = _receiptItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedItem.id }
        if (index != -1) {
            currentList[index] = updatedItem
            _receiptItems.value = currentList
        }
    }

    // This replaces the deleteReceiptItem in MainActivity
    fun deleteReceiptItem(itemToDelete: ReceiptItem) {
        _receiptItems.value = _receiptItems.value - itemToDelete
    }

    // Logic to save the current split bill
    fun saveCurrentReceipt(finalTotals: List<PersonTotal>) {
        if (finalTotals.isNotEmpty()) {
            val entityToSave = SavedReceiptEntity(
                id = UUID.randomUUID().toString(),
                description = "Receipt from ${SimpleDateFormat("MMM dd", Locale.US).format(Date())}",
                timestamp = System.currentTimeMillis(),
                grandTotal = finalTotals.sumOf { it.totalOwed },
                personTotals = finalTotals
            )
            viewModelScope.launch { // Use viewModelScope for coroutines
                receiptDao.insertReceipt(entityToSave)
            }
        }
    }

    // Logic to delete a saved receipt (called from HomeScreen)
    fun deleteSavedReceipt(receiptSummaryToDelete: SavedReceiptSummary) {
        viewModelScope.launch {
            receiptDao.deleteReceipt(receiptSummaryToDelete.toEntity())
        }
    }

    // Set the current items (e.g., after parsing)
    fun setCurrentItems(items: List<ReceiptItem>) {
        _receiptItems.value = items
    }

    // Clear current items (e.g., before navigating to splitter)
    fun clearCurrentItems() {
        _receiptItems.value = emptyList()
    }

}