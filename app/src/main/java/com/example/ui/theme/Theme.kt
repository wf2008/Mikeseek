package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = CyanPrimary,
    secondary = BlueSecondary,
    tertiary = GreenAccent,
    background = BackgroundDark,
    surface = SurfaceDarkCard,
    onBackground = Color.White,
    onSurface = Color.White
  )

private val LightColorScheme =
  darkColorScheme(
    primary = CyanPrimary,
    secondary = BlueSecondary,
    tertiary = GreenAccent,
    background = BackgroundDark,
    surface = SurfaceDarkCard,
    onBackground = Color.White,
    onSurface = Color.White
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  // Dynamic color is disabled to prevent system colors from overriding the premium branding
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
