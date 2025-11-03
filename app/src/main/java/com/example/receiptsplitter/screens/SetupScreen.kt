@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.example.receiptsplitter.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.receiptsplitter.data.Person
import com.example.receiptsplitter.ui.theme.DarkPastelBrown
import com.example.receiptsplitter.ui.theme.OffWhiteSurface
import com.example.receiptsplitter.ui.theme.PastelBrown
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.shadow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    // --- State from ViewModel ---
    people: List<Person>,
    previewImageUri: Uri?,

    // --- Callbacks to ViewModel ---
    onNavigateBack: () -> Unit,
    onScanReceiptClick: () -> Unit,
    onProceedToSplit: (Uri?) -> Unit,
    onAddPerson: (String) -> Unit,
    onEditPerson: (Person, String) -> Unit,
    onDeletePerson: (Person) -> Unit
) {

    // --- Local UI State for Dialogs ONLY (No Changes) ---
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var newPersonName by remember { mutableStateOf(TextFieldValue("")) }
    var personToEdit by remember { mutableStateOf<Person?>(null) }
    var editPersonName by remember { mutableStateOf(TextFieldValue("")) }
    val addFocusRequester = remember { FocusRequester() }
    val editFocusRequester = remember { FocusRequester() }

    // --- "Add Person" Dialog  ---
    if (showAddPersonDialog) {
        AlertDialog(
            onDismissRequest = { showAddPersonDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            onAddPerson(newPersonName.text)
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
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            onEditPerson(person, editPersonName.text)
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "New Bill",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Replaced header with a hint text ---
            Text(
                "Add people to split. Tap a name to edit.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            // --- Scrollable Middle Section ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- People "Bubbles" ---
                FlowRow( modifier = Modifier .fillMaxWidth()
                    .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    people.forEach { person ->
                        FilterChip(

                            selected = false,
                            onClick = {
                                val text = person.name
                                editPersonName = TextFieldValue(text = text, selection = TextRange(0, text.length))
                                personToEdit = person
                            },
                            label = {
                                Text(
                                    person.name,
                                    color = OffWhiteSurface
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = PastelBrown, // WHY MaterialTheme.colorScheme.surface NO WORK HERE????
                            ),
                            border = BorderStroke(0.dp, Color.Transparent),

                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Person, "Person",
                                    tint = OffWhiteSurface
                                )
                            },
                            trailingIcon = {
                                if (people.size > 1) {
                                    IconButton(onClick = { onDeletePerson(person) }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Clear, "Remove", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        )
                    }
                    IconButton(onClick = {
                        val defaultName = "Person ${people.size + 1}"
                        newPersonName = TextFieldValue(text = defaultName, selection = TextRange(0, defaultName.length))
                        showAddPersonDialog = true
                    }, ) {
                        Icon(Icons.Default.AddCircle, "Add Person")
                    }
                }

                HorizontalDivider()

                // --- Scan Section ---
                Button(onClick = onScanReceiptClick,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp))
                {
                    Icon(Icons.Default.PhotoCamera, null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Scan/Upload Receipt")
                }
                Text("Tip: Make sure the picture is straight and well-lit!", style = MaterialTheme.typography.labelSmall)

                // --- Image Preview ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.secondary,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(4.dp),
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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Scan a receipt to see a preview here",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } // --- End Scrollable Column ---

            Spacer(Modifier.height(16.dp))

            // --- Proceed Button (No Changes) ---
            Button(
                onClick = { onProceedToSplit(previewImageUri) },
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                enabled = previewImageUri != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text("Continue to Items")
            }
        } // End Main Column
    } // End Scaffold
}