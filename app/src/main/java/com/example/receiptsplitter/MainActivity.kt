package com.example.receiptsplitter // Make sure this matches your package name

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.receiptsplitter.ui.theme.ReceiptSplitterTheme // Make sure this matches your theme name
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.Objects
import androidx.compose.foundation.background

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This function creates a temporary file in your app's cache to store the camera photo
        fun getTempUri(context: Context): Uri {
            val file = File.createTempFile("temp_image", ".jpg", context.cacheDir)
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider", // This must match your AndroidManifest
                file
            )
        }

        setContent {
            ReceiptSplitterTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // This is our main app screen
                    ReceiptScannerScreen(
                        getTempUri = { getTempUri(applicationContext) }
                    )
                }
            }
        }
    }
    // Inside MainActivity class, but outside onCreate
    fun parseReceiptText(rawText: String): List<ReceiptItem> {
        // This is the Regex. It looks for:
        // (.+)      - Group 1: One or more characters (the item name)
        // [ ]+      - One or more spaces
        // (\d+\.\d{2}) - Group 2: One or more digits, a literal dot, and exactly two digits (the price)
        val regex = "(.+)[ ]+(\\d+\\.\\d{2})".toRegex()

        val items = mutableListOf<ReceiptItem>()

        // Go through the raw text line by line
        rawText.lines().forEach { line ->
            val match = regex.find(line)
            if (match != null) {
                // A match was found!
                val (name, priceStr) = match.destructured
                val price = priceStr.toDoubleOrNull() ?: 0.0 // Convert price string to a Double

                // Trim the name to remove extra spaces
                val cleanName = name.trim()

                // Don't add items with no name or price
                if (cleanName.isNotEmpty() && price > 0.0) {
                    items.add(ReceiptItem(name = cleanName, price = price))
                }
            }
        }
        return items
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReceiptScannerScreen(getTempUri: () -> Uri) {
    val context = LocalContext.current

    // --- STATE VARIABLES ---
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // <-- NEW! Store a list of items, not just raw text
    var receiptItems by remember { mutableStateOf(listOf<ReceiptItem>()) }

    // This variable controls whether the "Choose Camera/Gallery" dialog is open
    var showOptionsDialog by remember { mutableStateOf(false) }

    // This variable controls whether the "Camera Permission" dialog is open
    var showPermissionDialog by remember { mutableStateOf(false) }

    // --- ML KIT: THE RECEIPT READER ---
    val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // <-- NEW! We get the parser function from the Activity
    // We need to find the Activity to call its 'parseReceiptText' function
    val activity = (LocalActivity.current as? MainActivity)

    fun processImage(uri: Uri) {
        try {
            val inputImage = InputImage.fromFilePath(context, uri)

            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    // 3. Success! Get the raw text
                    val rawText = visionText.text
                    Log.d("TEXT_RECOGNITION", "Success: \n$rawText")

                    // <-- NEW! Call the parser
                    if (activity != null) {
                        receiptItems = activity.parseReceiptText(rawText)
                    } else {
                        Log.e("PARSER", "Could not get MainActivity instance")
                    }
                }
                .addOnFailureListener { e ->
                    // 4. Failed
                    Log.e("TEXT_RECOGNITION", "Failed:", e)
                }
        } catch (e: Exception) {
            Log.e("TEXT_RECOGNITION", "Error:", e)
        }
    }

    // --- LAUNCHERS (These are all the same as before) ---
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                imageUri = uri
                processImage(uri)
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                imageUri?.let { processImage(it) }
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val newImageUri = getTempUri()
                imageUri = newImageUri
                cameraLauncher.launch(newImageUri)
            } else {
                Toast.makeText(context, "Camera permission is required.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // --- DIALOGS (These are all the same as before) ---
    if (showOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = { Text("Split a new receipt") },
            text = { Text("How do you want to add your receipt?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOptionsDialog = false
                        showPermissionDialog = true
                    }
                ) {
                    Text("Open Camera")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showOptionsDialog = false
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Text("From Gallery")
                }
            }
        )
    }

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
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- UI (What the user sees) ---
    // <-- NEW! This UI is completely different
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Button at the top
        Button(
            onClick = { showOptionsDialog = true },
            modifier = Modifier.padding(16.dp) // Add padding
        ) {
            Text("Split a New Receipt")
        }

        // --- The new itemized list ---

        // LazyColumn is Compose's efficient way to show a long, scrollable list
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {

            // This is a "sticky" header that will stay at the top
            stickyHeader {
                Text(
                    text = "Items Found: ${receiptItems.size}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // This is the loop that creates one row for each item
            items(receiptItems, key = { it.id }) { item ->
                ListItem(item = item)
            }
        }
    }
}
data class ReceiptItem(
    val id: java.util.UUID = java.util.UUID.randomUUID(), // Unique ID for each item
    var name: String,
    var price: Double
)

@Composable
fun ListItem(item: ReceiptItem) {
    // You will need to import:
    // androidx.compose.foundation.layout.Row
    // androidx.compose.foundation.layout.Spacer
    // androidx.compose.foundation.layout.fillMaxWidth
    // androidx.compose.foundation.layout.width
    // androidx.compose.foundation.clickable

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // TODO: This is where we will add the logic to assign the item
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Item Name
        Text(
            text = item.name,
            modifier = Modifier.weight(1f), // This makes the name take up all extra space
            style = MaterialTheme.typography.bodyLarge
        )

        // Spacer to push price to the right
        Spacer(modifier = Modifier.width(16.dp))

        // Item Price
        Text(
            // Format the price to always show two decimal places (e.g., "$12.50")
            text = "$${String.format(java.util.Locale.US, "%.2f", item.price)}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// Inside MainActivity class, but outside onCreate
private fun parseReceiptText(rawText: String): List<ReceiptItem> {
    // This is the Regex. It looks for:
    // (.+)      - Group 1: One or more characters (the item name)
    // [ ]+      - One or more spaces
    // (\d+\.\d{2}) - Group 2: One or more digits, a literal dot, and exactly two digits (the price)
    val regex = "(.+)[ ]+(\\d+\\.\\d{2})".toRegex()

    val items = mutableListOf<ReceiptItem>()

    // Go through the raw text line by line
    rawText.lines().forEach { line ->
        val match = regex.find(line)
        if (match != null) {
            // A match was found!
            val (name, priceStr) = match.destructured
            val price = priceStr.toDoubleOrNull() ?: 0.0 // Convert price string to a Double

            // Trim the name to remove extra spaces
            val cleanName = name.trim()

            // Don't add items with no name or price
            if (cleanName.isNotEmpty() && price > 0.0) {
                items.add(ReceiptItem(name = cleanName, price = price))
            }
        }
    }
    return items
}