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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.receiptsplitter.data.*
import com.example.receiptsplitter.screens.*
import com.example.receiptsplitter.ui.theme.ReceiptSplitterTheme
import com.example.receiptsplitter.viewmodel.ReceiptViewModel
import com.example.receiptsplitter.viewmodel.ReceiptViewModelFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import com.google.mlkit.vision.text.Text
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
                // --- Collect state here ---
                val savedReceiptsList by viewModel.savedReceipts.collectAsState()
                val currentReceiptItems by viewModel.receiptItems.collectAsState()
                val currentPeople by viewModel.currentPeople.collectAsState()
                val previewImageUri by viewModel.previewImageUri.collectAsState()
                val totalsBeforeTip by viewModel.currentTotalsBeforeTip.collectAsState()
                val finalTotals by viewModel.finalTotals.collectAsState()

                // --- Dialog state here ---
                var showOptionsDialog by remember { mutableStateOf(false) }
                var showPermissionDialog by remember { mutableStateOf(false) }

                // 1. The outer Surface provides the pastel brown background
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // --- NavHost is the DIRECT child of Surface ---
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.HOME_SCREEN,
                        modifier = Modifier.fillMaxSize()
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
                                    viewModel.setFinalTotals(receipt.personTotals)
                                    viewModel.setCurrentItems(receipt.items)
                                    viewModel.setPeopleForCurrentSplit(receipt.personTotals.map { it.person })
                                    navController.navigate(NavRoutes.buildSummaryRoute(isViewOnly = true))
                                },
                                onUpdateReceiptName = { receipt, newName ->
                                    viewModel.updateReceiptName(receipt.id, newName)
                                }
                            )
                        } // <-- End HomeScreen composable

                        // --- Setup Screen ---
                        composable(NavRoutes.SETUP_SCREEN) {
                            SetupScreen(
                                people = currentPeople,
                                previewImageUri = previewImageUri,
                                onNavigateBack = { navController.navigateUp() },
                                onScanReceiptClick = { showOptionsDialog = true },
                                onProceedToSplit = { uri ->
                                    if (uri == null) {
                                        Toast.makeText(this@MainActivity, "Please scan a receipt first", Toast.LENGTH_SHORT).show()
                                    } else {
                                        processImage(uri)
                                        navController.navigate(NavRoutes.BILL_SPLITTER_SCREEN)
                                    }
                                },
                                onAddPerson = { name -> viewModel.addPerson(name) },
                                onEditPerson = { person, name -> viewModel.editPersonName(person, name) },
                                onDeletePerson = { person -> viewModel.deletePerson(person) }
                            )
                        } // <-- End SetupScreen composable

                        // --- Bill Splitter Screen ---
                        composable(NavRoutes.BILL_SPLITTER_SCREEN) {
                            BillSplitterScreen(
                                viewModel = viewModel,
                                items = currentReceiptItems,
                                people = currentPeople,
                                onUpdateItem = viewModel::updateReceiptItem,
                                onDeleteItem = viewModel::deleteReceiptItem,
                                onGoToTip = { totalsBeforeTip ->
                                    viewModel.setTotalsBeforeTip(totalsBeforeTip)
                                    navController.navigate(NavRoutes.TIP_SCREEN)
                                },
                                onNavigateBack = { navController.navigateUp() }
                            )
                        } // <-- End BillSplitterScreen composable

                        // --- Tip Screen ---
                        composable(NavRoutes.TIP_SCREEN) {
                            TipScreen(
                                totalsBeforeTip = totalsBeforeTip,
                                onNavigateBack = { navController.navigateUp() },
                                onGoToSummary = { calculatedFinalTotals ->
                                    viewModel.setFinalTotals(calculatedFinalTotals)
                                    navController.navigate(NavRoutes.buildSummaryRoute(isViewOnly = false))
                                }
                            )
                        } // <-- End TipScreen composable

                        // --- Summary Screen ---
                        composable(
                            route = NavRoutes.SUMMARY_SCREEN,
                            arguments = listOf(navArgument(NavRoutes.SUMMARY_ARG_VIEW_ONLY) { type = NavType.BoolType })
                        ) { backStackEntry ->
                            val isViewOnly = backStackEntry.arguments?.getBoolean(NavRoutes.SUMMARY_ARG_VIEW_ONLY) ?: false
                            SummaryScreen(
                                finalTotals = finalTotals,
                                allItems = currentReceiptItems,
                                onNavigateBack = { navController.navigateUp() },
                                onSaveReceipt = { description ->
                                    viewModel.saveCurrentReceipt(finalTotals, description)
                                    navController.popBackStack(NavRoutes.HOME_SCREEN, inclusive = false)
                                },
                                onNewBill = {
                                    viewModel.clearCurrentItems()
                                    navController.navigate(NavRoutes.SETUP_SCREEN) {
                                        popUpTo(NavRoutes.HOME_SCREEN)
                                    }
                                },
                                isViewOnly = isViewOnly
                            )
                        } // <-- End SummaryScreen composable
                    } // <-- End NavHost

                    // --- Dialogs (float on top of everything) ---
                    if (showOptionsDialog) {
                        AlertDialog(
                            onDismissRequest = { showOptionsDialog = false },
                            title = { Text("Scan a new receipt") },
                            text = { Text("How do you want to add your receipt?") },
                            confirmButton = { TextButton(onClick = { showOptionsDialog = false; showPermissionDialog = true }) { Text("Open Camera") } },
                            dismissButton = { TextButton(onClick = { showOptionsDialog = false; galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("From Gallery") } },
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (showPermissionDialog) {
                        AlertDialog(
                            onDismissRequest = { showPermissionDialog = false },
                            title = { Text("Camera Permission Needed") },
                            text = { Text("We need camera access...") },
                            confirmButton = { TextButton(onClick = { showPermissionDialog = false; permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Allow") } },
                            dismissButton = { TextButton(onClick = { showPermissionDialog = false }) { Text("Cancel") } },
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } // <-- End Surface
            } // End Theme
        } // End setContent
    } // End onCreate
//Yknow, i setup these "ends" and im still lost editing and adding stuff in this. wtf. i miss my vs plugins.

    // --- HELPER FUNCTIONS (INSIDE MainActivity) ---
    private fun getTempUri(context: Context): Uri {
        val file = File.createTempFile("temp_image", ".jpg", context.cacheDir)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // This must match your AndroidManifest
            file
        )
    }
    // --- c a m e r a ---
    private fun launchCamera() {
        tempImageUri = getTempUri(this) // Create a new temp URI
        tempImageUri?.let { uri -> // Ensure uri is not null
            cameraLauncher.launch(uri)
        } ?: run {
            Toast.makeText(this, "Could not create temporary file for camera", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * This function has aLOT of comments bc I was confused implementing it lol
     * This function is called when the user selects an image from the gallery or camera.
     * It uses ML Kit to recognize text and then sends that text to the custom parser.
     */
    private fun processImage(uri: Uri) {
        // Get an instance of the ML Kit text recognizer with default options.
        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            // Create an 'InputImage' from the URI (file path) of the image the user selected.
            // 'this' refers to the MainActivity context.
            val inputImage = InputImage.fromFilePath(this, uri)

            // Start the asynchronous process of analyzing the image for text.
            textRecognizer.process(inputImage)
                // This block runs only if the text recognition is SUCCESSFUL.
                // 'visionText' is the object returned by ML Kit, containing all recognized text.
                .addOnSuccessListener { visionText -> // ML Kit processing successful

                    // 1. Get the lines, already structured by ML Kit.
                    // ML Kit groups text into Blocks -> Lines -> Elements.
                    // We flatten this structure into a single List<Text.Line> for easier parsing.
                    val allLines = visionText.textBlocks.flatMap { it.lines }

                    // This loop prints every line and its bounding box coordinates to the Logcat.
                    Log.d("NEW_PARSE", "--- STARTING CALIBRATION LOG ---")
                    allLines.forEach { line ->
                        val box = line.boundingBox
                        Log.d("NEW_PARSE", "CALIBRATE: Line: '${line.text}',  Box: $box")
                    }
                    Log.d("NEW_PARSE", "--- ENDING CALIBRATION LOG ---")
                    // --- END OF LOGS ---


                    // 2. Pass these structured lines to our new parser
                    // Call our custom parser function which contains all the complex logic.
                    // It returns a 'Pair' containing:
                    //   _1_ (val parsedItems): The list of found ReceiptItems.
                    //   _2_ (val foundTax): The tax string (e.g., "6.58") or null if not found.
                    val (parsedItems, foundTax) = parseReceiptWithHybridLogic(allLines)

                    // 3. --- Update ViewModel (Directly) ---
                    // Update the ViewModel's StateFlow with the new list of items.
                    // This will trigger any Composable observing 'viewModel.receiptItems' (BillSplitterScreen) to recompose.
                    viewModel.setCurrentItems(parsedItems)

                    // --- Set the tax in the ViewModel ---
                    // If our parser found a non-null tax value...
                    if (foundTax != null) {
                        // ...update the ViewModel's separate 'totalTax' StateFlow.
                        // This will trigger the TextField in BillSplitterScreen to pre-fill.
                        viewModel.setTotalTax(foundTax)
                        Log.d("NEW_PARSE", "Found Tax: $foundTax")
                    }
                }
                // This block (lambda) runs if the ML Kit process FAILS (e.g., blurry image, no text).
                .addOnFailureListener { e ->
                    Log.e("TEXT_RECOGNITION", "Failed:", e)
                    // Clear the items list on failure so the user doesn't see old items from a previous scan.
                    viewModel.setCurrentItems(emptyList()) // Clear items on failure
                }
            // This 'catch' block handles any other errors, like if the 'InputImage.fromFilePath' fails
            // (e.g., the file URI is invalid or inaccessible).
        } catch (e: Exception) {
            Log.e("TEXT_RECOGNITION", "Error processing input image:", e)
            viewModel.setCurrentItems(emptyList()) // Clear items on error
        }
    }

    // --- NEW CONSTANTS  ---
    // Regex to find a price. Handles commas and dots.
    // Added more flexible international currency
    private val PRICE_REGEX = "[S\\$\\¥\\₩\\₱]?([\\d\\.,]+\\d)".toRegex()

    // Regex to find a quantity at the start of a line.
    private val QTY_REGEX = "^\\d+\\S*".toRegex()

    // Keywords to ignore
    private val IGNORE_KEYWORDS = setOf(
        "SUBTOTAL", /* "TAX", */ "TOTAL", "CASH", "CHANGE", "ORDER", // <-- REMOVED "TAX"
        "TABLE", "CLOVER", "TIP", "THANK", "VISITING", "JUSTIN",
        "DINE IN", "UNPAID", "SUGGESTIONS", "TERMINAL", "GUESTS",
        "DESCRIPTION", "CLERK", "INV#", "PRICE", "PM", "AM",
        "15%", "18%", "20%" // <-- ADDED THIS to ignore tip suggestions
    )

    /**
     * Checks if two bounding boxes are vertically aligned on the same "row".
     * (For side-by-side items)
     */
    private fun verticallyOverlaps(box1: Rect, box2: Rect): Boolean {
        val center1 = box1.centerY()
        val center2 = box2.centerY()
        val maxHeight = maxOf(box1.height(), box2.height())
        // They overlap if their centers are closer than half the max height
        return abs(center1 - center2) < (maxHeight / 2)
    }

    /**
     * Checks if box2 is reasonably "just below" box1 AND horizontally overlaps.
     * (For stacked items)
     */
    private fun isVerticallyBelow(itemBox: Rect, priceBox: Rect): Boolean {
        // --- NEW Rule: Must horizontally overlap ---
        val horizontalOverlap = itemBox.left < priceBox.right && itemBox.right > priceBox.left
        if (!horizontalOverlap) return false
        // --- End NEW Rule ---

        // Price must start at or below the item's top
        if (priceBox.top < itemBox.top) return false

        // Calculate vertical distance between the *centers*
        val verticalDistance = priceBox.centerY() - itemBox.centerY()

        // Must be a positive distance (price is below)
        if (verticalDistance <= 0) return false

        // Price must be "close" vertically (e.g., within 2 line heights)
        val maxHeight = maxOf(itemBox.height(), priceBox.height())

        // --- TWEAK: Reduced vertical distance to 2x height ---
        return verticalDistance < (maxHeight * 2)
    }

    // --- THIS IS THE NEW INTERNATIONAL VERSION ---
    /**
     * Extracts a clean price value (Double) from a text string.
     * This function is much smarter to handle international currency formats.
     */
    private fun extractPrice(text: String): Double? {
        // 1. Find the first part of the text that looks like a price.
        val priceMatch = PRICE_REGEX.find(text) ?: return null // e.g., finds "$15,000.00" or "¥1500"

        // 2. Get the number part (group 1) from the regex.
        // e.g., "15,000.00" or "1500" or "15.000,00"
        val numberString = priceMatch.groupValues[1]

        // --- Heuristic to handle different currency formats ---

        // 3. Check if this number has a "cents" part (e.g., .00 or ,00).
        // This regex looks for a separator (dot or comma) followed by exactly 2 digits *at the end*.
        val decimalRegex = "([\\.,])(\\d{2})$".toRegex()
        val decimalMatch = decimalRegex.find(numberString)

        if (decimalMatch != null) {
            // Case 1: FOUND DECIMALS (e.g., "15.000,00" or "15.00")
            // USD, EUR, etc.

            // Get the separator (e.g., ",")
            val decimalSeparator = decimalMatch.groupValues[1]
            // Get the part *before* the separator (e.g., "15.000")
            val integerPart = numberString.substringBeforeLast(decimalSeparator)
            // Get the "cents" (e.g., "00")
            val decimalPart = decimalMatch.groupValues[2]

            // Remove all *other* separators from the integer part (e.g., "15.000" -> "15000")
            val cleanIntegerPart = integerPart.replace(".", "").replace(",", "")

            // Re-assemble as a standard string (e.g., "15000.00") and convert to Double.
            return "$cleanIntegerPart.$decimalPart".toDoubleOrNull()

        } else {
            // Case 2: NO DECIMALS (e.g., "15,000" or "1500")
            // JPY (Yen) or KRW (Won).

            // Just remove *all* separators (e.g., "15,000" -> "15000") and convert.
            val cleanNumber = numberString.replace(".", "").replace(",", "")
            return cleanNumber.toDoubleOrNull()
        }
    }


    /**
     * New parser that uses spatial logic (X/Y coordinates) to find pairs.
     * Returns a Pair of the Item List and the Tax String
     */
    private fun parseReceiptWithHybridLogic(
        lines: List<Text.Line>
    ): Pair<List<ReceiptItem>, String?> { // <-- UPDATED RETURN TYPE

        val parsedItems = mutableListOf<ReceiptItem>()
        val potentialItemsWithQty = mutableListOf<Text.Line>()
        val potentialItemNames = mutableListOf<Text.Line>() // For names without quantities
        val potentialPrices = mutableListOf<Text.Line>()

        var taxLabelLine: Text.Line? = null // <-- NEW: Store the "TAX:" line

        // --- 1. Classify all lines first ---
        for (line in lines) {
            val lineText = line.text
            val lineTextUpper = lineText.uppercase()
            val lineTextTrim = lineText.trim()

            // --- Clean text first ---
            val priceValue = extractPrice(lineText)
            val hasQty = QTY_REGEX.containsMatchIn(lineText)
            val priceMatch = PRICE_REGEX.find(lineText)

            // Check if it's an ignorable line
            if (IGNORE_KEYWORDS.any { lineTextUpper.contains(it) } || lineText.length < 3) {
                Log.d("NEW_PARSE", "Ignoring keyword/junk line: $lineText")
                continue
            }

            // --- Check for TAX line ---
            if (lineTextUpper.contains("TAX")) {
                taxLabelLine = line // <-- NEW: Save the line itself
                Log.d("TAX_PARSE", "Found TAX label line: '$lineText'")
                continue // Don't process this line as an item
            }

            // Case 1: Price AND Qty found on the same line (e.g., "1 Beef Tofu$17.99")
            if (priceValue != null && hasQty && priceMatch != null) {
                val name = lineText.substringBefore(priceMatch.value)
                    .replace(QTY_REGEX, "").trim()

                if (name.isNotEmpty()) {
                    parsedItems.add(ReceiptItem(name = name, price = priceValue))
                    Log.d("NEW_PARSE", "Found Same-Line Item: '$name', Price: $priceValue")
                }
            }
            // Case 2: Price-Only line (e.g., "$16.99" or "$32.49")
            // We check if the *entire line* is basically just a price.
            else if (priceValue != null && priceMatch?.value?.length ?: 0 >= lineTextTrim.length - 2) {
                potentialPrices.add(line)
                Log.d("NEW_PARSE", "Found Potential Price: '$lineText'")
            }
            // Case 3: Item line with Qty (e.g., "1 Seafood Pancake")
            else if (hasQty && priceValue == null) {
                potentialItemsWithQty.add(line)
                Log.d("NEW_PARSE", "Found Potential Item (Qty): '$lineText'")
            }
            // Case 4: Item Name line without Qty (e.g., "(D) Galbi Combo" or "Toki Classic")
            //  --- THIS IS THE FIX for modifiers ---
            else if (priceValue == null && !lineTextTrim.startsWith("*") && !lineTextTrim.startsWith("-")) { // <-- THE BUG WAS HERE (was priceValue != null)
                potentialItemNames.add(line)
                Log.d("NEW_PARSE", "Found Potential Item Name: '$lineText'")
            }
            else {
                Log.d("NEW_PARSE", "Ignoring unclassified/modifier line: $lineText")
            }
        }

        Log.d("NEW_PARSE", "Found ${potentialItemsWithQty.size} Qty items, ${potentialItemNames.size} Name items, and ${potentialPrices.size} Price lines.")

        // --- 2. Find and EXTRACT Tax Price FIRST ---
        var foundTax: String? = null
        val taxBox = taxLabelLine?.boundingBox
        val pricesToRemove = mutableListOf<Text.Line>()

        if (taxBox != null) {
            val taxPriceLine = potentialPrices.find { priceLine ->
                val priceBox = priceLine.boundingBox
                priceBox != null && verticallyOverlaps(taxBox, priceBox) && priceBox.left > taxBox.right
            }

            if (taxPriceLine != null) {
                val taxPrice = extractPrice(taxPriceLine.text)
                if (taxPrice != null) {
                    foundTax = taxPrice.toString()
                    pricesToRemove.add(taxPriceLine) // Mark for removal
                    Log.d("TAX_PARSE", "SUCCESS: Matched 'TAX:' to price '${taxPriceLine.text}'")
                }
            } else {
                Log.d("TAX_PARSE", "FAILED: Found 'TAX:' label but no side-by-side price.")
            }
        }

        // --- CRITICAL: Remove tax price so it can't be matched to an item ---
        potentialPrices.removeAll(pricesToRemove)
        // --- END TAX LOGIC ---


        // --- 3. Spatially "Join" the remaining items and prices ---
        val remainingPrices = potentialPrices.toMutableList() // Use the cleaned list
        val processedNames = mutableSetOf<Text.Line>()

        // Combine all potential items into one list, Qty items first
        val allPotentialItems = potentialItemsWithQty + potentialItemNames

        for (itemLine in allPotentialItems) {
            val itemBox = itemLine.boundingBox ?: continue
            var name = itemLine.text.replace(QTY_REGEX, "").trim()

            // Skip if this is a "name" line we already merged
            if (itemLine in processedNames) continue

            // --- 3a. Find any side-by-side "Name" lines (for Galbi Combo) ---
            val sideByName = potentialItemNames
                .filter { nameLine -> nameLine !in processedNames }
                .find { nameLine ->
                    val nameBox = nameLine.boundingBox
                    nameBox != null && verticallyOverlaps(itemBox, nameBox) && nameBox.left > itemBox.right
                }

            if (sideByName != null) {
                name = "$name ${sideByName.text}" // Merge names (e.g., "12H|" + "(D) Galbi Combo")
                processedNames.add(sideByName)
                Log.d("NEW_PARSE", "Merged item name: '$name'")
            }

            // --- 3b. Find the best matching price (THE "IF/ELSE" LOGIC) ---
            val itemBoxWithMergedName = sideByName?.boundingBox ?: itemBox

            // --- Logic 1: Check for Side-by-Side match FIRST (Ramen logic) ---
            val sideBySidePrice = remainingPrices
                .filter { priceLine ->
                    val priceBox = priceLine.boundingBox
                    priceBox != null && verticallyOverlaps(itemBoxWithMergedName, priceBox) && priceBox.left > itemBoxWithMergedName.right
                }
                .minByOrNull { priceLine ->
                    // Find closest one to the right
                    val priceBox = priceLine.boundingBox!!
                    abs(priceBox.left - itemBoxWithMergedName.right) // Closest by horizontal distance
                }

            if (sideBySidePrice != null) {
                // Found a Ramen-style match. Use it.
                val price = extractPrice(sideBySidePrice.text)
                if (price != null) {
                    parsedItems.add(ReceiptItem(name = name, price = price))
                    Log.d("NEW_PARSE", "==> SUCCESS (Side): Matched '$name' with '${sideBySidePrice.text}'")
                    remainingPrices.remove(sideBySidePrice)
                }
            } else {
                // --- Logic 2: No side match. Check for Stacked match (BCD logic) ---
                val stackedPrice = remainingPrices
                    .filter { priceLine ->
                        val priceBox = priceLine.boundingBox
                        priceBox != null && isVerticallyBelow(itemBoxWithMergedName, priceBox)
                    }
                    .minByOrNull { priceLine ->
                        // Find closest one *below*
                        val priceBox = priceLine.boundingBox!!
                        abs(priceBox.top - itemBoxWithMergedName.bottom) // Closest by vertical distance
                    }

                if (stackedPrice != null) {
                    val price = extractPrice(stackedPrice.text)
                    if (price != null) {
                        parsedItems.add(ReceiptItem(name = name, price = price))
                        Log.d("NEW_PARSE", "==> SUCCESS (Stack): Matched '$name' with '${stackedPrice.text}'")
                        remainingPrices.remove(stackedPrice)
                    } else {
                        Log.d("NEW_PARSE", "==> FAILED (Stack): No price found for '$name'")
                    }
                } else {
                    Log.d("NEW_PARSE", "==> FAILED (None): No side-by-side or stacked price found for '$name'")
                }
            }
        }

        Log.d("NEW_PARSE", "Finished parsing. Found ${parsedItems.size} items.")
        return Pair(parsedItems, foundTax) // <-- UPDATED RETURN TYPE
    }
    // --- End new parsing logic ---

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
