package com.example.receiptsplitter

import android.Manifest
import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope // Needed for launching coroutine from callback
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.receiptsplitter.data.AppDatabase
import com.example.receiptsplitter.data.Person
import com.example.receiptsplitter.data.PersonTotal
import com.example.receiptsplitter.data.ReceiptItem
import com.example.receiptsplitter.data.SavedReceiptEntity // Needed for DB interaction
import com.example.receiptsplitter.data.toEntity // Needed for DB interaction
import com.example.receiptsplitter.screens.BillSplitterScreen
import com.example.receiptsplitter.screens.HomeScreen
import com.example.receiptsplitter.data.SavedReceiptSummary // UI Model
import com.example.receiptsplitter.screens.SetupScreen
import com.example.receiptsplitter.screens.SummaryScreen
import com.example.receiptsplitter.screens.TipScreen
import com.example.receiptsplitter.ui.theme.ReceiptSplitterTheme
import com.example.receiptsplitter.viewmodel.ReceiptViewModel
import com.example.receiptsplitter.viewmodel.ReceiptViewModelFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch // Needed for launching coroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    // --- Database & ViewModel ---
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val receiptDao by lazy { database.receiptDao() }
    private val viewModel: ReceiptViewModel by lazy {
        ViewModelProvider(this, ReceiptViewModelFactory(receiptDao))
            .get(ReceiptViewModel::class.java)
    }

    // This will hold the URI for the camera to save its photo to
    private var tempImageUri: Uri? = null

    // --- LAUNCHERS ---
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            processImage(uri) // Process the selected image
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempImageUri?.let { processImage(it) } // Process the image saved at the temp URI
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera() // Permission granted, launch camera
        } else {
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tempImageUri = getTempUri(this) // Initialize temp URI once

        setContent {
            ReceiptSplitterTheme {
                val navController = rememberNavController()

                // Collect State from ViewModel
                val savedReceiptsList by viewModel.savedReceipts.collectAsState()
                val currentReceiptItems by viewModel.receiptItems.collectAsState()
                val currentPeople by viewModel.currentPeople.collectAsState()
                val finalTotals by viewModel.finalTotals.collectAsState()

                // Dialog State (Managed by Activity's Composable Scope)
                var showOptionsDialog by remember { mutableStateOf(false) }
                var showPermissionDialog by remember { mutableStateOf(false) }

                // --- Navigation Graph ---
                NavHost(
                    navController = navController,
                    startDestination = NavRoutes.HOME_SCREEN
                ) {
                    composable(NavRoutes.HOME_SCREEN) {
                        HomeScreen(
                            savedReceipts = savedReceiptsList,
                            onNavigateToSplitter = {
                                viewModel.clearCurrentItems() // Clears items AND people in ViewModel
                                navController.navigate(NavRoutes.SETUP_SCREEN) // Navigate to Setup
                            },
                            onDeleteReceipt = viewModel::deleteSavedReceipt, // Call ViewModel function
                            onReceiptClick = { receipt ->
                                Log.d("NAV", "Clicked receipt: ${receipt.id}")
                                // TODO: Navigate to details screen
                            }
                        )
                    }

                    composable(NavRoutes.SETUP_SCREEN) {
                        SetupScreen(
                            onNavigateBack = { navController.navigateUp() },
                            onScanReceiptClick = { showOptionsDialog = true }, // Trigger dialog SHOW
                            onProceedToSplit = { peopleList ->
                                viewModel.setPeopleForCurrentSplit(peopleList) // Set people in ViewModel
                                navController.navigate(NavRoutes.BILL_SPLITTER_SCREEN) // Navigate to Splitter
                            }
                        )
                    }

                    composable(NavRoutes.BILL_SPLITTER_SCREEN) {
                        val selectedPersonId by viewModel.selectedPersonId.collectAsState()

                        BillSplitterScreen(
                            items = currentReceiptItems,
                            people = currentPeople,
                            selectedPersonId = selectedPersonId,
                            onUpdateItem = viewModel::updateReceiptItem,
                            onDeleteItem = viewModel::deleteReceiptItem,
                            // --- Update this callback ---
                            onGoToTip = { totalsBeforeTip ->
                                viewModel.setTotalsBeforeTip(totalsBeforeTip) // Save pre-tip data
                                navController.navigate(NavRoutes.TIP_SCREEN) // Navigate to TipScreen
                            },
                            // ---
                            onNavigateBack = { navController.navigateUp() },
                            onAddPerson = viewModel::addPerson,
                            onEditPersonName = viewModel::editPersonName,
                            onDeletePerson = viewModel::deletePerson,
                            onSelectPerson = viewModel::selectPerson,
                            onToggleItem = viewModel::toggleItemForSelectedPerson
                        )
                    }
                    composable(NavRoutes.TIP_SCREEN) {
                        val totalsBeforeTip by viewModel.currentTotalsBeforeTip.collectAsState()
                        TipScreen(
                            totalsBeforeTip = totalsBeforeTip,
                            onNavigateBack = { navController.navigateUp() },
                            onGoToSummary = { calculatedFinalTotals ->
                                viewModel.setFinalTotals(calculatedFinalTotals) // Save final data to ViewModel
                                navController.navigate(NavRoutes.SUMMARY_SCREEN) // Navigate to Summary
                            }
                        )
                    }

                    // --- NEW: Add SummaryScreen composable ---
                    composable(NavRoutes.SUMMARY_SCREEN) {
                        SummaryScreen(
                            finalTotals = finalTotals, // Get totals from ViewModel
                            allItems = currentReceiptItems, // Get items from ViewModel
                            onNavigateBack = { navController.navigateUp() },
                            onSaveAndExit = {
                                viewModel.saveCurrentReceipt(finalTotals) // Save to DB
                                navController.popBackStack(NavRoutes.HOME_SCREEN, inclusive = false) // Go Home
                            }
                        )
                    }
                } // End NavHost

                // --- DIALOGS (Managed Here, Triggered by Callbacks) ---
                if (showOptionsDialog) {
                    AlertDialog(
                        onDismissRequest = { showOptionsDialog = false },
                        title = { Text("Scan a new receipt") },
                        text = { Text("How do you want to add your receipt?") },
                        confirmButton = {
                            TextButton(onClick = {
                                showOptionsDialog = false; showPermissionDialog = true // Show permission dialog next
                            }) { Text("Open Camera") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showOptionsDialog = false
                                galleryLauncher.launch( // Launch gallery
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }) { Text("From Gallery") }
                        }
                    )
                }

                if (showPermissionDialog) {
                    AlertDialog(
                        onDismissRequest = { showPermissionDialog = false },
                        title = { Text("Camera Permission Needed") },
                        text = { Text("We need camera access...") },
                        confirmButton = {
                            TextButton(onClick = {
                                showPermissionDialog = false
                                permissionLauncher.launch(Manifest.permission.CAMERA) // Request permission
                            }) { Text("Continue") }
                        },
                        dismissButton = { TextButton(onClick = { showPermissionDialog = false }) { Text("Cancel") } }
                    )
                }
            } // End Theme
        } // End setContent
    } // End onCreate


    // --- HELPER FUNCTIONS (INSIDE MainActivity) ---

    private fun getTempUri(context: Context): Uri {
        val file = File.createTempFile("temp_image", ".jpg", context.cacheDir)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    private fun launchCamera() {
        tempImageUri = getTempUri(this) // Use the stored context
        tempImageUri?.let { uri -> // Ensure uri is not null
            cameraLauncher.launch(uri)
        } ?: run {
            Toast.makeText(this, "Could not create temporary file for camera", Toast.LENGTH_SHORT).show()
        }
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
                                    if (priceRegex.matches(elementText.replace("$", ""))) {
                                        potentialPrices.add(Pair(elementText, elementFrame))
                                    }
                                }
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
                    viewModel.setCurrentItems(emptyList()) // Clear items on failure
                }
        } catch (e: Exception) {
            Log.e("TEXT_RECOGNITION", "Error processing input image:", e)
            viewModel.setCurrentItems(emptyList()) // Clear items on error
        }
    }

    // --- Coordinate-Based Parsing Logic (INSIDE MainActivity) ---
    private fun parseUsingCoordinates(
        elementsWithBoxes: List<Pair<String, Rect>>,
        potentialPrices: List<Pair<String, Rect>> // Note: potentialPrices isn't heavily used in *this* version, but keep parameter
    ): List<ReceiptItem> {

        // --- Helper Function ---
        fun verticallyOverlaps(rect1: Rect, rect2: Rect): Boolean {
            // Allow overlap if vertical centers are within half the combined height
            val overlapThreshold = (rect1.height() + rect2.height()) / 4 // Adjust threshold (e.g., /4, /3)
            return rect1.top < rect2.bottom - overlapThreshold && rect1.bottom > rect2.top + overlapThreshold
        }

        // --- Group elements into lines ---
        val lines = mutableListOf<MutableList<Pair<String, Rect>>>()
        val processedElements = mutableSetOf<Pair<String, Rect>>()
        elementsWithBoxes.forEach { element ->
            if (element !in processedElements) {
                val currentLine = mutableListOf(element)
                processedElements.add(element)
                elementsWithBoxes.forEach { otherElement ->
                    if (otherElement !in processedElements && verticallyOverlaps(element.second, otherElement.second)) {
                        currentLine.add(otherElement)
                        processedElements.add(otherElement)
                    }
                }
                currentLine.sortBy { it.second.left }
                lines.add(currentLine)
            }
        }
        lines.sortBy { it.firstOrNull()?.second?.top ?: 0 }
        Log.d("COORD_PARSE", "Found ${lines.size} potential lines.")
        // --- End Grouping ---

        // --- Process Lines ---
        val items = mutableListOf<ReceiptItem>()
        val priceRegex = "\\$?(\\d+\\.\\d{2})".toRegex()
        val ignoreKeywords = listOf(
            "SUBTOTAL", "TAX", "TOTAL", "CASH", "CHANGE", "ORDER",
            "TABLE", "CLOVER", "TIP", "THANK", "VISITING"
            // Add more common non-item words if needed
        )
        val dateRegex = "\\d{1,2}-\\w{3}-\\d{4}".toRegex() // Or more specific date/time regex

        lines.forEach { lineElements -> // Loop through each identified line
            val lineText = lineElements.joinToString(" ") { it.first }

            // Ignore line if it contains keywords or matches date pattern
            if (ignoreKeywords.any { lineText.uppercase().contains(it) } || dateRegex.containsMatchIn(lineText)) {
                Log.d("COORD_PARSE", "Ignoring line (keyword/date): $lineText")
                return@forEach // Skip to next line
            }

            // Separate words and prices found *on this line*
            val words = mutableListOf<Pair<String, Rect>>()
            val pricesOnLine = mutableListOf<Pair<Double, Rect>>()
            lineElements.forEach { (text, rect) ->
                val priceMatch = priceRegex.matchEntire(text.replace("$", ""))
                if (priceMatch != null) {
                    priceMatch.groupValues.getOrNull(1)?.toDoubleOrNull()?.let { priceValue ->
                        pricesOnLine.add(Pair(priceValue, rect))
                    }
                } else {
                    if (text.length > 1 || text.any { it.isLetter() }) { // Basic filter for potential words
                        words.add(Pair(text, rect))
                    }
                }
            }

            // --- Association Logic ---
            if (words.isNotEmpty() && pricesOnLine.isNotEmpty()) {
                // Calculate vertical center and height of the name words group
                val nameTop = words.minOfOrNull { it.second.top } ?: 0
                val nameBottom = words.maxOfOrNull { it.second.bottom } ?: 0
                val nameCenterY = (nameTop + nameBottom) / 2
                val nameHeight = nameBottom - nameTop

                // Find the rightmost edge of the name words
                val nameRightEdge = words.maxOfOrNull { it.second.right } ?: 0

                // Consider only prices starting to the right of the name
                val candidatePrices = pricesOnLine.filter { it.second.left > nameRightEdge }

                if (candidatePrices.isNotEmpty()) {
                    // Find the best price based on vertical alignment and horizontal proximity
                    val bestPricePair = candidatePrices.minByOrNull { (priceValue, priceRect) ->
                        val priceCenterY = priceRect.centerY()
                        val verticalDistance = kotlin.math.abs(priceCenterY - nameCenterY)
                        val verticalPenalty = if (priceRect.bottom < nameTop || priceRect.top > nameBottom) nameHeight * 2 else verticalDistance
                        val horizontalDistance = priceRect.left - nameRightEdge
                        (verticalPenalty * 1.5) + horizontalDistance // Score prioritizes vertical alignment

                    }

                    if (bestPricePair != null) {
                        // Use all words on the line before the best price as the name
                        val potentialNameWords = words.filter { it.second.right < bestPricePair.second.left }
                        var potentialName = potentialNameWords.joinToString(" ") { it.first }

                        // Cleaning
                        potentialName = potentialName.replaceFirst("^\\d+\\s+".toRegex(), "").trim()
                        potentialName = potentialName.replaceFirst("\\s+[^A-Za-z0-9]+$".toRegex(), "").trim()

                        if (potentialName.isNotEmpty()) {
                            Log.d("COORD_PARSE", "   -> Associated: '$potentialName' with price ${bestPricePair.first}")
                            items.add(ReceiptItem(name = potentialName, price = bestPricePair.first))
                        } else {
                            Log.d("COORD_PARSE", "   -> Found price ${bestPricePair.first} but name empty after cleaning on line: $lineText")
                        }
                    } else {
                        Log.d("COORD_PARSE", "   -> No suitable aligned/close price found to right on line: $lineText")
                    }
                } else {
                    Log.d("COORD_PARSE", "   -> No prices found to right of words on line: $lineText")
                }
            } else {
                // Log.d("COORD_PARSE", "   -> No clear words/prices combo found on line: $lineText")
            }
        } // --- End lines.forEach ---

        Log.d("COORD_PARSE", "Finished parsing. Found ${items.size} items.")
        return items
    } // --- End parseUsingCoordinates ---


    // --- Calculation Logic (INSIDE MainActivity) ---
    // (Could be moved to ViewModel or UseCase later)
    fun calculateTotalsBeforeTip(
        people: List<Person>,
        items: List<ReceiptItem>,
        taxStr: String
    ): List<PersonTotal> {

        val totalTax = taxStr.toDoubleOrNull() ?: 0.0

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
        // --- END OF MISSING LOGIC ---

        // 2. Calculate the total subtotal that has been accounted for
        val assignedSubtotal = personSubtotals.values.sum()
        if (assignedSubtotal == 0.0) {
            return emptyList() // Return empty if no items were assigned
        }

        // 3. Calculate and return the final list (without tip)
        val finalTotals = mutableListOf<PersonTotal>()
        personSubtotals.forEach { (person, subtotal) ->
            // Find what percentage of the subtotal this person is responsible for
            val percentageOfSubtotal = subtotal / assignedSubtotal

            // Calculate their share of the tax
            val taxShare = totalTax * percentageOfSubtotal

            // Calculate their final total (before tip)
            val totalOwed = subtotal + taxShare

            finalTotals.add(
                PersonTotal(
                    person = person,
                    subtotal = subtotal,
                    taxShare = taxShare,
                    tipShare = 0.0, // Tip is 0 for now
                    totalOwed = totalOwed
                )
            )
        }

        return finalTotals
    }
} // <-- FINAL closing brace for MainActivity
