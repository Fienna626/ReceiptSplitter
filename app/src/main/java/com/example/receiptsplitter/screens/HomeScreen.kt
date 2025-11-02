package com.example.receiptsplitter.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.example.receiptsplitter.data.SavedReceiptSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    savedReceipts: List<SavedReceiptSummary>,
    onNavigateToSplitter: () -> Unit,
    onDeleteReceipt: (SavedReceiptSummary) -> Unit,
    onReceiptClick: (SavedReceiptSummary) -> Unit
) {
    var showFilterMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // Use an actual color for the TopAppBar
            TopAppBar(
                title = { Text("Your Receipts") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface, // Solid color for the bar
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                // --- Add padding for system status bar ---
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                navigationIcon = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Filled.Spa, "App Icon", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(Icons.Filled.MoreVert, "Filter Options")
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("Sort by Date") }, onClick = { showFilterMenu = false })
                            DropdownMenuItem(text = { Text("Sort by Total") }, onClick = { showFilterMenu = false })
                            DropdownMenuItem(text = { Text("Filter by Person...") }, onClick = { showFilterMenu = false })
                        }
                    }
                }
            )
        },
        bottomBar = {
            // Use an actual color for the BottomAppBar
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface, // Solid color for the bar
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                // --- Add padding for system navigation bar ---
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Button(
                    onClick = onNavigateToSplitter,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Create New Bill")
                }
            }
        },
        // It should implicitly use the background of the parent Composable (the rounded Surface)
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                // --- IMPORTANT: ONLY apply content padding, NOT windowInsetsPadding here ---
                // The Scaffold paddingValues already account for Top/Bottom app bars
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (savedReceipts.isEmpty()) {
                item {
                    Text(
                        "No saved receipts. Tap 'Create New Bill' to start!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(savedReceipts, key = { it.id }) { receipt ->
                    SavedReceiptCard(
                        receipt = receipt,
                        onDelete = { onDeleteReceipt(receipt) },
                        onClick = { onReceiptClick(receipt) }
                    )
                }
            }
        }
    }
}

// Card for displaying a single saved receipt
@Composable
fun SavedReceiptCard(
    receipt: SavedReceiptSummary,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateString = dateFormat.format(Date(receipt.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        // Use the 'surface' color (your OffWhite)
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Add a shadow
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(receipt.description, style = MaterialTheme.typography.titleMedium)
                Text(
                    dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Total: $${String.format(Locale.US, "%.2f", receipt.grandTotal)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Clear, "Delete Receipt", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}