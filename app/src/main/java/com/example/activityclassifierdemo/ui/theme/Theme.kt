package com.example.activityclassifierdemo.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary              = Teal700,
    onPrimary            = Color.White,
    primaryContainer     = Teal100,
    onPrimaryContainer   = Teal900,
    secondary            = Blue700,
    onSecondary          = Color.White,
    secondaryContainer   = Blue50,
    onSecondaryContainer = Blue900,
    background           = Grey50,
    onBackground         = Grey900,
    surface              = Color.White,
    onSurface            = Grey900,
    surfaceVariant       = Teal50,
    onSurfaceVariant     = Grey800,
)

private val DarkColorScheme = darkColorScheme(
    primary              = Teal200,
    onPrimary            = Teal900,
    primaryContainer     = Teal700,
    onPrimaryContainer   = Teal50,
    secondary            = Blue200,
    onSecondary          = Blue900,
    secondaryContainer   = Blue900,
    onSecondaryContainer = Blue100,
    background           = Grey900,
    onBackground         = Grey100,
    surface              = Grey800,
    onSurface            = Grey100,
    surfaceVariant       = Grey800,
    onSurfaceVariant     = Grey100,
)

@Composable
fun ActivityClassifierDemoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
