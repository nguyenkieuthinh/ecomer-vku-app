package com.ecomer.vku.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EcomerColorScheme = lightColorScheme(
    primary = EcomerPrimary,
    onPrimary = Color.White,
    primaryContainer = EcomerPrimarySoft,
    onPrimaryContainer = EcomerPrimaryDark,
    secondary = EcomerTextMuted,
    onSecondary = Color.White,
    background = EcomerBackground,
    onBackground = EcomerTextPrimary,
    surface = EcomerSurface,
    onSurface = EcomerTextPrimary,
    surfaceVariant = EcomerBackground,
    onSurfaceVariant = EcomerTextMuted,
    outline = EcomerBorder,
    error = EcomerDanger,
    onError = Color.White,
)

@Composable
fun EcomerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EcomerColorScheme,
        typography = EcomerTypography,
        content = content,
    )
}
