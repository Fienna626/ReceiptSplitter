package com.example.receiptsplitter.viewmodel

import android.net.Uri
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

class ReceiptViewModel(private val receiptDao: ReceiptDao) : ViewModel() {

    // --- STATE ---
    private val _receiptItems = MutableStateFlow<List<ReceiptItem>>(emptyList())
    val receiptItems: StateFlow<List<ReceiptItem>> = _receiptItems.asStateFlow()

    private val _currentPeople = MutableStateFlow<List<Person>>(emptyList())
    val currentPeople: StateFlow<List<Person>> = _currentPeople.asStateFlow()

    private val _previewImageUri = MutableStateFlow<Uri?>(null)
    val previewImageUri: StateFlow<Uri?> = _previewImageUri.asStateFlow()

    private val _currentTotalsBeforeTip = MutableStateFlow<List<PersonTotal>>(emptyList())
    val currentTotalsBeforeTip: StateFlow<List<PersonTotal>> = _currentTotalsBeforeTip.asStateFlow()

    private val _finalTotals = MutableStateFlow<List<PersonTotal>>(emptyList())
    val finalTotals: StateFlow<List<PersonTotal>> = _finalTotals.asStateFlow()

    val savedReceipts: StateFlow<List<SavedReceiptSummary>> = receiptDao.getAllReceipts()
        .map { entityList -> entityList.map { it.toSummary() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _totalTax = MutableStateFlow<String?>(null)
    val totalTax: StateFlow<String?> = _totalTax.asStateFlow()

    fun setTotalTax(tax: String) {
        _totalTax.value = tax
    }


    // --- LOGIC ---

    fun setPreviewImageUri(uri: Uri?) {
        _previewImageUri.value = uri
    }

    fun setCurrentItems(items: List<ReceiptItem>) {
        _receiptItems.value = items
    }

    fun setTotalsBeforeTip(totals: List<PersonTotal>) {
        _currentTotalsBeforeTip.value = totals
    }

    fun setFinalTotals(totals: List<PersonTotal>) {
        _finalTotals.value = totals
    }

    // --- Person Management ---
    fun addPerson(name: String) {
        _currentPeople.value = _currentPeople.value + Person(name = name)
    }

    fun editPersonName(personToEdit: Person, newName: String) {
        _currentPeople.value = _currentPeople.value.map {
            if (it.id == personToEdit.id) it.copy(name = newName) else it
        }
    }

    fun deletePerson(personToDelete: Person) {
        if (_currentPeople.value.size <= 1) return // Keep at least one person

        _currentPeople.value = _currentPeople.value.filter { it.id != personToDelete.id }

        // Also remove this person from any item assignments
        _receiptItems.value = _receiptItems.value.map { item ->
            if (item.assignedPeople.any { it.id == personToDelete.id }) {
                item.copy(assignedPeople = item.assignedPeople.filter { it.id != personToDelete.id }.toMutableList())
            } else {
                item
            }
        }
    }

    // --- Item Management ---
    fun updateReceiptItem(updatedItem: ReceiptItem) {
        val currentList = _receiptItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedItem.id }
        if (index != -1) {
            // It's an existing item, replace it
            currentList[index] = updatedItem
        } else {
            // It's a new item (ID doesn't exist), add it
            currentList.add(updatedItem)
        }
        _receiptItems.value = currentList
    }

    fun deleteReceiptItem(itemToDelete: ReceiptItem) {
        _receiptItems.value = _receiptItems.value - itemToDelete
    }

    // --- Receipt Lifecycle ---
    fun clearCurrentItems() {
        _receiptItems.value = emptyList()
        _currentPeople.value = listOf(Person(name = "Person 1")) // Default person
        _previewImageUri.value = null
        _currentTotalsBeforeTip.value = emptyList()
        _finalTotals.value = emptyList()
        _totalTax.value = null
    }

    fun saveCurrentReceipt(finalTotals: List<PersonTotal>, description: String) {
        if (finalTotals.isEmpty()) return
        val entityToSave = SavedReceiptEntity(
            id = UUID.randomUUID().toString(),
            description = description,
            timestamp = System.currentTimeMillis(),
            grandTotal = finalTotals.sumOf { it.totalOwed },
            personTotals = finalTotals,
            items = _receiptItems.value
        )
        viewModelScope.launch {
            receiptDao.insertReceipt(entityToSave)
        }
    }

    fun setPeopleForCurrentSplit(people: List<Person>) {
        _currentPeople.value = people
    }

    fun deleteSavedReceipt(receiptSummaryToDelete: SavedReceiptSummary) {
        viewModelScope.launch {
            receiptDao.deleteReceipt(receiptSummaryToDelete.toEntity())
        }
    }
    fun updateReceiptName(receiptId: String, newName: String) {
        viewModelScope.launch {
            receiptDao.updateDescription(receiptId, newName)
        }
    }
}