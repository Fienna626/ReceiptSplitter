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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.receiptsplitter.data.*
import com.example.receiptsplitter.screens.*
import com.example.receiptsplitter.ui.theme.ReceiptSplitterTheme
import com.example.receiptsplitter.viewmodel.ReceiptViewModel
import com.example.receiptsplitter.viewmodel.ReceiptViewModelFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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

    private var tempImageUri: Uri? = null

    // --- LAUNCHERS ---
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setPreviewImageUri(uri)
        }
    }
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempImageUri?.let { viewModel.setPreviewImageUri(it) }
        }
    }
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tempImageUri = getTempUri(this)

        setContent {
            ReceiptSplitterTheme {
                val navController = rememberNavController()

                // --- Collect All State from ViewModel ---
                val savedReceiptsList by viewModel.savedReceipts.collectAsState()
                val currentReceiptItems by viewModel.receiptItems.collectAsState()
                val currentPeople by viewModel.currentPeople.collectAsState()
                val previewImageUri by viewModel.previewImageUri.collectAsState()
                val totalsBeforeTip by viewModel.currentTotalsBeforeTip.collectAsState()
                val finalTotals by viewModel.finalTotals.collectAsState()

                // Dialog State
                var showOptionsDialog by remember { mutableStateOf(false) }
                var showPermissionDialog by remember { mutableStateOf(false) }

                // --- Navigation Graph ---
                NavHost(
                    navController = navController,
                    startDestination = NavRoutes.HOME_SCREEN
                ) {
                    // --- Home Screen ---
                    composable(NavRoutes.HOME_SCREEN) {
                        HomeScreen(
                            savedReceipts = savedReceiptsList,
                            onNavigateToSplitter = {
                                viewModel.clearCurrentItems()
                                navController.navigate(NavRoutes.SETUP_SCREEN)
                            },
                            onDeleteReceipt = viewModel::deleteSavedReceipt,
                            onReceiptClick = { receipt ->
                                // Pass data to ViewModel for detail view
                                viewModel.setFinalTotals(receipt.personTotals)
                                viewModel.setCurrentItems(emptyList()) // Or load items if saved
                                navController.navigate(NavRoutes.SUMMARY_SCREEN)
                            }
                        )
                    }

                    // --- Setup Screen ---
                    composable(NavRoutes.SETUP_SCREEN) {
                        SetupScreen(
                            people = currentPeople,
                            previewImageUri = previewImageUri,
                            onNavigateBack = { navController.navigateUp() }, // <-- This is here, correctly.
                            onScanReceiptClick = { showOptionsDialog = true },
                            onProceedToSplit = { uri ->
                                if (uri != null) {
                                    // --- URI IS GOOD, DO THE WORK ---
                                    processImage(uri)
                                    navController.navigate(NavRoutes.BILL_SPLITTER_SCREEN)
                                } else {
                                    // --- URI IS NULL, SHOW THE TOAST ---
                                    Toast.makeText(this@MainActivity, "Please scan a receipt first", Toast.LENGTH_SHORT).show()
                                    // No return needed
                                }
                            },
                            onAddPerson = { name -> viewModel.addPerson(name) },
                            onEditPerson = { person, name -> viewModel.editPersonName(person, name) },
                            onDeletePerson = { person -> viewModel.deletePerson(person) }
                        )
                    }

                    // --- Bill Splitter Screen ---
                    composable(NavRoutes.BILL_SPLITTER_SCREEN) {
                        BillSplitterScreen(
                            items = currentReceiptItems,
                            onUpdateItem = viewModel::updateReceiptItem,
                            onDeleteItem = viewModel::deleteReceiptItem,
                            onGoToTip = { totalsBeforeTip ->
                                viewModel.setTotalsBeforeTip(totalsBeforeTip)
                                navController.navigate(NavRoutes.TIP_SCREEN)
                            },
                            onNavigateBack = { navController.navigateUp() }
                        )
                    }

                    // --- Tip Screen ---
                    composable(NavRoutes.TIP_SCREEN) {
                        TipScreen(
                            totalsBeforeTip = totalsBeforeTip,
                            onNavigateBack = { navController.navigateUp() },
                            onGoToSummary = { calculatedFinalTotals ->
                                viewModel.setFinalTotals(calculatedFinalTotals)
                                navController.navigate(NavRoutes.SUMMARY_SCREEN)
                            }
                        )
                    }

                    // --- Summary Screen ---
                    composable(NavRoutes.SUMMARY_SCREEN) {
                        SummaryScreen(
                            finalTotals = finalTotals,
                            allItems = currentReceiptItems, // Show items if needed
                            onNavigateBack = { navController.navigateUp() },
                            onSaveAndExit = {
                                viewModel.saveCurrentReceipt(finalTotals)
                                navController.popBackStack(NavRoutes.HOME_SCREEN, inclusive = false)
                            }
                        )
                    }
                } // End NavHost

                // --- Dialogs (Managed Here) ---
                if (showOptionsDialog) {
                    AlertDialog(
                        onDismissRequest = { showOptionsDialog = false },
                        title = { Text("Scan a new receipt") },
                        text = { Text("How do you want to add your receipt?") },
                        confirmButton = { TextButton(onClick = { showOptionsDialog = false; showPermissionDialog = true }) { Text("Open Camera") } },
                        dismissButton = { TextButton(onClick = { showOptionsDialog = false; galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("From Gallery") } }
                    )
                }
                if (showPermissionDialog) {
                    AlertDialog(
                        onDismissRequest = { showPermissionDialog = false },
                        title = { Text("Camera Permission Needed") },
                        text = { Text("We need camera access...") },
                        confirmButton = { TextButton(onClick = { showPermissionDialog = false; permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Continue") } },
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
            "${context.packageName}.provider", // This must match your AndroidManifest
            file
        )
    }

    private fun launchCamera() {
        tempImageUri = getTempUri(this) // Create a new temp URI
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

                    // 3. --- Update ViewModel (Directly) ---
                    viewModel.setCurrentItems(parsedItems)
                }
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
        potentialPrices: List<Pair<String, Rect>>
    ): List<ReceiptItem> {

        // --- Helper Function ---
        fun verticallyOverlaps(rect1: Rect, rect2: Rect): Boolean {
            val overlapThreshold = (rect1.height() + rect2.height()) / 4
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
        )
        val dateRegex = "\\d{1,2}-\\w{3}-\\d{4}".toRegex()

        lines.forEach { lineElements ->
            val lineText = lineElements.joinToString(" ") { it.first }

            if (ignoreKeywords.any { lineText.uppercase().contains(it) } || dateRegex.containsMatchIn(lineText)) {
                Log.d("COORD_PARSE", "Ignoring line (keyword/date): $lineText")
                return@forEach
            }

            val words = mutableListOf<Pair<String, Rect>>()
            val pricesOnLine = mutableListOf<Pair<Double, Rect>>()
            lineElements.forEach { (text, rect) ->
                val priceMatch = priceRegex.matchEntire(text.replace("$", ""))
                if (priceMatch != null) {
                    priceMatch.groupValues.getOrNull(1)?.toDoubleOrNull()?.let { priceValue ->
                        pricesOnLine.add(Pair(priceValue, rect))
                    }
                } else {
                    if (text.length > 1 || text.any { it.isLetter() }) {
                        words.add(Pair(text, rect))
                    }
                }
            }

            // --- Association Logic ---
            if (words.isNotEmpty() && pricesOnLine.isNotEmpty()) {
                val nameTop = words.minOfOrNull { it.second.top } ?: 0
                val nameBottom = words.maxOfOrNull { it.second.bottom } ?: 0
                val nameCenterY = (nameTop + nameBottom) / 2
                val nameHeight = nameBottom - nameTop
                val nameRightEdge = words.maxOfOrNull { it.second.right } ?: 0
                val candidatePrices = pricesOnLine.filter { it.second.left > nameRightEdge }

                if (candidatePrices.isNotEmpty()) {
                    val bestPricePair = candidatePrices.minByOrNull { (priceValue, priceRect) ->
                        val priceCenterY = priceRect.centerY()
                        val verticalDistance = kotlin.math.abs(priceCenterY - nameCenterY)
                        val verticalPenalty = if (priceRect.bottom < nameTop || priceRect.top > nameBottom) nameHeight * 2 else verticalDistance
                        val horizontalDistance = priceRect.left - nameRightEdge
                        (verticalPenalty * 1.5) + horizontalDistance
                    }

                    if (bestPricePair != null) {
                        val potentialNameWords = words.filter { it.second.right < bestPricePair.second.left }
                        var potentialName = potentialNameWords.joinToString(" ") { it.first }
                        potentialName = potentialName.replaceFirst("^\\d+\\s+".toRegex(), "").trim()
                        potentialName = potentialName.replaceFirst("\\s+[^A-Za-z0-9]+$".toRegex(), "").trim()

                        if (potentialName.isNotEmpty()) {
                            Log.d("COORD_PARSE", "   -> Associated: '$potentialName' with price ${bestPricePair.first}")
                            items.add(ReceiptItem(name = potentialName, price = bestPricePair.first))
                        } else {
                            Log.d("COORD_PARSE", "   -> Found price ${bestPricePair.first} but name empty on line: $lineText")
                        }
                    } else {
                        Log.d("COORD_PARSE", "   -> No suitable price found on line: $lineText")
                    }
                } else {
                    Log.d("COORD_PARSE", "   -> No prices found to right of words on line: $lineText")
                }
            }
        } // --- End lines.forEach ---

        Log.d("COORD_PARSE", "Finished parsing. Found ${items.size} items.")
        return items
    } // --- End parseUsingCoordinates ---

    // --- Calculation Logic (INSIDE MainActivity) ---
    fun calculateTotalsBeforeTip(
        people: List<Person>,
        items: List<ReceiptItem>,
        taxStr: String
    ): List<PersonTotal> {

        val totalTax = taxStr.toDoubleOrNull() ?: 0.0

        // 1. Calculate each person's subtotal
        val personSubtotals = mutableMapOf<Person, Double>()
        people.forEach { personSubtotals[it] = 0.0 } // Initialize all people to 0

        items.forEach { item ->
            val numPeopleForItem = item.assignedPeople.size
            if (numPeopleForItem > 0) {
                val pricePerPerson = item.price / numPeopleForItem
                item.assignedPeople.forEach { person ->
                    personSubtotals[person] = (personSubtotals.getOrDefault(person, 0.0)) + pricePerPerson
                }
            }
        }
        // --- End of subtotal calculation ---

        // 2. Calculate the total subtotal that was actually assigned
        val assignedSubtotal = personSubtotals.values.sum()
        if (assignedSubtotal == 0.0) {
            return emptyList() // Return empty if no items were assigned
        }

        // 3. Calculate and return the final list (without tip)
        val finalTotals = mutableListOf<PersonTotal>()
        personSubtotals.forEach { (person, subtotal) ->
            if (subtotal > 0) {
                val percentageOfSubtotal = subtotal / assignedSubtotal
                val taxShare = totalTax * percentageOfSubtotal
                val totalOwed = subtotal + taxShare

                finalTotals.add(
                    PersonTotal(
                        person = person,
                        subtotal = subtotal,
                        taxShare = taxShare,
                        tipShare = 0.0, // Tip is 0.0 for now
                        totalOwed = totalOwed
                    )
                )
            }
        }

        return finalTotals
    } // --- End calculateTotalsBeforeTip ---
} // End MainActivity