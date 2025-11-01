package com.example.receiptsplitter.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.receiptsplitter.MainActivity
import com.example.receiptsplitter.data.Person
import com.example.receiptsplitter.data.PersonTotal
import com.example.receiptsplitter.data.ReceiptItem
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSplitterScreen(
    // --- CORRECT, SIMPLE PARAMETER LIST ---
    items: List<ReceiptItem>,
    people: List<Person>, // <-- IT MUST RECEIVE THE PEOPLE LIST
    onUpdateItem: (ReceiptItem) -> Unit,
    onDeleteItem: (ReceiptItem) -> Unit,
    onGoToTip: (List<PersonTotal>) -> Unit,
    onNavigateBack: () -> Unit
) {
    // --- Local UI State ---
    val (editingItem, setEditingItem) = remember { mutableStateOf<ReceiptItem?>(null) }
    var taxInput by remember { mutableStateOf(TextFieldValue("")) }

    // --- NO VIEWMODEL IS CREATED HERE ---
    val context = LocalContext.current // Only need context for the calculation

    // --- Calculations ---
    val calculatedTotals = remember(items, people, taxInput.text) { // Uses the 'people' parameter
        (context as? MainActivity)?.calculateTotalsBeforeTip(
            people, // Uses the 'people' parameter
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
                    ItemRow(
                        item = item,
                        onClick = { setEditingItem(item) }, // Open dialog
                        onDeleteClick = { onDeleteItem(item) }
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

            Button(
                onClick = { onGoToTip(calculatedTotals) },
                enabled = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
            ) {
                Text("Go to Tip")
            }
        } // End Main Column

        // --- Dialogs ---
        editingItem?.let { item ->
            EditItemDialog(
                item = item,
                allPeople = people, // <-- PASSES THE 'people' PARAMETER
                onDismiss = { setEditingItem(null) },
                onSave = { updatedItem ->
                    onUpdateItem(updatedItem)
                    setEditingItem(null)
                }
            )
        }
    } // End Scaffold
}