package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val GymColorScheme = darkColorScheme(
    primary = GymPrimary,
    onPrimary = GymOnPrimary,
    primaryContainer = GymPrimaryContainer,
    onPrimaryContainer = GymTextPrimary,
    secondary = GymSecondary,
    onSecondary = GymOnSecondary,
    secondaryContainer = GymSurfaceVariant,
    onSecondaryContainer = GymTextPrimary,
    tertiary = Color(0xFFFF5E00), // Bright Orange
    onTertiary = Color.White,
    background = GymBackground,
    onBackground = GymTextPrimary,
    surface = GymSurface,
    onSurface = GymTextPrimary,
    surfaceVariant = GymSurfaceVariant,
    onSurfaceVariant = GymTextSecondary,
    outline = GymBorder,
    outlineVariant = GymBorder,
    error = GymError,
    onError = Color.White
)

val GymShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for Gym Aesthetic
  // Disabling dynamic colors to show custom branding
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = GymColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, shapes = GymShapes, content = content)
}
