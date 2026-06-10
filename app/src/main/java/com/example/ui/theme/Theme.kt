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

private val ImmersiveColorScheme = darkColorScheme(
    primary = ImmersiveLime,
    onPrimary = ImmersiveBackground,
    secondary = ImmersiveTextSecondary,
    onSecondary = ImmersiveBackground,
    background = ImmersiveBackground,
    onBackground = ImmersiveTextPrimary,
    surface = ImmersiveSurface,
    onSurface = ImmersiveTextPrimary,
    outline = ImmersiveBorder,
    surfaceVariant = ImmersiveSurface,
    onSurfaceVariant = ImmersiveTextSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ImmersiveColorScheme,
        typography = Typography,
        content = content
    )
}
