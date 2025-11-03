package com.example.receiptsplitter.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.receiptsplitter.data.PersonTotal
import com.example.receiptsplitter.data.ReceiptItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.TextFieldDefaults

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

    var description by remember {
        mutableStateOf(
            TextFieldValue("Receipt from ${SimpleDateFormat("MMM dd", Locale.US).format(Date())}")
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
                            if (isViewOnly) "Saved Summary" else "Final Summary",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        // ---  Use bottomBar slot to guarantee bottom placement ---
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 1. Horizontal padding for the buttons
                        .padding(horizontal = 16.dp)
                        // 2. Top padding for separation from list
                        .padding(top = 16.dp)
                        // 3. Generous bottom padding increased to 48.dp
                        .padding(bottom = 48.dp)
                ) {
                    // Conditional Buttons (Fixed position at the bottom)
                    if (isViewOnly) {
                        // Wrap the single button in a Row for consistent sizing properties
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onNavigateBack,
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                                modifier = Modifier.weight(1f)
                                    .padding(bottom = 8.dp)
                            ) {
                                Text("Done")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),

                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = onNewBill,
                                modifier = Modifier.weight(1f))
                            {
                                Text("Don't Save")
                            }
                            Button(
                                onClick = { onSaveReceipt(description.text)},
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                                modifier = Modifier.weight(1f)) {
                                Text("Save Receipt")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        // --- Main Column takes padding from Scaffold (including new bottomBar padding) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Includes spacing for the bottomBar
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            // --- Bill Description Field (Fixed position) ---
            if (!isViewOnly) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Bill Description") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- TotalsDisplay expands to fill remaining space ---
            TotalsDisplay(
                totals = finalTotals,
                onPersonClick = { personTotal ->
                    setViewingPerson(personTotal)
                },
                modifier = Modifier.weight(1f) // This forces the list to expand down
            )

        } // End Main Column

        viewingPerson?.let { personTotal ->
            BreakdownDialog(
                personTotal = personTotal,
                allItems = allItems,
                onDismiss = { setViewingPerson(null) }
            )
        }
    }
}