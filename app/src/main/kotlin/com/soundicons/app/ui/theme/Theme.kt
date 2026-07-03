package com.soundicons.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Purple80       = Color(0xFFD0BCFF)
private val PurpleGrey80   = Color(0xFFCCC2DC)
private val Pink80         = Color(0xFFEFB8C8)
private val Purple40       = Color(0xFF6650A4)
private val PurpleGrey40   = Color(0xFF625B71)
private val Pink40         = Color(0xFF7D5260)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80,
    background = Color(0xFF1C1B1F), surface = Color(0xFF1C1B1F),
    onPrimary = Color(0xFF381E72), onSecondary = Color(0xFF332D41), onTertiary = Color(0xFF492532),
    onBackground = Color(0xFFE6E1E5), onSurface = Color(0xFFE6E1E5)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40, secondary = PurpleGrey40, tertiary = Pink40,
    background = Color(0xFFFFFBFE), surface = Color(0xFFFFFBFE),
    onPrimary = Color.White, onSecondary = Color.White, onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F), onSurface = Color(0xFF1C1B1F)
)

/**
 * Root theme.
 * [themeMode]: "DARK", "LIGHT", or "SYSTEM" (default).
 * Uses dynamic color on Android 12+ when themeMode is SYSTEM.
 */
@Composable
fun SoundIconsTheme(
    themeMode:    String  = "SYSTEM",
    dynamicColor: Boolean = true,
    content:      @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "DARK"  -> true
        "LIGHT" -> false
        else    -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && themeMode == "SYSTEM" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
