package com.example.receiptsplitter.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.receiptsplitter.MainActivity
import com.example.receiptsplitter.data.AppDatabase
import com.example.receiptsplitter.data.Person
import com.example.receiptsplitter.data.PersonTotal
import com.example.receiptsplitter.data.ReceiptItem
import com.example.receiptsplitter.viewmodel.ReceiptViewModel
import com.example.receiptsplitter.viewmodel.ReceiptViewModelFactory
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSplitterScreen(
    // Simple parameter list
    items: List<ReceiptItem>,
    onUpdateItem: (ReceiptItem) -> Unit,
    onDeleteItem: (ReceiptItem) -> Unit,
    onGoToTip: (List<PersonTotal>) -> Unit,
    onNavigateBack: () -> Unit
) {
    // --- Local UI State ---
    val (editingItem, setEditingItem) = remember { mutableStateOf<ReceiptItem?>(null) }
    var taxInput by remember { mutableStateOf(TextFieldValue("")) }

    // --- Get ViewModel to access people list *only for the dialog* ---
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context.applicationContext)
    val receiptDao = database.receiptDao()
    val factory = ReceiptViewModelFactory(receiptDao)
    val viewModel: ReceiptViewModel = viewModel(factory = factory)
    // Get the people list from the ViewModel (set by SetupScreen)
    val people by viewModel.currentPeople.collectAsState()
    // ---

    // --- Calculations ---
    val calculatedTotals = remember(items, people, taxInput.text) {
        (context as? MainActivity)?.calculateTotalsBeforeTip(
            people,
            items,
            taxInput.text
        ) ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assign Items") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), // Apply Scaffold padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- People list UI is GONE from here ---

            // --- ITEM LIST (Scrollable) ---
            Text(
                "Tap an item to edit/assign, or add a new one",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Fills the space
                    .padding(horizontal = 16.dp)
            ) {
                // --- Add Item Button ---
                item {
                    Button(
                        onClick = {
                            // Open dialog to add a new, blank item
                            setEditingItem(ReceiptItem(id = UUID.randomUUID(), name = "", price = 0.0))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, "Add", Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Add New Item Manually")
                    }
                }

                // --- Item List ---
                items(items, key = { it.id }) { item ->
                    ItemRow( // We need ItemRow.kt
                        item = item,
                        onClick = { setEditingItem(item) }, // Open dialog
                        onDeleteClick = { onDeleteItem(item) }
                        // No selectedPersonId is passed
                    )
                }
            } // End LazyColumn

            // --- CHECKOUT SECTION (Fixed at Bottom) ---
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            OutlinedTextField(
                value = taxInput,
                onValueChange = { taxInput = it },
                label = { Text("Total Tax") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // --- Totals Display is GONE from here ---

            Button(
                onClick = { onGoToTip(calculatedTotals) },
                enabled = true, // User can proceed even with 0 items
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
            ) {
                Text("Go to Tip")
            }
        } // End Main Column

        // --- Dialogs ---
        editingItem?.let { item ->
            EditItemDialog( // We need EditItemDialog.kt
                item = item,
                allPeople = people, // Pass the people list here
                onDismiss = { setEditingItem(null) },
                onSave = { updatedItem -> // Receives the full item
                    onUpdateItem(updatedItem) // Pass it up to ViewModel
                    setEditingItem(null)
                }
            )
        }
    } // End Scaffold
}