package com.financetracker.app.presentation.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary          = Teal600,
    onPrimary        = Color.White,
    primaryContainer = Teal900,
    onPrimaryContainer = TealLight,
    secondary        = Green600,
    onSecondary      = Color.White,
    secondaryContainer = Green900,
    onSecondaryContainer = Green400,
    background       = SurfaceDark,
    onBackground     = Color.White,
    surface          = SurfaceCard,
    onSurface        = Color.White,
    surfaceVariant   = SurfaceElevated,
    error            = ColorDebit,
    onError          = Color.White
)

@Composable
fun FinanceTrackerTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SurfaceDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = FinanceTypography,
        content     = content
    )
}
