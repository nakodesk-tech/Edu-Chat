package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryIndigo,
    onPrimary = Color.White,
    primaryContainer = PrimaryIndigoContainer,
    onPrimaryContainer = OnPrimaryIndigoContainer,
    secondary = SecondaryGreen,
    onSecondary = Color.White,
    secondaryContainer = SecondaryGreenContainer,
    onSecondaryContainer = OnSecondaryGreenContainer,
    tertiary = AccentAmber,
    onTertiary = Color.White,
    tertiaryContainer = AccentAmberContainer,
    onTertiaryContainer = OnAccentAmberContainer,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = CardHighlight,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = Color(0xFFF3F4F6),
  )

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = LightColorScheme,
    typography = Typography,
    content = content
  )
}

