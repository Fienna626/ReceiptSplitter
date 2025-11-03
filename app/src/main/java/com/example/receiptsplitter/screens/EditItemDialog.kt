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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.receiptsplitter.data.Person
import com.example.receiptsplitter.data.ReceiptItem
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.Color

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
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                // Ensure the outermost column for content uses scroll to handle overall dialog size
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    if (item.name.isBlank()) "Add New Item" else "Edit Item",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Item Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = editPrice,
                    onValueChange = { editPrice = it },
                    label = { Text("Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    "Assign to:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(8.dp))

                // --- MODIFIED: Checklist with dedicated scrolling ---
                // This used to be select person -> select item
                Column(
                    modifier = Modifier
                        .heightIn(max = 180.dp) // Slightly increased max height
                        .verticalScroll(rememberScrollState()) // Applied scroll here
                ) {
                    allPeople.forEach { person ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSet = selectedPeople.toMutableSet()
                                    if (newSet.contains(person)) newSet.remove(person)
                                    else newSet.add(person)
                                    selectedPeople = newSet
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedPeople.contains(person),
                                onCheckedChange = null
                            )
                            Text(
                                person.name,
                                modifier = Modifier.padding(start = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                // --- End Checklist ---

                Spacer(Modifier.height(24.dp))

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
                            onSave(updatedItem)
                        },
                        enabled = editName.text.isNotBlank() && editPrice.text.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
