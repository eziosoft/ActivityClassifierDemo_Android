package com.example.activityclassifierdemo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary              = Primary,
    onPrimary            = OnBackground,
    primaryContainer     = SurfaceContainer,
    onPrimaryContainer   = OnBackground,
    secondary            = Secondary,
    onSecondary          = Background,
    secondaryContainer   = SurfaceContainer,
    onSecondaryContainer = OnSurfaceVariant,
    error                = Error,
    onError              = Background,
    background           = Background,
    onBackground         = OnBackground,
    surface              = Surface,
    onSurface            = OnBackground,
    surfaceVariant       = SurfaceContainer,
    onSurfaceVariant     = OnSurfaceVariant,
    outline              = Outline,
    outlineVariant       = OutlineVariant,
)

@Composable
fun ActivityClassifierDemoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
