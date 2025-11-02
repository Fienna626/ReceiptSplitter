package com.example.receiptsplitter.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PastelBrown,             // Your main button color
    onPrimary = Color.Black,           // Text on your buttons
    background = Cream,      // The main app background
    surface = OffWhite,         // Color of Cards, Dialogs
    surfaceVariant = Cream,  // Color for things like BottomAppBar
    onSurface = Color.Black.copy(alpha = 0.8f) // Main text color
)

private val LightColorScheme = lightColorScheme(
    primary = PastelBrown,             // Your main button color
    onPrimary = Color.Black,           // Text on your buttons
    background = Cream,      // The main app background
    surface = OffWhite,         // Color of Cards, Dialogs
    surfaceVariant = Cream,  // Color for things like BottomAppBar
    onSurface = Color.Black.copy(alpha = 0.8f) // Main text color

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)
// ? am i stupid why is it purple.

@Composable
fun ReceiptSplitterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, //<---- i found you, you little shit.
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // --- SideEffect block is GONE ---

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}