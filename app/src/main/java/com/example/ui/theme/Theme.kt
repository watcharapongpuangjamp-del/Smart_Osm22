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

private val DarkColorScheme =
  darkColorScheme(
    primary = EmeraldPrimaryDarkTheme,
    onPrimary = Color(0xFF043324),
    primaryContainer = Color(0xFF0E543E),
    onPrimaryContainer = Color(0xFF86F3C7),
    secondary = TealSecondaryDarkTheme,
    onSecondary = Color(0xFF02363D),
    secondaryContainer = Color(0xFF13525D),
    onSecondaryContainer = Color(0xFFA5F3FC),
    tertiary = MintAccent,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = HairlineBorderDark,
    outlineVariant = Color(0xFF1B382D)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3EEDF),
    onPrimaryContainer = Color(0xFF063B2B),
    secondary = TealSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4EFF4),
    onSecondaryContainer = Color(0xFF0C3D46),
    tertiary = MintAccent,
    onTertiary = Color(0xFF023926),
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onBackground = OnSurfacePrimary,
    onSurface = OnSurfacePrimary,
    onSurfaceVariant = OnSurfaceSecondary,
    outline = HairlineBorder,
    outlineVariant = Color(0xFFE0E8E2)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Keep intentional branding consistent across devices
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

