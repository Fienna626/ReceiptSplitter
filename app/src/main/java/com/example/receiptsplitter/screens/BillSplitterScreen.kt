package com.example.receiptsplitter.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.receiptsplitter.MainActivity
import com.example.receiptsplitter.data.Person
import com.example.receiptsplitter.data.ReceiptItem
import com.example.receiptsplitter.data.PersonTotal
import com.example.receiptsplitter.screens.ItemRow
import com.example.receiptsplitter.screens.EditItemDialog
import com.example.receiptsplitter.screens.TotalsDisplay
import com.example.receiptsplitter.screens.BreakdownDialog
import androidx.compose.ui.text.input.KeyboardType
import com.example.receiptsplitter.PeopleList

@Composable
fun BillSplitterScreen(
    items: List<ReceiptItem>,
    onScanReceiptClick: () -> Unit,
    onUpdateItem: (ReceiptItem) -> Unit, // <-- to save edits
    onDeleteItem: (ReceiptItem) -> Unit // <-- to delete item

) {
    // --- State for People ---
    val people = remember { mutableStateOf(listOf(Person(name = "Person 1"))) }
    val (editingItem, setEditingItem) = remember { mutableStateOf<ReceiptItem?>(null) }
    // --- State for Tax and Tip ---
    var taxInput by remember { mutableStateOf(TextFieldValue("")) }

    // --- NEW: Change Tip to a percentage Float ---
    // Start with a default tip of 18%
    var tipPercent by remember { mutableStateOf(15f) }

    // --- Get the activity to call the math function ---
    val context = LocalContext.current

    // --- State for the breakdown dialog ---
    val (viewingPerson, setViewingPerson) = remember { mutableStateOf<PersonTotal?>(null) }

    // --- Calculate the total subtotal (for the tip) ---
    val totalSubtotal = items.sumOf { it.price }

    // --- Calculate the tip dollar amount ---
    val calculatedTipAmount = totalSubtotal * (tipPercent / 100.0)

    // --- UPDATED: Update the remember block ---
    val calculatedTotals = remember(items, people.value, taxInput.text, tipPercent) { // <-- Use tipPercent
        (context as? MainActivity)?.calculateTotals(
            people.value,
            items,
            taxInput.text,
            calculatedTipAmount.toString() // <-- Pass the calculated dollar amount
        ) ?: emptyList()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. THE TOP BUTTON ---
        Button(
            onClick = onScanReceiptClick,
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Scan a New Receipt")
        }

        // --- 2. THE PEOPLE LIST ---
        Text("People", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        PeopleList(
            people = people.value,
            onAddPerson = {
                val newName = "Person ${people.value.size + 1}"
                people.value = people.value + Person(name = newName)
            },
            onEditPersonName = { person, newName ->
                val updatedList = people.value.map {
                    if (it.id == person.id) it.copy(name = newName) else it
                }
                people.value = updatedList
            },
            onDeletePerson = { personToDelete ->
                // 1. Remove the person from the main people list
                people.value = people.value.filter { it.id != personToDelete.id }

                // 2. Remove the person from any items they were assigned to
                items.forEach { item ->
                    if (item.assignedPeople.contains(personToDelete)) {
                        val updatedItem = item.copy(
                            assignedPeople = item.assignedPeople.filter { it.id != personToDelete.id }.toMutableList()
                        )
                        // Use the onUpdateItem function we already have!
                        onUpdateItem(updatedItem)
                    }
                }
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        // --- Tax and Tip Input Fields ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = taxInput,
                onValueChange = { taxInput = it },
                label = { Text("Total Tax") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${tipPercent.toInt()}%", // Show the current percent
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        Slider(
            value = tipPercent,
            onValueChange = { tipPercent = it },
            valueRange = 0f..30f, // From 0% to 30%
            steps = 29, // This makes it snap to whole numbers (0, 1, 2...)
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // --- 3. THE ITEM LIST ---
        Text("Items", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        // --- FIX: Use a non-scrolling Column ---
        LazyColumn(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            // --- FIX: ADD WEIGHT ---
            .weight(1f) // <-- Add this crucial line
        ) {
            // --- FIX: Use items builder ---
            items(items, key = { it.id }) { item ->
                ItemRow(
                    item = item,
                    onClick = { setEditingItem(item) },
                    onDeleteClick = { onDeleteItem(item) }
                )
            }
        }

        // --- 4. THE EDIT DIALOG ---
        editingItem?.let { item ->
            EditItemDialog(
                item = item,
                allPeople = people.value,
                onDismiss = { setEditingItem(null) },
                // --- FIX: Receive all three parameters ---
                onSave = { updatedName: String, updatedPrice: Double, assignedPeople: List<Person> ->
                    // --- Save the changes ---
                    val updatedItem = item.copy(
                        name = updatedName,
                        price = updatedPrice,
                        // --- This line will now work! ---
                        assignedPeople = assignedPeople.toMutableList()
                    )
                    // Call the function to update the list
                    onUpdateItem(updatedItem)
                    setEditingItem(null) // Close the dialog
                }
            )
        }
        // --- NEW! 5. THE TOTALS DISPLAY ---
        // Show the totals card if there are any totals to show
        if (calculatedTotals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            TotalsDisplay(
                totals = calculatedTotals,
                onPersonClick = { personTotal -> // <-- FIX: Add this line
                    setViewingPerson(personTotal) // Tell the screen to open the dialog
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    // --- NEW: 6. THE BREAKDOWN DIALOG ---
    viewingPerson?.let { personTotal ->
        BreakdownDialog(
            personTotal = personTotal,
            allItems = items,
            onDismiss = { setViewingPerson(null) }
        )
    }
}
