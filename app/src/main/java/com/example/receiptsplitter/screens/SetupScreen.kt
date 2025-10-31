package com.example.receiptsplitter.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.PhotoCamera
import com.example.receiptsplitter.data.Person // Import Person data class

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onNavigateBack: () -> Unit,
    onScanReceiptClick: () -> Unit, // Callback to trigger MainActivity's dialog
    onProceedToSplit: (List<Person>) -> Unit // Callback with the list of people
) {
    // State to hold the list of people for *this setup session*
    // Start with one person by default
    val peopleState = remember { mutableStateOf(listOf(Person(name = "Person 1"))) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup New Bill") },
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
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Who is splitting this bill?", style = MaterialTheme.typography.titleLarge)

            // People List UI
            PeopleList(
                people = peopleState.value,
                onAddPerson = {
                    val newName = "Person ${peopleState.value.size + 1}"
                    peopleState.value = peopleState.value + Person(name = newName)
                },
                onEditPersonName = { person, newName ->
                    // Update the temporary list in this screen's state
                    val updatedList = peopleState.value.map {
                        if (it.id == person.id) it.copy(name = newName) else it
                    }
                    peopleState.value = updatedList
                },
                onDeletePerson = { personToDelete ->
                    // Update the temporary list, ensuring at least one person remains
                    if (peopleState.value.size > 1) {
                        peopleState.value = peopleState.value.filter { it.id != personToDelete.id }
                    }
                },
                canDelete = peopleState.value.size > 1, // Enable delete only if > 1 person
                selectedPersonId = null, // No one is selected on this screen
                onSelectPerson = { }
            )

            HorizontalDivider()

            // Scan Button and Helper Text
            Button(onClick = onScanReceiptClick) { // Trigger MainActivity's dialog
                Icon(
                    // --- CHANGE THIS LINE ---
                    imageVector = Icons.Filled.PhotoCamera, // Use Icons.Filled
                    // --- END CHANGE ---
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Scan/Upload Receipt")
            }
            Text(
                "Tip: Make sure the picture is straight and well-lit or the parser might not work correctly!",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.weight(1f)) // Pushes button to bottom

            // Proceed Button (Only enabled if people exist)
            Button(
                onClick = { onProceedToSplit(peopleState.value) }, // Pass the current list
                enabled = peopleState.value.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue to Items")
            }
        }
    }
}