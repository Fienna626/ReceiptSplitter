package com.example.receiptsplitter.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.receiptsplitter.data.PersonTotal
import java.util.Locale
import androidx.compose.material3.TextFieldDefaults

private sealed class TipOption {
    data class Percent(val value: Double) : TipOption()
    data class CustomAmount(val value: Double) : TipOption()
    data object CustomPercent : TipOption()
}
// I was told all restaurants do tip *after* tax is added so thats what I did.
// Fun fact: I go out alot and THAT WAS NOT THE CASE YOUR HONOR
// Restaurants RANDOMIZE AFTER OR BEFORE TIP CALCULATIONS

// --- Tip Calculations ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipScreen(
    totalsBeforeTip: List<PersonTotal>,
    onNavigateBack: () -> Unit,
    onGoToSummary: (List<PersonTotal>) -> Unit
) {

    // --- State & Calculations ---
    var selectedOption by remember { mutableStateOf<TipOption>(TipOption.Percent(15.0)) }
    var customPercentString by remember { mutableStateOf("") }
    var customAmountString by remember { mutableStateOf("") }

    val totalBeforeTip = totalsBeforeTip.sumOf { it.totalOwed }
    val tipAmount = when (val option = selectedOption) {
        is TipOption.Percent -> totalBeforeTip * (option.value / 100.0)
        is TipOption.CustomAmount -> option.value
        is TipOption.CustomPercent -> customPercentString.toDoubleOrNull()?.let { totalBeforeTip * (it / 100.0) } ?: 0.0
    }
    val finalGrandTotal = totalBeforeTip + tipAmount

    fun getFinalTotals(): List<PersonTotal> {
        return totalsBeforeTip.map { personTotal ->
            val personShareOfTotal = if (totalBeforeTip > 0) personTotal.totalOwed / totalBeforeTip else 0.0
            val personTipShare = tipAmount * personShareOfTotal
            personTotal.copy(
                tipShare = personTipShare,
                totalOwed = personTotal.totalOwed + personTipShare
            )
        }
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
                            "Add a Tip",
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
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp) // Changed default spacing
            ) {

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Before Tip", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp)) // Reduced gap here
                    Text(
                        "$${String.format(Locale.US, "%.2f", totalBeforeTip)}",
                        style = MaterialTheme.typography.displaySmall
                    )
                }
                // --- End Total Before Tip Adjustment ---

                HorizontalDivider()

                // --- Preset Tip Buttons ---
                val presets = listOf(5.0, 10.0, 15.0, 18.0)
                val notes = mapOf(
                    5.0 to "felt that",
                    10.0 to ":)",
                    15.0 to "Average",
                    18.0 to "Sounds like you can buy me boba eheh",
                )

                presets.forEach { percent ->
                    TipButton(
                        text = "${percent.toInt()}%",
                        note = notes[percent] ?: "",
                        isSelected = selectedOption == TipOption.Percent(percent),
                        onClick = {
                            selectedOption = TipOption.Percent(percent)
                            customAmountString = ""
                            customPercentString = ""

                        },

                        )
                }

                // --- Custom Tip Options ---
                TipButton(
                    text = "Custom",
                    note = "Enter your own % or amount",
                    isSelected = selectedOption is TipOption.CustomAmount || selectedOption is TipOption.CustomPercent,
                    onClick = {
                        selectedOption = TipOption.CustomPercent
                    }
                )

                if (selectedOption is TipOption.CustomAmount || selectedOption is TipOption.CustomPercent) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customPercentString,
                            onValueChange = {
                                customPercentString = it
                                selectedOption = TipOption.CustomPercent
                                customAmountString = ""
                            },
                            label = { Text("%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        Text("or")
                        OutlinedTextField(
                            value = customAmountString,
                            onValueChange = {
                                customAmountString = it
                                val amount = it.toDoubleOrNull() ?: 0.0
                                selectedOption = TipOption.CustomAmount(amount)
                                customPercentString = ""
                            },
                            label = { Text("$") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }
                }

                // --- Final Totals Display ---
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tip Amount:", style = MaterialTheme.typography.titleMedium)
                    Text("$${String.format(Locale.US, "%.2f", tipAmount)}", style = MaterialTheme.typography.titleMedium)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Grand Total:", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "$${String.format(Locale.US, "%.2f", finalGrandTotal)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    "Note: Tip % is calculated on the total *after* tax.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(Modifier.height(16.dp))

            } // --- End of scrollable Column

            // --- Button locked to the bottom ---
            Button(
                onClick = { onGoToSummary(getFinalTotals()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("Calculate Total Per Person")
            }
        } // --- End of outer Column
    } // --- End of Scaffold
}

// --- TipButton composable ---
@Composable
private fun TipButton(
    text: String,
    note: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Use ElevatedButton to support elevation, BECAUSE, AGAIN, ITS DIFFERENT THAN THE OTHER ONES
    // I should've just made a new data file
    ElevatedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),

        colors = if (isSelected) {
            // Use elevatedButtonColors for consistency
            ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            ButtonDefaults.elevatedButtonColors(
                // Use surface for the non-selected state
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        }
        // border parameter is not needed for ElevatedButton
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, style = MaterialTheme.typography.titleMedium)
            if (note.isNotEmpty()) {
                Text(note, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}