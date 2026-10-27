package com.example.futtersuche.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

/**
 * Eigenes, buntes Farbschema für Futtersuche (statt der Systemfarben des Handys),
 * damit das Spiel überall gleich freundlich aussieht.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFFD9480F),            // kräftiges Karotten-Orange
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC7),
    onPrimaryContainer = Color(0xFF3A1300),
    secondary = Color(0xFF2E7D32),          // Blattgrün
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Color(0xFF0B2E0F),
    tertiary = Color(0xFFC2185B),           // Himbeer-Pink
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9E4),
    onTertiaryContainer = Color(0xFF3E0019),
    background = Color(0xFFFFFBF3),
    onBackground = Color(0xFF2B2118),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2B2118),
    surfaceVariant = Color(0xFFF6EBDD),
    onSurfaceVariant = Color(0xFF51443A),
    error = Color(0xFFC62828),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB68F),
    onPrimary = Color(0xFF552000),
    primaryContainer = Color(0xFF7A3000),
    onPrimaryContainer = Color(0xFFFFDCC7),
    secondary = Color(0xFF9CD69F),
    onSecondary = Color(0xFF0B3A11),
    secondaryContainer = Color(0xFF1E5122),
    onSecondaryContainer = Color(0xFFC8E6C9),
    tertiary = Color(0xFFFFB0C8),
    onTertiary = Color(0xFF5E1133),
    tertiaryContainer = Color(0xFF7D2949),
    onTertiaryContainer = Color(0xFFFFD9E4),
    background = Color(0xFF1B1612),
    onBackground = Color(0xFFF1E5DA),
    surface = Color(0xFF26201B),
    onSurface = Color(0xFFF1E5DA),
    surfaceVariant = Color(0xFF3A3029),
    onSurfaceVariant = Color(0xFFD9C8B9),
    error = Color(0xFFFF8A80),
)

/** Etwas kräftigere Überschriften – wirkt verspielter. */
private val GameTypography = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun FuttersucheTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = GameTypography,
        content = content,
    )
}
