package com.example.receiptsplitter

import android.Manifest
import android.graphics.Rect
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
import kotlinx.coroutines.launch
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
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.receiptsplitter.data.Person
import com.example.receiptsplitter.data.PersonTotal
import com.example.receiptsplitter.data.ReceiptItem
import com.example.receiptsplitter.screens.BillSplitterScreen
import com.example.receiptsplitter.screens.HomeScreen
import com.example.receiptsplitter.data.SavedReceiptSummary
import java.text.SimpleDateFormat
import java.util.Date
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.receiptsplitter.data.AppDatabase
import com.example.receiptsplitter.viewmodel.ReceiptViewModel
import com.example.receiptsplitter.viewmodel.ReceiptViewModelFactory
import androidx.lifecycle.viewModelScope
import com.example.receiptsplitter.data.toSummary


class MainActivity : ComponentActivity() {

    // --- STATE ---
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val receiptDao by lazy { database.receiptDao() }

    private val viewModel: ReceiptViewModel by lazy {
        ViewModelProvider(this, ReceiptViewModelFactory(receiptDao))
            .get(ReceiptViewModel::class.java)
    }

    // This will hold the URI for the camera to save its photo to
    private var tempImageUri: Uri? = null

    // --- LAUNCHERS ---
    // Register all the launchers here, in onCreate

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
            Toast.makeText(
                this,
                "Camera permission is required to use the camera.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the temp URI once
        tempImageUri = getTempUri(this)

        setContent {
            ReceiptSplitterTheme {
                val navController = rememberNavController()

                // --- Get ViewModel Instance ---
                val viewModel: ReceiptViewModel = viewModel(
                    factory = ReceiptViewModelFactory(receiptDao) // Use the factory
                )

                // --- Collect State from ViewModel ---
                val savedReceiptsList by viewModel.savedReceipts.collectAsState()
                val currentReceiptItems by viewModel.receiptItems.collectAsState()

                NavHost(
                    navController = navController,
                    startDestination = NavRoutes.HOME_SCREEN // Start on the home screen
                ) {
                    // --- Home Screen Destination ---
                    composable(NavRoutes.HOME_SCREEN) {
                        HomeScreen(
                            savedReceipts = savedReceiptsList,
                            onNavigateToSplitter = {
                                viewModel.clearCurrentItems() // Clear items before navigating
                                navController.navigate(NavRoutes.BILL_SPLITTER_SCREEN)
                            },
                            onDeleteReceipt = { receiptToDelete ->
                                viewModel.deleteSavedReceipt(receiptToDelete)
                            },
                            onReceiptClick = { receipt ->
                                Log.d("NAV", "Clicked receipt: ${receipt.id}")
                            }
                        )
                    } // <-- End of Home Screen Composable

                    // --- Bill Splitter Screen Destination ---
                    composable(NavRoutes.BILL_SPLITTER_SCREEN) { // <-- WRAP EVERYTHING IN THIS
                        // --- MOVE DIALOG STATE INSIDE HERE ---
                        var showOptionsDialog by remember { mutableStateOf(false) }
                        var showPermissionDialog by remember { mutableStateOf(false) }

                        // --- MOVE BILLSPLITTER CALL INSIDE HERE ---
                        BillSplitterScreen(
                            items = currentReceiptItems, // Use state from ViewModel
                            onScanReceiptClick = { showOptionsDialog = true },
                            onUpdateItem = viewModel::updateReceiptItem, // Pass ViewModel function reference
                            onDeleteItem = viewModel::deleteReceiptItem, // Pass ViewModel function reference
                            onSaveAndExit = { finalTotals ->
                                viewModel.saveCurrentReceipt(finalTotals) // Call ViewModel function
                                navController.popBackStack()
                            },
                            onNavigateBack = { navController.navigateUp() }
                        )

                        // --- MOVE DIALOGS INSIDE HERE ---
                        if (showOptionsDialog) {
                            AlertDialog(
                                onDismissRequest = { showOptionsDialog = false },
                                title = { Text("Scan a new receipt") },
                                text = { Text("How do you want to add your receipt?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        showOptionsDialog = false; showPermissionDialog = true
                                    }) { Text("Open Camera") }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        showOptionsDialog = false; galleryLauncher.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                    }) { Text("From Gallery") }
                                }
                            )
                        }

                        if (showPermissionDialog) {
                            AlertDialog(
                                onDismissRequest = { showPermissionDialog = false },
                                title = { Text("Camera Permission Needed") },
                                text = { Text("We need camera access to scan your receipt directly.") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        showPermissionDialog = false; permissionLauncher.launch(
                                        Manifest.permission.CAMERA
                                    )
                                    }) { Text("Continue") }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        showPermissionDialog = false
                                    }) { Text("Cancel") }
                                }
                            )
                        }
                    } // <-- End of Bill Splitter Composable
                } // <-- End of NavHost
            } // <-- End of Theme
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
                .addOnSuccessListener { visionText -> // ML Kit processing successful

                    // 1. --- Collect Elements and Coordinates ---
                    val elementsWithBoxes = mutableListOf<Pair<String, Rect>>()
                    val potentialPrices = mutableListOf<Pair<String, Rect>>()
                    val priceRegex = "\\$?(\\d+\\.\\d{2})".toRegex()

                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            for (element in line.elements) {
                                val elementText = element.text
                                val elementFrame = element.boundingBox

                                if (elementFrame != null) {
                                    elementsWithBoxes.add(Pair(elementText, elementFrame))
                                    // Check if this element looks like a price
                                    if (priceRegex.matches(elementText.replace("$", ""))) { // Check against text without '$'
                                        potentialPrices.add(Pair(elementText, elementFrame))
                                    }
                                }
                                // Log.d("ML_KIT_COORDS", ...)
                            }
                        }
                    }
                    // --- End Collection ---


                    // 2. --- Parse Using Coordinates (AFTER collecting) ---
                    val parsedItems = parseUsingCoordinates(elementsWithBoxes, potentialPrices)


                    // 3. --- Update ViewModel (Directly, NO viewModelScope.launch) ---
                    viewModel.setCurrentItems(parsedItems) // 'viewModel' is the instance from MainActivity


                } // End addOnSuccessListener
                .addOnFailureListener { e ->
                    Log.e("TEXT_RECOGNITION", "Failed:", e)
                    // Optionally update ViewModel with error state or empty list
                    viewModel.setCurrentItems(emptyList())
                }
        } catch (e: Exception) {
            Log.e("TEXT_RECOGNITION", "Error processing input image:", e)
            // Optionally update ViewModel with error state or empty list
            viewModel.setCurrentItems(emptyList())
        }
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


    // --- Coordinate-Based Parsing Logic ---
    private fun parseUsingCoordinates(
        elementsWithBoxes: List<Pair<String, Rect>>,
        potentialPrices: List<Pair<String, Rect>>


    ): List<ReceiptItem> {
        // A helper function to check if two rectangles vertically overlap significantly
        fun verticallyOverlaps(rect1: Rect, rect2: Rect): Boolean {
            val overlapThreshold =
                (rect1.height() + rect2.height()) / 4 // Allow some vertical leeway
            return rect1.top < rect2.bottom - overlapThreshold && rect1.bottom > rect2.top + overlapThreshold
        }

        // Group elements roughly by line based on vertical overlap
        val lines = mutableListOf<MutableList<Pair<String, Rect>>>()
        val processedElements =
            mutableSetOf<Pair<String, Rect>>() // Keep track of elements already added

        elementsWithBoxes.forEach { element ->
            if (element !in processedElements) {
                val currentLine = mutableListOf(element)
                processedElements.add(element)

                // Find other elements that overlap vertically with this one
                elementsWithBoxes.forEach { otherElement ->
                    if (otherElement !in processedElements && verticallyOverlaps(
                            element.second,
                            otherElement.second
                        )
                    ) {
                        currentLine.add(otherElement)
                        processedElements.add(otherElement)
                    }
                }
                // Sort elements on the line by their horizontal position (left to right)
                currentLine.sortBy { it.second.left }
                lines.add(currentLine)
            }
        }

        // Sort the lines roughly from top to bottom
        lines.sortBy { it.firstOrNull()?.second?.top ?: 0 }

        Log.d("COORD_PARSE", "Found ${lines.size} potential lines.")

        val items = mutableListOf<ReceiptItem>()
        val priceRegex = "\\$?(\\d+\\.\\d{2})".toRegex() // Regex to find price shape
        val ignoreKeywords = listOf(
            "SUBTOTAL", "TAX", "TOTAL", "CASH", "CHANGE", "ORDER",
            "TABLE", "CLOVER", "TIP", "THANK", "VISITING"
        ) // More comprehensive ignore list
        val dateRegex = "\\d{1,2}-\\w{3}-\\d{4}".toRegex() // Simple date pattern

        lines.forEach { lineElements ->

            // Basic check to ignore lines that are mostly numbers or likely headers/footers
            val lineText = lineElements.joinToString(" ") { it.first }

            // --- THIS is the combined check ---
            if (ignoreKeywords.any {
                    lineText.uppercase().contains(it)
                } || dateRegex.containsMatchIn(lineText)) {
                // Log.d("COORD_PARSE", "Ignoring line (keyword/date): $lineText")
                return@forEach // Skip this line
            }

            val words = mutableListOf<Pair<String, Rect>>()
            val pricesOnLine = mutableListOf<Pair<Double, Rect>>()

            // Separate words and prices on this line
            lineElements.forEach { (text, rect) ->
                // Try to match the price pattern, allowing optional '$'
                val priceMatch = priceRegex.matchEntire(text.replace("$", ""))
                if (priceMatch != null) {
                    // It looks like a price, try converting
                    priceMatch.groupValues.getOrNull(1)?.toDoubleOrNull()?.let { priceValue ->
                        pricesOnLine.add(Pair(priceValue, rect))
                    }
                } else {
                    // It's not just a price, consider it a word if it's not tiny/numeric
                    if (text.length > 1 || text.any { it.isLetter() }) {
                        words.add(Pair(text, rect))
                    }
                }
            }
            if (words.isNotEmpty() && pricesOnLine.isNotEmpty()) {
                // Find the price element furthest to the right
                val bestPricePair = pricesOnLine.maxByOrNull { it.second.right }

                if (bestPricePair != null) {
                    // Combine words that appear *before* the chosen price
                    val potentialNameWords =
                        words.filter { it.second.right < bestPricePair.second.left }
                    var potentialName = potentialNameWords.joinToString(" ") { it.first }

                    // Simple cleaning
                    potentialName = potentialName.replaceFirst("^\\d+\\s+".toRegex(), "")
                        .trim() // Remove leading qty
                    potentialName = potentialName.replaceFirst("\\s+[^A-Za-z0-9]+$".toRegex(), "")
                        .trim() // Remove trailing symbols

                    if (potentialName.isNotEmpty()) {
                        Log.d(
                            "COORD_PARSE",
                            "   -> Associated: '$potentialName' with price ${bestPricePair.first}"
                        )
                        items.add(ReceiptItem(name = potentialName, price = bestPricePair.first))
                    } else {
                        Log.d(
                            "COORD_PARSE",
                            "   -> Found price ${bestPricePair.first} but name was empty after cleaning on line: $lineText"
                        )
                    }
                } else {
                    Log.d(
                        "COORD_PARSE",
                        "   -> Found words but couldn't determine best price on line: $lineText"
                    )
                }
            } else {
                // Log.d("COORD_PARSE", "   -> No clear words/prices found on line: $lineText")
            }
        } // End of loop through lines

        return items
    }
}