package com.example.receiptsplitter

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.receiptsplitter.ui.theme.ReceiptSplitterTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.Locale
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import com.example.receiptsplitter.data.Person
import com.example.receiptsplitter.data.PersonTotal
import com.example.receiptsplitter.data.ReceiptItem
import com.example.receiptsplitter.screens.BillSplitterScreen


class MainActivity : ComponentActivity() {

    // --- STATE ---
    // This is the "source of truth" for our list of items.
    // It's now stored in the Activity, not in the composable.
    private var receiptItem = mutableStateOf(listOf<ReceiptItem>())

    // This will hold the URI for the camera to save its photo to
    private var tempImageUri: Uri? = null

    // --- LAUNCHERS ---
    // We register all the launchers here, in onCreate

    // Launcher for picking an image from the GALLERY
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            processImage(uri) // Process the selected image
        }
    }

    // Launcher for taking a picture with the CAMERA
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempImageUri?.let { processImage(it) } // Process the image saved at the temp URI
        }
    }

    // Launcher for requesting CAMERA PERMISSION
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission was granted, launch the camera!
            launchCamera()
        } else {
            // Permission was denied
            Toast.makeText(this, "Camera permission is required to use the camera.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the temp URI once
        tempImageUri = getTempUri(this)

        setContent {
            ReceiptSplitterTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

                    // --- DIALOG STATE ---
                    // This state lives here so the Activity can control the dialogs
                    var showOptionsDialog by remember { mutableStateOf(false) }
                    var showPermissionDialog by remember { mutableStateOf(false) }

                    // --- THE MAIN UI ---
                    BillSplitterScreen(
                        // Pass the item list down to the UI
                        items = receiptItem.value,

                        // When the button is clicked, just show the dialog
                        onScanReceiptClick = { showOptionsDialog = true },

                        // Pass a function to update the item
                        onUpdateItem = { updatedItem ->
                            // Find the item in our list and replace it
                            val currentList = receiptItem.value.toMutableList()
                            val index = currentList.indexOfFirst { it.id == updatedItem.id }
                            if (index != -1) {
                                currentList[index] = updatedItem
                                receiptItem.value = currentList
                            }
                        },
                        onDeleteItem = { itemToDelete ->
                            val currentList = receiptItem.value.toMutableList()
                            currentList.remove(itemToDelete)
                            receiptItem.value = currentList
                        }
                    )


                    // --- DIALOGS ---
                    // The dialogs are part of the UI, controlled by the state

                    // Dialog 1: "Choose an option"
                    if (showOptionsDialog) {
                        AlertDialog(
                            onDismissRequest = { showOptionsDialog = false },
                            title = { Text("Scan a new receipt") },
                            text = { Text("How do you want to add your receipt?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showOptionsDialog = false
                                        showPermissionDialog = true // Show permission dialog
                                    }
                                ) { Text("Open Camera") }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showOptionsDialog = false
                                        // Launch the gallery
                                        galleryLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                ) { Text("From Gallery") }
                            }
                        )
                    }

                    // Dialog 2: "Camera Permission"
                    if (showPermissionDialog) {
                        AlertDialog(
                            onDismissRequest = { showPermissionDialog = false },
                            title = { Text("Camera Permission Needed") },
                            text = { Text("We need camera access to scan your receipt directly.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showPermissionDialog = false
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                ) { Text("Continue") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showPermissionDialog = false }) { Text("Cancel") }
                            }
                        )
                    }
                }
            }
        }
    }

    // --- HELPER FUNCTIONS ---

    private fun getTempUri(context: Context): Uri {
        val file = File.createTempFile("temp_image", ".jpg", context.cacheDir)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // This must match your AndroidManifest
            file
        )
    }

    private fun launchCamera() {
        // 1. Create a new, non-null URI and store it
        val newImageUri = getTempUri(this)

        // 2. Save this new URI to your class variable
        tempImageUri = newImageUri

        // 3. Launch the camera with the new, non-null URI
        cameraLauncher.launch(newImageUri)
    }

    private fun processImage(uri: Uri) {
        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val inputImage = InputImage.fromFilePath(this, uri)
            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val rawText = visionText.text
                    Log.d("TEXT_RECOGNITION", "Success: \n$rawText")
                    // Call the parser and update our state
                    receiptItem.value = parseReceiptText(rawText)
                }
                .addOnFailureListener { e ->
                    Log.e("TEXT_RECOGNITION", "Failed:", e)
                }
        } catch (e: Exception) {
            Log.e("TEXT_RECOGNITION", "Error:", e)
        }
    }

    fun parseReceiptText(rawText: String): List<ReceiptItem> {
        // We are using the "greedy" regex to get the LAST price on the line
        val regex = "(.+)\\s+\\$(\\d+\\.\\d{2})".toRegex()
        val ignoreKeywords = listOf("SUBTOTAL", "TAX", "TOTAL", "CASH", "CHANGE", "YOUR ORDER")
        val items = mutableListOf<ReceiptItem>()


        rawText.lines().forEach { line ->
            val shouldIgnore = ignoreKeywords.any { keyword ->
                line.uppercase().contains(keyword)
            }

            if (!shouldIgnore) {
                val match = regex.find(line)
                if (match != null) {
                    val (name, priceStr) = match.destructured
                    val price = priceStr.toDoubleOrNull() ?: 0.0

                    // Clean the name by splitting at the "@"
                    var cleanName = if (name.contains("@")) {
                        name.split("@")[0].trim()
                    } else {
                        name.trim()
                    }
                    cleanName = cleanName.replaceFirst("^\\d+\\s+".toRegex(), "").trim()

                    if (cleanName.isNotEmpty() && price > 0.0) {
                        items.add(ReceiptItem(name = cleanName, price = price))
                    }
                }
            }
        }
        return items
    }
    // --- THE CALCULATION LOGIC ---
    fun calculateTotals(
        people: List<Person>,
        items: List<ReceiptItem>,
        taxStr: String,
        tipStr: String
    ): List<PersonTotal> {

        val totalTax = taxStr.toDoubleOrNull() ?: 0.0
        val totalTip = tipStr.toDoubleOrNull() ?: 0.0

        // 1. Calculate each person's subtotal
        val personSubtotals = mutableMapOf<Person, Double>()
        people.forEach { personSubtotals[it] = 0.0 } // Initialize all to 0

        items.forEach { item ->
            val numPeopleForItem = item.assignedPeople.size
            if (numPeopleForItem > 0) {
                val pricePerPerson = item.price / numPeopleForItem
                item.assignedPeople.forEach { person ->
                    // Add this item's share to the person's subtotal
                    personSubtotals[person] = (personSubtotals[person] ?: 0.0) + pricePerPerson
                }
            }
        }

        // 2. Calculate the total subtotal that has been accounted for
        val accountedForSubtotal = personSubtotals.values.sum()
        if (accountedForSubtotal == 0.0) {
            // Avoid division by zero; return empty list
            return emptyList()
        }

        // 3. Calculate and return the final list
        val finalTotals = mutableListOf<PersonTotal>()
        personSubtotals.forEach { (person, subtotal) ->
            // Find what percentage of the subtotal this person is responsible for
            val percentageOfBill = subtotal / accountedForSubtotal

            // Calculate their share of the tax and tip
            val taxShare = totalTax * percentageOfBill
            val tipShare = totalTip * percentageOfBill

            // Calculate their final total
            val totalOwed = subtotal + taxShare + tipShare

            finalTotals.add(
                PersonTotal(
                    person = person,
                    subtotal = subtotal,
                    taxShare = taxShare,
                    tipShare = tipShare,
                    totalOwed = totalOwed
                )
            )
        }

        return finalTotals
    }
}

// --- UI for the row of people ---
@Composable
fun PeopleList(
    people: List<Person>,
    onAddPerson: () -> Unit,
    onEditPersonName: (Person, String) -> Unit,
    onDeletePerson: (Person) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(people) { person ->
            var personName by remember { mutableStateOf(TextFieldValue(person.name)) }

            OutlinedTextField(
                value = personName,
                onValueChange = {
                    personName = it
                    onEditPersonName(person, it.text)
                },
                label = { Text("Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { onDeletePerson(person) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Remove Person")
                    }
                },
                modifier = Modifier.width(170.dp)
            )
        }

        item {
            IconButton(onClick = onAddPerson) {
                Icon(Icons.Default.Add, contentDescription = "Add Person")
            }
        }
    }
}

