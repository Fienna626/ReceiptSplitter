package com.example.receiptsplitter.screens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.receiptsplitter.data.Person
import com.example.receiptsplitter.data.ReceiptItem




// --- UI for the "Edit Item" pop-up dialog ---
@Composable
fun EditItemDialog(
    item: ReceiptItem,
    allPeople: List<Person>,
    onDismiss: () -> Unit,
    onSave: (String, Double, List<Person>) -> Unit
) {
    var editName by remember { mutableStateOf(TextFieldValue(item.name)) }
    var editPrice by remember { mutableStateOf(TextFieldValue(item.price.toString())) }
    var selectedPeople by remember { mutableStateOf(item.assignedPeople.toSet())}

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Edit Item", style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Item Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editPrice,
                    onValueChange = { editPrice = it },
                    label = { Text("Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // --- Checklist of People ---
                Text("Assign to:", style = MaterialTheme.typography.titleMedium)
                // --- FIX: Use a regular Column with a constrained height ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp) // <-- Give it a max height (e.g., 200.dp)
                        .verticalScroll(rememberScrollState()) // Make *this* column scrollable
                ) {
                    allPeople.forEach { person -> // <-- FIX: Use a simple loop
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // ... (your existing click logic)
                                    val newSet = selectedPeople.toMutableSet()
                                    if (selectedPeople.contains(person)) {
                                        newSet.remove(person)
                                    } else {
                                        newSet.add(person)
                                    }
                                    selectedPeople = newSet
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedPeople.contains(person),
                                onCheckedChange = { isChecked ->
                                    // ... (your existing check logic)
                                    val newSet = selectedPeople.toMutableSet()
                                    if (isChecked) {
                                        newSet.add(person)
                                    } else {
                                        newSet.remove(person)
                                    }
                                    selectedPeople = newSet
                                }
                            )
                            Text(person.name)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newPrice = editPrice.text.toDoubleOrNull() ?: 0.0
                            onSave(editName.text, newPrice, selectedPeople.toList())
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

