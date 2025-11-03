package com.example.receiptsplitter.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.receiptsplitter.data.SavedReceiptSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- Total hours spent here: 14 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    savedReceipts: List<SavedReceiptSummary>,
    onNavigateToSplitter: () -> Unit,
    onDeleteReceipt: (SavedReceiptSummary) -> Unit,
    onReceiptClick: (SavedReceiptSummary) -> Unit,
    onUpdateReceiptName: (SavedReceiptSummary, String) -> Unit
) {
    // --- STATE MODIFICATIONS ---
    var showFilterMenu by remember { mutableStateOf(false) }
    var receiptToEdit by remember { mutableStateOf<SavedReceiptSummary?>(null) }

    // MODIFIED: State for Filtering is now a List<String> for multi-selection
    var filterPerson by remember { mutableStateOf<List<String>>(emptyList()) }
    var showPersonFilterDialog by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf<String>("date") }

    // --- LOGIC: Extract unique people for the filter dialog ---
    val uniquePeople = remember(savedReceipts) {
        savedReceipts
            .flatMap { it.personTotals }
            .map { it.person.name }
            .distinct()
            .sorted()
    }

    // --- LOGIC: Filter and Sort Receipts (MODIFIED FOR MULTI-SELECT) ---
    val filteredAndSortedReceipts = remember(savedReceipts, filterPerson, sortOrder) {
        savedReceipts
            .filter { receipt ->
                if (filterPerson.isEmpty()) {
                    true // No filter applied
                } else {
                    // Check if ANY person in the receipt is present in the filterPerson list
                    receipt.personTotals.any { personTotal ->
                        filterPerson.contains(personTotal.person.name)
                    }
                }
            }
            .sortedWith(
                when (sortOrder) {
                    "total" -> compareByDescending { it.grandTotal }
                    "date" -> compareByDescending { it.timestamp } // Default
                    else -> compareByDescending { it.timestamp }
                }
            )
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
                            "Your Receipts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    navigationIcon = {
                        IconButton(onClick = { /* Does absolutely nothing but be pretty, honestly dont need it but its fiiiiine */ }) {
                            Icon(Icons.Filled.Spa, "App Icon", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { showFilterMenu = true }) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    "Filter Options",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false },
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sort by Date", color = Color.Black) },
                                    onClick = {
                                        sortOrder = "date"
                                        showFilterMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort by Total", color = Color.Black) },
                                    onClick = {
                                        sortOrder = "total"
                                        showFilterMenu = false
                                    }
                                )
                                // Opens the new filter dialog
                                DropdownMenuItem(
                                    text = {
                                        Text("Filter by Person...", color = Color.Black)
                                    },
                                    onClick = {
                                        showFilterMenu = false
                                        showPersonFilterDialog = true
                                    }
                                )
                                // Option to clear filter if one is active
                                if (filterPerson.isNotEmpty()) {
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = {
                                            Text("Clear ${filterPerson.size} Filters", color = Color.Black)
                                        },
                                        onClick = {
                                            filterPerson = emptyList() // Clear the list
                                            showFilterMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },

        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Create New Bill") },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Create New Bill") },
                onClick = onNavigateToSplitter,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        floatingActionButtonPosition = FabPosition.Center,

        containerColor = Color.Transparent
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Display the current filter status
            if (filterPerson.isNotEmpty()) {
                item {
                    Text(
                        "Filtering by: ${filterPerson.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            if (filteredAndSortedReceipts.isEmpty()) {
                item {
                    Text(
                        "No saved receipts match the current filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                // Use the filtered and sorted list
                items(filteredAndSortedReceipts, key = { it.id }) { receipt ->
                    SavedReceiptCard(
                        receipt = receipt,
                        onDelete = { onDeleteReceipt(receipt) },
                        onClick = { onReceiptClick(receipt) },
                        onLongClick = { receiptToEdit = receipt},
                        elevation = 6.dp
                    )
                }
            }
        }

        // Dialogs - referencing the new file names
        receiptToEdit?.let { receipt ->
            EditNameDialog(
                initialName = receipt.description,
                onDismiss = { receiptToEdit = null },
                onSave = { newName ->
                    onUpdateReceiptName(receipt, newName)
                    receiptToEdit = null
                }
            )
        }

        if (showPersonFilterDialog) {
            FilterByPersonDialog(
                uniquePeople = uniquePeople,
                currentFilter = filterPerson, // Pass the List<String>
                onDismiss = { showPersonFilterDialog = false },
                onSelectPerson = { selectedList -> // Receive the new List<String>
                    filterPerson = selectedList
                    // --- FIX APPLIED HERE: Removed showPersonFilterDialog = false ---
                }
            )
        }
    }
}

// Card for displaying a single saved receipt
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavedReceiptCard(
    receipt: SavedReceiptSummary,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    elevation: Dp = 4.dp
) {
    // --- Save receipts comes with date of when it was scanned automatically ---
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateString = dateFormat.format(Date(receipt.timestamp))

    val peopleCount = receipt.personTotals.size
    val peopleNames = receipt.personTotals.map { it.person.name }
    val peopleListDisplay = if (peopleNames.size > 3) {
        "${peopleNames.take(3).joinToString(", ")} + ${peopleNames.size - 3} others"
    } else {
        peopleNames.joinToString(", ")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // 1. Description (Title)
                Text(receipt.description, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                // 2. Date
                Text(
                    dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                // 3. People Count
                Text(
                    text = "${peopleCount} People Involved",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // 4. Compact Name List
                Text(
                    text = "Participants: $peopleListDisplay",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                // 5. Total
                Text(
                    "Total Bill: $${String.format(Locale.US, "%.2f", receipt.grandTotal)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            // 6. Smol X on the right side
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Clear, "Delete Receipt", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
