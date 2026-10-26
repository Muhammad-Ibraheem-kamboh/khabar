package com.khabar.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = SystemBlue,
    onPrimary = CardSurfaceLight,
    primaryContainer = SystemBlueContainer,
    onPrimaryContainer = CardSurfaceLight,
    background = CanvasLight,
    onBackground = PrimaryLabelLight,
    surface = CardSurfaceLight,
    onSurface = PrimaryLabelLight,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = SecondaryLabelLight,
    outline = SecondaryLabelLight,
    outlineVariant = HairlineSeparatorLight
)

private val DarkColorScheme = darkColorScheme(
    primary = SystemBlueDark,
    onPrimary = CardSurfaceLight,
    primaryContainer = SystemBlueDark,
    onPrimaryContainer = CardSurfaceLight,
    background = CanvasDark,
    onBackground = PrimaryLabelDark,
    surface = CardSurfaceDark,
    onSurface = PrimaryLabelDark,
    surfaceVariant = ElevatedSurfaceDark,
    onSurfaceVariant = SecondaryLabelDark,
    outline = SecondaryLabelDark,
    outlineVariant = HairlineSeparatorDark
)

@Composable
fun KhabarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = KhabarShapes,
        typography = KhabarTypography,
        content = content
    )
}
