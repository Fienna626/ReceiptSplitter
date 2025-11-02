@file:OptIn(ExperimentalLayoutApi::class) // For FlowRow

package com.example.receiptsplitter.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.receiptsplitter.data.Person
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    // --- State from ViewModel ---
    people: List<Person>, // <-- Receives the list
    previewImageUri: Uri?,

    // --- Callbacks to ViewModel ---
    onNavigateBack: () -> Unit,
    onScanReceiptClick: () -> Unit,
    onProceedToSplit: (Uri?) -> Unit, // <-- Does NOT pass people
    onAddPerson: (String) -> Unit,
    onEditPerson: (Person, String) -> Unit,
    onDeletePerson: (Person) -> Unit
) {

    // --- Local UI State for Dialogs ONLY ---
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var newPersonName by remember { mutableStateOf(TextFieldValue("")) }
    var personToEdit by remember { mutableStateOf<Person?>(null) }
    var editPersonName by remember { mutableStateOf(TextFieldValue("")) }
    val addFocusRequester = remember { FocusRequester() }
    val editFocusRequester = remember { FocusRequester() }

    // --- NO 'peopleState' variable here ---

    // --- "Add Person" Dialog ---
    if (showAddPersonDialog) {
        AlertDialog(
            onDismissRequest = { showAddPersonDialog = false },
            title = { Text("Add New Person") },
            text = {
                OutlinedTextField(
                    value = newPersonName,
                    onValueChange = { newPersonName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    isError = newPersonName.text.isBlank(),
                    modifier = Modifier.focusRequester(addFocusRequester)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPersonName.text.isNotBlank()) {
                            onAddPerson(newPersonName.text) // Call ViewModel
                            showAddPersonDialog = false
                            newPersonName = TextFieldValue("")
                        }
                    },
                    enabled = newPersonName.text.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddPersonDialog = false }) { Text("Cancel") } }
        )
        LaunchedEffect(Unit) {
            delay(100)
            addFocusRequester.requestFocus()
        }
    }

    // --- "Edit Person" Dialog ---
    personToEdit?.let { person ->
        AlertDialog(
            onDismissRequest = { personToEdit = null },
            title = { Text("Edit Name") },
            text = {
                OutlinedTextField(
                    value = editPersonName,
                    onValueChange = { editPersonName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    isError = editPersonName.text.isBlank(),
                    modifier = Modifier.focusRequester(editFocusRequester)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editPersonName.text.isNotBlank()) {
                            onEditPerson(person, editPersonName.text) // Call ViewModel
                            personToEdit = null
                        }
                    },
                    enabled = editPersonName.text.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { personToEdit = null }) { Text("Cancel") } }
        )
        LaunchedEffect(personToEdit) {
            delay(100)
            editFocusRequester.requestFocus()
        }
    }

    // --- Main Screen UI ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup New Bill") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Who is splitting this bill?", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            // --- Scrollable Middle Section ---
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- People "Bubbles" ---
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // --- Use the 'people' parameter from ViewModel ---
                    people.forEach { person ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                val text = person.name
                                editPersonName = TextFieldValue(text = text, selection = TextRange(0, text.length))
                                personToEdit = person
                            },
                            label = { Text(person.name) },
                            leadingIcon = { Icon(Icons.Filled.Person, "Person") },
                            trailingIcon = {
                                if (people.size > 1) { // Use 'people' parameter
                                    IconButton(onClick = { onDeletePerson(person) }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Clear, "Remove", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        )
                    }
                    // "Add Person" Button
                    IconButton(onClick = {
                        val defaultName = "Person ${people.size + 1}" // Use 'people' parameter
                        newPersonName = TextFieldValue(text = defaultName, selection = TextRange(0, defaultName.length))
                        showAddPersonDialog = true
                    }) {
                        Icon(Icons.Default.AddCircle, "Add Person")
                    }
                }

                HorizontalDivider()

                // --- Scan Section (This is the button for the camera) ---
                Button(onClick = onScanReceiptClick) {
                    Icon(Icons.Default.PhotoCamera, null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Scan/Upload Receipt")
                }
                Text("Tip: Make sure the picture is straight and well-lit!", style = MaterialTheme.typography.labelSmall)

                // --- Image Preview ---
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp).border(1.dp, MaterialTheme.colorScheme.outline).padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewImageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(previewImageUri),
                            contentDescription = "Receipt Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("Scan a receipt to see a preview here", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } // --- End Scrollable Column ---

            Spacer(Modifier.height(16.dp))

            // --- Proceed Button ---
            Button(
                onClick = { onProceedToSplit(previewImageUri) }, // <-- CORRECT: Only passes URI
                enabled = previewImageUri != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue to Items")
            }
        } // End Main Column
    } // End Scaffold
}