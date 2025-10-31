package com.example.receiptsplitter.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.receiptsplitter.data.Person // Import Person data class
import java.util.UUID
// In screens/PeopleList.kt (or wherever it's defined)

@Composable
fun PeopleList(
    people: List<Person>,
    selectedPersonId: UUID?,
    onAddPerson: () -> Unit,
    onEditPersonName: (person: Person, newName: String) -> Unit,
    onDeletePerson: (person: Person) -> Unit,
    onSelectPerson: (Person) -> Unit,
    canDelete: Boolean = true
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(people) { person ->
            val isSelected = person.id == selectedPersonId
            var personName by remember(person.id, person.name) {
                mutableStateOf(TextFieldValue(person.name))
            }

            // Use a Card for better visual feedback
            Card(
                onClick = { onSelectPerson(person) },
                modifier = Modifier.width(120.dp),
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                OutlinedTextField(
                    value = personName,
                    onValueChange = {
                        personName = it
                        onEditPersonName(person, it.text)
                    },
                    label = { Text("Name", style = MaterialTheme.typography.labelSmall) }, // Smaller label
                    modifier = Modifier.padding(8.dp), // Padding inside the card
                    // Use a smaller text style
                    textStyle = MaterialTheme.typography.bodyMedium,
                    // Add delete button back
                    trailingIcon = {
                        if (canDelete && people.size > 1) {
                            IconButton(onClick = { onDeletePerson(person) }, modifier = Modifier.size(20.dp)) { // Smaller icon button
                                Icon(Icons.Default.Clear, "Remove Person", modifier = Modifier.size(16.dp)) // Smaller icon
                            }
                        }
                    },
                    singleLine = true // Keep it on one line
                )
                // ---
            }
        }
        item {
            IconButton(onClick = onAddPerson) {
                Icon(Icons.Default.Add, "Add Person")
            }
        }
    }
}