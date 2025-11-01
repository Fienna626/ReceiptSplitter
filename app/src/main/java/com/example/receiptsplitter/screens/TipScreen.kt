package com.example.receiptsplitter.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.receiptsplitter.data.PersonTotal
import java.util.Locale
import kotlin.math.abs

// Sealed class to manage which tip option is selected
private sealed class TipOption {
    data class Percent(val value: Double) : TipOption()
    data class CustomAmount(val value: Double) : TipOption()
    data object CustomPercent : TipOption()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipScreen(
    totalsBeforeTip: List<PersonTotal>,
    onNavigateBack: () -> Unit,
    onGoToSummary: (List<PersonTotal>) -> Unit // Passes the *final* totals
) {
    // --- State ---
    var selectedOption by remember { mutableStateOf<TipOption>(TipOption.Percent(15.0)) }
    var customPercentString by remember { mutableStateOf("") }
    var customAmountString by remember { mutableStateOf("") }

    // --- Calculations ---
    val totalBeforeTip = totalsBeforeTip.sumOf { it.totalOwed } // This is Subtotal + Tax

    val tipAmount = when (val option = selectedOption) {
        is TipOption.Percent -> totalBeforeTip * (option.value / 100.0)
        is TipOption.CustomAmount -> option.value
        is TipOption.CustomPercent -> customPercentString.toDoubleOrNull()?.let { totalBeforeTip * (it / 100.0) } ?: 0.0
    }

    val finalGrandTotal = totalBeforeTip + tipAmount

    // --- Helper Function to create final list ---
    fun getFinalTotals(): List<PersonTotal> {
        return totalsBeforeTip.map { personTotal ->
            // Distribute tip based on each person's share of the pre-tip total
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
            TopAppBar(
                title = { Text("Add a Tip") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "Back") } }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Display Total Before Tip
            Text("Total Before Tip", style = MaterialTheme.typography.titleMedium)
            Text(
                "$${String.format(Locale.US, "%.2f", totalBeforeTip)}",
                style = MaterialTheme.typography.displaySmall
            )

            HorizontalDivider()

            // --- Preset Tip Buttons ---
            val presets = listOf(0.0, 10.0, 15.0, 18.0)
            val notes = mapOf(
                0.0 to "*side eyes*",
                10.0 to "Okkkay",
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
                    }
                )
            }

            // --- Custom Tip Options ---
            TipButton(
                text = "Custom",
                note = "Enter your own % or amount",
                isSelected = selectedOption is TipOption.CustomAmount || selectedOption is TipOption.CustomPercent,
                onClick = {
                    selectedOption = TipOption.CustomPercent // Default to custom %
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
                            selectedOption = TipOption.CustomPercent // Set mode to custom %
                            customAmountString = "" // Clear other field
                        },
                        label = { Text("%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Text("or")
                    OutlinedTextField(
                        value = customAmountString,
                        onValueChange = {
                            customAmountString = it
                            // Try to parse, default to 0
                            val amount = it.toDoubleOrNull() ?: 0.0
                            selectedOption = TipOption.CustomAmount(amount) // Set mode to custom $
                            customPercentString = "" // Clear other field
                        },
                        label = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

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
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // --- Save Button ---
            Button(
                onClick = { onGoToSummary(getFinalTotals()) }, // <-- Call new callback
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Calculate Total Per Person")
            }
        }
    }
}

@Composable
private fun TipButton(
    text: String,
    note: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (isSelected) ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) else ButtonDefaults.outlinedButtonColors(),
        border = if (isSelected) null else ButtonDefaults.outlinedButtonBorder
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, style = MaterialTheme.typography.titleMedium)
            if (note.isNotEmpty()) {
                Text(note, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}