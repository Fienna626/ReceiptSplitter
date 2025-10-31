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
import com.example.receiptsplitter.data.Person

class ReceiptViewModel(private val receiptDao: ReceiptDao) : ViewModel() {

    // --- State ---
    // Holds the items for the *current* bill being split
    private val _receiptItems = MutableStateFlow<List<ReceiptItem>>(emptyList())
    val receiptItems: StateFlow<List<ReceiptItem>> = _receiptItems.asStateFlow()

    private val _currentPeople = MutableStateFlow<List<Person>>(emptyList())
    val currentPeople: StateFlow<List<Person>> = _currentPeople.asStateFlow()

    private val _selectedPersonId = MutableStateFlow<UUID?>(null)
    val selectedPersonId: StateFlow<UUID?> = _selectedPersonId.asStateFlow()

    private val _currentTotalsBeforeTip = MutableStateFlow<List<PersonTotal>>(emptyList())
    val currentTotalsBeforeTip: StateFlow<List<PersonTotal>> = _currentTotalsBeforeTip.asStateFlow()

    // --- State for final totals (with tip) ---
    private val _finalTotals = MutableStateFlow<List<PersonTotal>>(emptyList())
    val finalTotals: StateFlow<List<PersonTotal>> = _finalTotals.asStateFlow()

    // Holds the list of saved receipts for the HomeScreen (mapped to UI model)
    val savedReceipts: StateFlow<List<SavedReceiptSummary>> = receiptDao.getAllReceipts()
        .map { entityList -> entityList.map { it.toSummary() } }
        .stateIn(
            scope = viewModelScope, // Use viewModelScope
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // --- Logic ---
    fun setPeopleForCurrentSplit(people: List<Person>) {
        _currentPeople.value = people
    }
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


    // --- Function to set the final totals ---
    fun setFinalTotals(totals: List<PersonTotal>) {
        _finalTotals.value = totals
    }

    // Clear current items (e.g., before navigating to splitter)
    fun clearCurrentItems() {
        _receiptItems.value = emptyList()
        _currentPeople.value = emptyList()
        _currentTotalsBeforeTip.value = emptyList()
        _finalTotals.value = emptyList()
    }

    fun addPerson() {
        val currentList = _currentPeople.value
        val newName = "Person ${currentList.size + 1}"
        _currentPeople.value = currentList + Person(name = newName)
    }

    fun editPersonName(personToEdit: Person, newName: String) {
        val currentList = _currentPeople.value
        _currentPeople.value = currentList.map {
            if (it.id == personToEdit.id) it.copy(name = newName) else it
        }
    }

    fun deletePerson(personToDelete: Person) {
        val currentList = _currentPeople.value
        if (currentList.size <= 1) return // Don't delete the last person

        _currentPeople.value = currentList.filter { it.id != personToDelete.id }

        // Also remove deleted person from item assignments
        val currentItems = _receiptItems.value
        _receiptItems.value = currentItems.map { item ->
            if (item.assignedPeople.contains(personToDelete)) {
                item.copy(assignedPeople = item.assignedPeople.filter { it.id != personToDelete.id }.toMutableList())
            } else {
                item
            }
        }


    }

    // --- Function to handle person selection ---
    fun selectPerson(person: Person) {
        if (_selectedPersonId.value == person.id) {
            _selectedPersonId.value = null // Deselect if tapping the same person
        } else {
            _selectedPersonId.value = person.id // Select this person
        }
    }

    // --- Function to assign/unassign an item ---
    fun toggleItemForSelectedPerson(itemToToggle: ReceiptItem) {
        val selectedId = _selectedPersonId.value ?: return // Do nothing if no person is selected

        val currentList = _receiptItems.value
        _receiptItems.value = currentList.map { item ->
            if (item.id == itemToToggle.id) {
                // Find the person object from the ID
                val selectedPerson = _currentPeople.value.find { it.id == selectedId } ?: return

                val newAssignedList = item.assignedPeople.toMutableList()
                if (newAssignedList.any { it.id == selectedId }) {
                    // Person is assigned, remove them
                    newAssignedList.removeAll { it.id == selectedId }
                } else {
                    // Person is not assigned, add them
                    newAssignedList.add(selectedPerson)
                }
                item.copy(assignedPeople = newAssignedList)
            } else {
                item // Return unmodified item
            }
        }
    }

    // --- Function to set pre-tip totals ---
    fun setTotalsBeforeTip(totals: List<PersonTotal>) {
        _currentTotalsBeforeTip.value = totals
    }



}