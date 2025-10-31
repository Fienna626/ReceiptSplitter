package com.example.receiptsplitter.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.receiptsplitter.data.PersonTotal
import com.example.receiptsplitter.data.ReceiptItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    finalTotals: List<PersonTotal>,
    allItems: List<ReceiptItem>, // Needed for the breakdown dialog
    onNavigateBack: () -> Unit,
    onSaveAndExit: () -> Unit
) {
    // State to manage the breakdown dialog
    val (viewingPerson, setViewingPerson) = remember { mutableStateOf<PersonTotal?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Final Summary") },
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
            verticalArrangement = Arrangement.SpaceBetween // Pushes button to bottom
        ) {
            // Re-use your TotalsDisplay composable!
            TotalsDisplay(
                totals = finalTotals,
                onPersonClick = { personTotal ->
                    setViewingPerson(personTotal) // Open dialog on click
                }
            )

            // "Save and Exit" button
            Button(
                onClick = onSaveAndExit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save and Exit to Home")
            }
        }

        // --- Breakdown Dialog ---
        // This will pop up when viewingPerson is not null
        viewingPerson?.let { personTotal ->
            BreakdownDialog(
                personTotal = personTotal,
                allItems = allItems,
                onDismiss = { setViewingPerson(null) }
            )
        }
    }
}