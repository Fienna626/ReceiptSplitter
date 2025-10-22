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


class MainActivity : ComponentActivity() {

    // --- STATE ---
    // This is the "source of truth" for our list of items.
    // It's now stored in the Activity, not in the composable.
    private var receiptItems = mutableStateOf(listOf<ReceiptItem>())

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
                        items = receiptItems.value,

                        // When the button is clicked, just show the dialog
                        onScanReceiptClick = { showOptionsDialog = true },

                        // Pass a function to update the item
                        onUpdateItem = { updatedItem ->
                            // Find the item in our list and replace it
                            val currentList = receiptItems.value.toMutableList()
                            val index = currentList.indexOfFirst { it.id == updatedItem.id }
                            if (index != -1) {
                                currentList[index] = updatedItem
                                receiptItems.value = currentList
                            }
                        },
                        onDeleteItem = { itemToDelete ->
                            val currentList = receiptItems.value.toMutableList()
                            currentList.remove(itemToDelete)
                            receiptItems.value = currentList
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
                    receiptItems.value = parseReceiptText(rawText)
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
        val regex = "(.+)[ ]+(\\d+\\.\\d{2})".toRegex()
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
                    val cleanName = if (name.contains("@")) {
                        name.split("@")[0].trim()
                    } else {
                        name.trim()
                    }

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


// --- DATA CLASSES (at the bottom of your file) ---

data class Person(
    val id: java.util.UUID = java.util.UUID.randomUUID(),
    var name: String
)

data class ReceiptItem(
    val id: java.util.UUID = java.util.UUID.randomUUID(),
    var name: String,
    var price: Double,
    // A list of people who are splitting this item
    val assignedPeople: MutableList<Person> = mutableListOf()
)


// --- COMPOSABLES (at the bottom of your file) ---

// This is our NEW main screen.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BillSplitterScreen(
    items: List<ReceiptItem>,
    onScanReceiptClick: () -> Unit,
    onUpdateItem: (ReceiptItem) -> Unit, // <-- to save edits
    onDeleteItem: (ReceiptItem) -> Unit // <-- to delete item

) {
    // --- State for People ---
    val people = remember { mutableStateOf(listOf(Person(name = "Person 1"))) }
    val (editingItem, setEditingItem) = remember { mutableStateOf<ReceiptItem?>(null) }
    // --- State for Tax and Tip ---
    var taxInput by remember { mutableStateOf(TextFieldValue("")) }
    var tipInput by remember { mutableStateOf(TextFieldValue("")) }

    // --- NEW: Change Tip to a percentage Float ---
    // Start with a default tip of 18%
    var tipPercent by remember { mutableStateOf(15f) }

    // --- Get the activity to call the math function ---
    val activity = (LocalActivity.current as? MainActivity)

    // --- Calculate totals whenever data changes ---
    // This 'remember' block will auto-recalculate when any of its "keys" change
    val calculatedTotals = remember(items, people.value, taxInput.text, tipInput.text) {
        activity?.calculateTotals(
            people.value,
            items,
            taxInput.text,
            tipInput.text
        ) ?: emptyList() // If activity is null, return an empty list
    }
    // --- Calculate the total subtotal (for the tip) ---
    val totalSubtotal = items.sumOf { it.price }

    // --- Calculate the tip dollar amount ---
    val calculatedTipAmount = totalSubtotal * (tipPercent / 100.0)

    // --- UPDATED: Update the remember block ---
    val calculatedTotals = remember(items, people.value, taxInput.text, tipPercent) { // <-- Use tipPercent
        activity?.calculateTotals(
            people.value,
            items,
            taxInput.text,
            calculatedTipAmount.toString() // <-- Pass the calculated dollar amount
        ) ?: emptyList()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. THE TOP BUTTON ---
        Button(
            onClick = onScanReceiptClick,
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Scan a New Receipt")
        }

        // --- 2. THE PEOPLE LIST ---
        Text("People", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        PeopleList(
            people = people.value,
            onAddPerson = {
                val newName = "Person ${people.value.size + 1}"
                people.value = people.value + Person(name = newName)
            },
            onEditPersonName = { person, newName ->
                val updatedList = people.value.map {
                    if (it.id == person.id) it.copy(name = newName) else it
                }
                people.value = updatedList
            },
            onDeletePerson = { personToDelete ->
                // 1. Remove the person from the main people list
                people.value = people.value.filter { it.id != personToDelete.id }

                // 2. Remove the person from any items they were assigned to
                items.forEach { item ->
                    if (item.assignedPeople.contains(personToDelete)) {
                        val updatedItem = item.copy(
                            assignedPeople = item.assignedPeople.filter { it.id != personToDelete.id }.toMutableList()
                        )
                        // Use the onUpdateItem function we already have!
                        onUpdateItem(updatedItem)
                    }
                }
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        // --- Tax and Tip Input Fields ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = taxInput,
                onValueChange = { taxInput = it },
                label = { Text("Total Tax") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${tipPercent.toInt()}%", // Show the current percent
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            OutlinedTextField(
                value = tipInput,
                onValueChange = { tipInput = it },
                label = { Text("Total Tip") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        Slider(
            value = tipPercent,
            onValueChange = { tipPercent = it },
            valueRange = 0f..30f, // From 0% to 30%
            steps = 29, // This makes it snap to whole numbers (0, 1, 2...)
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // --- 3. THE ITEM LIST ---
        // --- 3. THE ITEM LIST ---
        Text("Items", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        // --- FIX: Use a non-scrolling Column ---
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)) {
            // --- FIX: Just loop through the items ---
            items.forEach { item ->
                ItemRow(
                    item = item,
                    onClick = { setEditingItem(item) }, // Open the dialog when clicked
                    onDeleteClick = { onDeleteItem(item) }
                )
            }
        }

        // --- 4. THE EDIT DIALOG ---
        editingItem?.let { item ->
            EditItemDialog(
                item = item,
                allPeople = people.value,
                onDismiss = { setEditingItem(null) },
                // --- FIX: Receive all three parameters ---
                onSave = { updatedName, updatedPrice, assignedPeople ->
                    // --- Save the changes ---
                    val updatedItem = item.copy(
                        name = updatedName,
                        price = updatedPrice,
                        // --- This line will now work! ---
                        assignedPeople = assignedPeople.toMutableList()
                    )
                    // Call the function to update the list
                    onUpdateItem(updatedItem)
                    setEditingItem(null) // Close the dialog
                }
            )
        }
        // --- NEW! 5. THE TOTALS DISPLAY ---
        // Show the totals card if there are any totals to show
        if (calculatedTotals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            TotalsDisplay(totals = calculatedTotals)
            Spacer(modifier = Modifier.height(16.dp)) // Add space at the bottom
        }
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


// --- UI for a single item row ---
@Composable
fun ItemRow(
    item: ReceiptItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit // <-- Renamed from onLongClick
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // 1. The Delete Button
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.Clear, // This is the 'X' icon
                contentDescription = "Delete Item",
                tint = MaterialTheme.colorScheme.error // Make it red
            )
        }

        // 2. The Clickable Item Info (for editing)
        Column(
            modifier = Modifier
                .weight(1f) // This makes the clickable area fill the rest of the space
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp), // Padding is now here
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "$${String.format(java.util.Locale.US, "%.2f", item.price)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            // --- NEW: Show assigned people's initials ---
            if (item.assignedPeople.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                val initials = item.assignedPeople.joinToString(", ") {
                    it.name.take(2).uppercase() // Get first 2 letters
                }
                Text(
                    text = "Split by: $initials",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- UI for the "Edit Item" pop-up dialog ---
@Composable
fun EditItemDialog(
    item: ReceiptItem,
    allPeople: List<Person>,
    onDismiss: () -> Unit,
    onSave: (String, Double, List<Person>) -> Unit
) {
    var editName by remember { mutableStateOf(TextFieldValue(item.name)) }
    var editPrice by remember { mutableStateOf(TextFieldValue(item.price.toString())) }
    var selectedPeople by remember { mutableStateOf(item.assignedPeople.toSet())}

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Edit Item", style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Item Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editPrice,
                    onValueChange = { editPrice = it },
                    label = { Text("Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // --- Checklist of People ---
                Text("Assign to:", style = MaterialTheme.typography.titleMedium)
                // --- FIX: Use a regular Column with a constrained height ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp) // <-- Give it a max height (e.g., 200.dp)
                        .verticalScroll(rememberScrollState()) // Make *this* column scrollable
                ) {
                    allPeople.forEach { person -> // <-- FIX: Use a simple loop
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // ... (your existing click logic)
                                    val newSet = selectedPeople.toMutableSet()
                                    if (selectedPeople.contains(person)) {
                                        newSet.remove(person)
                                    } else {
                                        newSet.add(person)
                                    }
                                    selectedPeople = newSet
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedPeople.contains(person),
                                onCheckedChange = { isChecked ->
                                    // ... (your existing check logic)
                                    val newSet = selectedPeople.toMutableSet()
                                    if (isChecked) {
                                        newSet.add(person)
                                    } else {
                                        newSet.remove(person)
                                    }
                                    selectedPeople = newSet
                                }
                            )
                            Text(person.name)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newPrice = editPrice.text.toDoubleOrNull() ?: 0.0
                            onSave(editName.text, newPrice, selectedPeople.toList())
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

data class PersonTotal(
    val person: Person,
    val subtotal: Double,
    val taxShare: Double,
    val tipShare: Double,
    val totalOwed: Double
)

// --- UI for the Final Totals card ---
@Composable
fun TotalsDisplay(totals: List<PersonTotal>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Totals Per Person",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // A row for each person
            totals.forEach { personTotal ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onPersonClick(personTotal) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) { // <-- Add Column for detail
                        Text(
                            text = personTotal.person.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Sub: $${String.format(Locale.US, "%.2f", personTotal.subtotal)} " +
                                    "Tax: $${String.format(Locale.US, "%.2f", personTotal.taxShare)} " +
                                    "Tip: $${String.format(Locale.US, "%.2f", personTotal.tipShare)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", personTotal.totalOwed)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // --- Grand Total ---
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            val grandTotal = totals.sumOf { it.totalOwed }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Grand Total",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$${String.format(Locale.US, "%.2f", grandTotal)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
