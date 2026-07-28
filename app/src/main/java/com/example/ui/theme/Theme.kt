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

private val DarkColorScheme =
  darkColorScheme(
    primary = MD3DarkPrimary,
    onPrimary = MD3DarkOnPrimary,
    primaryContainer = MD3DarkPrimaryContainer,
    onPrimaryContainer = MD3DarkOnPrimaryContainer,
    secondary = MD3DarkSecondary,
    onSecondary = MD3DarkOnSecondary,
    secondaryContainer = MD3DarkSecondaryContainer,
    onSecondaryContainer = MD3DarkOnSecondaryContainer,
    background = MD3DarkBackground,
    onBackground = MD3DarkOnBackground,
    surface = MD3DarkSurface,
    onSurface = MD3DarkOnSurface,
    surfaceVariant = MD3DarkSurfaceVariant,
    onSurfaceVariant = MD3DarkOnSurfaceVariant,
    outline = MD3DarkOutline,
    outlineVariant = MD3DarkOutlineVariant
  )

private val LightColorScheme =
  lightColorScheme(
    primary = MD3Primary,
    onPrimary = MD3OnPrimary,
    primaryContainer = MD3PrimaryContainer,
    onPrimaryContainer = MD3OnPrimaryContainer,
    secondary = MD3Secondary,
    onSecondary = MD3OnSecondary,
    secondaryContainer = MD3SecondaryContainer,
    onSecondaryContainer = MD3OnSecondaryContainer,
    background = MD3Background,
    onBackground = MD3OnBackground,
    surface = MD3Surface,
    onSurface = MD3OnSurface,
    surfaceVariant = MD3SurfaceVariant,
    onSurfaceVariant = MD3OnSurfaceVariant,
    outline = MD3Outline,
    outlineVariant = MD3OutlineVariant
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is disabled by default to keep the gorgeous customized Professional Polish scheme
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
