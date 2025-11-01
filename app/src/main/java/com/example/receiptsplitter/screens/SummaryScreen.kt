package com.example.receiptsplitter.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.receiptsplitter.data.PersonTotal
import com.example.receiptsplitter.data.ReceiptItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    finalTotals: List<PersonTotal>,
    allItems: List<ReceiptItem>,
    onNavigateBack: () -> Unit,
    onSaveReceipt: (description: String) -> Unit,
    onNewBill: () -> Unit,
    isViewOnly: Boolean
) {
    val (viewingPerson, setViewingPerson) = remember { mutableStateOf<PersonTotal?>(null) }

    // State for the bill name/description
    // We get the default name. If it's view-only, we'll get the saved name later.
    var description by remember {
        mutableStateOf(
            TextFieldValue("Receipt from ${SimpleDateFormat("MMM dd", Locale.US).format(Date())}")
        )
    }

    // --- Main Screen UI ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isViewOnly) "Saved Summary" else "Final Summary") },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- 1. Bill Description (Only show if NOT view-only) ---
            if (!isViewOnly) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Bill Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- 2. Totals Display (Always show) ---
            TotalsDisplay(
                totals = finalTotals,
                onPersonClick = { personTotal ->
                    setViewingPerson(personTotal)
                }
            )

            Spacer(modifier = Modifier.weight(1f)) // Pushes buttons to bottom

            // --- 3. Conditional Buttons (At bottom) ---
            if (isViewOnly) {
                // --- Show only a "Done" button ---
                Button(
                    onClick = onNavigateBack, // "Done" just goes back
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            } else {
                // --- Show the "Save" / "New Bill" buttons ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onNewBill, modifier = Modifier.weight(1f)) {
                        Text("New Bill (Don't Save)")
                    }
                    Button(onClick = { onSaveReceipt(description.text) }, modifier = Modifier.weight(1f)) {
                        Text("Save Receipt")
                    }
                }
            }
            // --- ALL DUPLICATE CODE IS REMOVED ---

        } // End Column

        // --- Breakdown Dialog (remains the same) ---
        viewingPerson?.let { personTotal ->
            BreakdownDialog(
                personTotal = personTotal,
                allItems = allItems,
                onDismiss = { setViewingPerson(null) }
            )
        }
    } // End Scaffold
}