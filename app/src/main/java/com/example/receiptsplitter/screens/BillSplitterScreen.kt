package com.example.receiptsplitter.screens

// ... (Keep all your existing imports: Column, Row, LazyColumn, Scaffold, TopAppBar, etc.) ...
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.receiptsplitter.MainActivity
import com.example.receiptsplitter.data.Person
import com.example.receiptsplitter.data.ReceiptItem
import com.example.receiptsplitter.data.PersonTotal
import androidx.compose.ui.text.input.KeyboardType
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSplitterScreen(
    items: List<ReceiptItem>,
    people: List<Person>,
    selectedPersonId: UUID?,
    onUpdateItem: (ReceiptItem) -> Unit,
    onDeleteItem: (ReceiptItem) -> Unit,
    onGoToTip: (List<PersonTotal>) -> Unit,
    onNavigateBack: () -> Unit,
    onAddPerson: () -> Unit,
    onEditPersonName: (Person, String) -> Unit,
    onDeletePerson: (Person) -> Unit,
    onSelectPerson: (Person) -> Unit,
    onToggleItem: (ReceiptItem) -> Unit
) {
    // --- State variables ---
    val (editingItem, setEditingItem) = remember { mutableStateOf<ReceiptItem?>(null) }
    var taxInput by remember { mutableStateOf(TextFieldValue("")) } // Tax state
    val context = LocalContext.current
    val (viewingPerson, setViewingPerson) = remember { mutableStateOf<PersonTotal?>(null) }


    // --- Calculations ---
    val calculatedTotals = remember(items, people, taxInput.text) { // Re-calculates when tax changes
        (context as? MainActivity)?.calculateTotalsBeforeTip(
            people,
            items,
            taxInput.text
        ) ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assign Items") }, // Updated title
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

            // --- 1. PEOPLE LIST (Fixed at Top) ---
            Text(
                "Assign Items To:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
            )
            PeopleList( // The horizontal list of people
                people = people,
                selectedPersonId = selectedPersonId,
                onAddPerson = onAddPerson,
                onEditPersonName = onEditPersonName,
                onDeletePerson = onDeletePerson,
                onSelectPerson = onSelectPerson
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // --- 2. ITEM LIST (Scrollable, takes up all remaining space) ---
            Text(
                "Items",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // This makes the list fill the available space
                    .padding(horizontal = 16.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    ItemRow( // Your existing composable for an item row
                        item = item,
                        onClick = {
                            if (selectedPersonId != null) {
                                onToggleItem(item) // Assign/unassign
                            } else {
                                // setEditingItem(item) // Keep if you still want to edit name/price
                            }
                        },
                        onDeleteClick = { onDeleteItem(item) },
                        // --- Pass selected ID to highlight ---
                        selectedPersonId = selectedPersonId
                    )
                }
            } // End LazyColumn

            // --- 3. CHECKOUT SECTION (Fixed at Bottom) ---
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            // --- Tax Field (Moved to Bottom) ---
            OutlinedTextField(
                value = taxInput,
                onValueChange = { taxInput = it },
                label = { Text("Total Tax") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // --- Totals Display (Shows Subtotal + Tax) ---
            if (calculatedTotals.isNotEmpty()) {
                TotalsDisplay(
                    totals = calculatedTotals,
                    onPersonClick = { personTotal -> setViewingPerson(personTotal) }
                )
            }

            // --- Proceed to Tip Button ---
            Button(
                onClick = { onGoToTip(calculatedTotals) },
                // Only enable if items exist and have been assigned
                enabled = calculatedTotals.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
            ) {
                Text("Go to Tip")
            }
        } // End Main Column

        // --- Dialogs (Overlay) ---
        editingItem?.let { item ->
            EditItemDialog(
                item = item,
                allPeople = people, // Pass people list to dialog
                onDismiss = { setEditingItem(null) },
                onSave = { updatedName: String, updatedPrice: Double, assignedPeople: List<Person> ->
                    val updatedItem = item.copy(
                        name = updatedName,
                        price = updatedPrice,
                        assignedPeople = assignedPeople.toMutableList()
                    )
                    onUpdateItem(updatedItem)
                    setEditingItem(null)
                }
            )
        }
        viewingPerson?.let { personTotal ->
            BreakdownDialog(
                personTotal = personTotal,
                allItems = items,
                onDismiss = { setViewingPerson(null) }
            )
        }
    } // End Scaffold
}

// Keep your PeopleList composable here or in its own file

@Composable

fun PeopleList(

    people: List<Person>,
    onAddPerson: () -> Unit,
    onEditPersonName: (Person, String) -> Unit,
    onDeletePerson: (Person) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically

    ) { items(people) { person ->
            var personName by remember(person.id, person.name) { // <-- FIX: Key by person's data
                mutableStateOf(TextFieldValue(person.name))
            }

            OutlinedTextField(
                value = personName,
                onValueChange = {
                    personName = it
                    onEditPersonName(person, it.text)
                },

                label = { Text("Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                trailingIcon = {

                    if (people.size > 1) { // <-- FIX: Only show if more than 1 person
                        IconButton(onClick = { onDeletePerson(person) }) {
                            Icon(Icons.Default.Clear, contentDescription = "Remove Person")
                        }
                    }
                },
                modifier = Modifier.width(170.dp)
            )
        }

        item {
            IconButton(onClick = onAddPerson) {
                Icon(Icons.Default.Add, contentDescription = "Add Person")
            }
        }
    }
}