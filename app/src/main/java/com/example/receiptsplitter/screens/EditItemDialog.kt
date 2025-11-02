package com.example.receiptsplitter.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.receiptsplitter.data.Person
import com.example.receiptsplitter.data.ReceiptItem

@Composable
fun EditItemDialog(
    item: ReceiptItem,
    allPeople: List<Person>,
    onDismiss: () -> Unit,
    onSave: (ReceiptItem) -> Unit // Returns the full, updated item
) {
    var editName by remember { mutableStateOf(TextFieldValue(item.name)) }
    var editPrice by remember { mutableStateOf(TextFieldValue(if (item.price > 0) item.price.toString() else "")) }
    var selectedPeople by remember { mutableStateOf(item.assignedPeople.toSet()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface // Or surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    if (item.name.isBlank()) "Add New Item" else "Edit Item",
                    style = MaterialTheme.typography.titleLarge
                )

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

                Text("Assign to:", style = MaterialTheme.typography.titleMedium)

                // --- This is the Checklist ---
                Column(
                    Modifier.verticalScroll(rememberScrollState())
                        .heightIn(max = 150.dp) // Max height for scroll
                ) {
                    allPeople.forEach { person ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                val newSet = selectedPeople.toMutableSet()
                                if (newSet.contains(person)) newSet.remove(person)
                                else newSet.add(person)
                                selectedPeople = newSet
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = selectedPeople.contains(person), onCheckedChange = null)
                            Text(person.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
                // --- End Checklist ---

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newPrice = editPrice.text.toDoubleOrNull() ?: 0.0
                            val updatedItem = item.copy(
                                name = editName.text,
                                price = newPrice,
                                assignedPeople = selectedPeople.toMutableList()
                            )
                            onSave(updatedItem) // Pass the whole object back
                        },
                        // Enable save only if name and price are not blank
                        enabled = editName.text.isNotBlank() && editPrice.text.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}