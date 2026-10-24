package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NavalCADColorScheme = darkColorScheme(
    primary = MarineCyan,
    onPrimary = NavalNavyDark,
    primaryContainer = NavalSurfaceElevated,
    onPrimaryContainer = MarineCyanLight,
    secondary = MarineBronze,
    onSecondary = NavalNavyDark,
    secondaryContainer = NavalSurfaceDark,
    onSecondaryContainer = MarineGold,
    background = NavalNavyDark,
    onBackground = SteelLight,
    surface = NavalSurfaceDark,
    onSurface = SteelLight,
    surfaceVariant = NavalSurfaceElevated,
    onSurfaceVariant = SteelMuted,
    outline = NavalBorder,
    error = FeaCriticalRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = NavalNavyDark.toArgb()
                window.navigationBarColor = NavalNavyDark.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = NavalCADColorScheme,
        typography = Typography,
        content = content
    )
}
