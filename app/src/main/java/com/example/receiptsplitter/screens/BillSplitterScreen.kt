package com.example.receiptsplitter.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.receiptsplitter.MainActivity
import com.example.receiptsplitter.data.Person
import com.example.receiptsplitter.data.PersonTotal
import com.example.receiptsplitter.data.ReceiptItem
import java.util.UUID
import androidx.compose.material3.TextFieldDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSplitterScreen(
    // --- State ---
    items: List<ReceiptItem>,
    people: List<Person>,
    // --- Callbacks ---
    onUpdateItem: (ReceiptItem) -> Unit,
    onDeleteItem: (ReceiptItem) -> Unit,
    onGoToTip: (List<PersonTotal>) -> Unit,
    onNavigateBack: () -> Unit
) {
    // --- Local UI State ---
    val (editingItem, setEditingItem) = remember { mutableStateOf<ReceiptItem?>(null) }
    var taxInput by remember { mutableStateOf(TextFieldValue("")) }

    val context = LocalContext.current

    // --- Calculations ---
    val calculatedTotals = remember(items, people, taxInput.text) {
        (context as? MainActivity)?.calculateTotalsBeforeTip(
            people,
            items,
            taxInput.text
        ) ?: emptyList()
    }

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
                            "Assign Items",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- ITEM LIST (Scrollable) ---
            Text(
                "Tap an item to edit/assign, or add a new one",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Fills the space
            ) {
                // --- Add Item Button ---
                item {
                    Button(
                        onClick = {
                            setEditingItem(ReceiptItem(id = UUID.randomUUID(), name = "", price = 0.0))
                        },
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),

                        modifier = Modifier
                            .fillMaxWidth()

                    ) {
                        Icon(Icons.Default.Add, "Add", Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Add New Item Manually")
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // --- Item List ---
                items(items, key = { it.id }) { item ->
                    ItemRow(
                        item = item,
                        onClick = { setEditingItem(item) },
                        onDeleteClick = { onDeleteItem(item) }
                    )
                }
            } // End LazyColumn

            // --- CHECKOUT SECTION  ---
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            OutlinedTextField(
                value = taxInput,
                onValueChange = { taxInput = it },
                label = { Text("Total Tax") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary, //this block of code took an hour because of kotlin versions.
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
                // --- END OF CHANGE ---
            )

            Text(
                "Double check your items and prices are correct!",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Button(
                onClick = { onGoToTip(calculatedTotals) },
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                enabled = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text("Go to Tip")
            }
        } // End Main Column

        // --- Dialogs ---
        editingItem?.let { item ->
            EditItemDialog(
                item = item,
                allPeople = people,
                onDismiss = { setEditingItem(null) },
                onSave = { updatedItem ->
                    onUpdateItem(updatedItem)
                    setEditingItem(null)
                }
            )
        }
    } // End Scaffold
}