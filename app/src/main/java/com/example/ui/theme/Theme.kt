package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GoogleBlueLight,
    onPrimary = GoogleBlueDark,
    primaryContainer = GoogleBlueDark,
    onPrimaryContainer = GoogleBlueLight,
    secondary = GoogleBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1F2A38),
    onSecondaryContainer = GoogleBlueLight,
    tertiary = GoogleGreenLight,
    onTertiary = GoogleGreenDark,
    error = Color(0xFFF28B82),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE8EAED),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF2D2E30),
    onSurfaceVariant = Color(0xFFC4C7C5),
    outline = Color(0xFF5F6368)
)

private val LightColorScheme = lightColorScheme(
    primary = GoogleBlue,
    onPrimary = Color.White,
    primaryContainer = GoogleBlueContainer,
    onPrimaryContainer = GoogleBlueDark,
    secondary = GoogleBlueDark,
    onSecondary = Color.White,
    secondaryContainer = GoogleBlueLight,
    onSecondaryContainer = GoogleBlueDark,
    tertiary = GoogleGreen,
    onTertiary = Color.White,
    tertiaryContainer = GoogleGreenLight,
    onTertiaryContainer = GoogleGreenDark,
    error = GoogleRed,
    onError = Color.White,
    errorContainer = GoogleRedLight,
    onErrorContainer = GoogleRedDark,
    background = SurfaceLight,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF1F3F4),
    onSurfaceVariant = TextSecondary,
    outline = OutlineGrey
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use intentional Google Material You identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
