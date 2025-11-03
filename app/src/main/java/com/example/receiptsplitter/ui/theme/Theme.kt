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
    primary = PastelBrown,            // Main button color
    onPrimary = Color.White,          // Text on main buttons
    primaryContainer = Color(0xFFD7C8BC), // Lighter shade of primary
    onPrimaryContainer = DarkText,

    secondary = DarkPastelBrown,      // Accent color
    onSecondary = Color.White,

    background = CreamBackground,     // Main app background
    onBackground = DarkText,          // Text on the background

    surface = OffWhiteSurface,        // Card/Bar background
    onSurface = DarkText,             // Text on cards

    surfaceVariant = CreamBackground, // Can be used for Top/Bottom bars if you want them slightly different
    onSurfaceVariant = MutedText      // Secondary text (like dates)
)

private val LightColorScheme = lightColorScheme(
    primary = PastelBrown,            // Main button color
    onPrimary = Color.White,          // Text on main buttons
    primaryContainer = Color(0xFFD7C8BC), // Lighter shade of primary
    onPrimaryContainer = DarkText,

    secondary = DarkPastelBrown,      // Accent color
    onSecondary = Color.White,

    background = CreamBackground,     // Main app background
    onBackground = DarkText,          // Text on the background

    surface = OffWhiteSurface,        // Card/Bar background
    onSurface = DarkText,             // Text on cards

    surfaceVariant = CreamBackground, // Can be used for Top/Bottom bars if you want them slightly different
    onSurfaceVariant = MutedText      // Secondary text (like dates)
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