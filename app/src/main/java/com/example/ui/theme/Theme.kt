package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.ThemeMode

val SwiftWebShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private fun incognitoColorScheme() = darkColorScheme(
    primary = IncognitoPrimary,
    onPrimary = IncognitoOnPrimary,
    primaryContainer = IncognitoPrimaryContainer,
    onPrimaryContainer = IncognitoOnPrimaryContainer,
    secondary = ShieldEmeraldDark,
    onSecondary = Color(0xFF002116),
    secondaryContainer = Color(0xFF0B3B2B),
    onSecondaryContainer = Color(0xFFA7F3D0),
    background = IncognitoCanvas,
    onBackground = IncognitoInk,
    surface = IncognitoSurface,
    onSurface = IncognitoInk,
    surfaceVariant = IncognitoSurfaceVariant,
    onSurfaceVariant = IncognitoInkMuted,
    surfaceContainer = IncognitoSurface,
    surfaceContainerHigh = IncognitoSurfaceVariant,
    outline = IncognitoOutline,
    outlineVariant = Color(0xFF2E2245),
    error = DangerCrimson
)

private fun buildLightScheme(accent: AccentPreset) = lightColorScheme(
    primary = accent.lightPrimary,
    onPrimary = Color.White,
    primaryContainer = accent.lightContainer,
    onPrimaryContainer = accent.lightOnContainer,
    secondary = ShieldEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),
    background = LightCanvas,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightInkMuted,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineSubtle,
    error = DangerCrimson
)

private fun buildDarkScheme(accent: AccentPreset, isAmoled: Boolean) = darkColorScheme(
    primary = accent.darkPrimary,
    onPrimary = Color(0xFF081020),
    primaryContainer = accent.darkContainer,
    onPrimaryContainer = accent.darkOnContainer,
    secondary = ShieldEmeraldDark,
    onSecondary = Color(0xFF002116),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFA7F3D0),
    background = if (isAmoled) AmoledCanvas else DarkCanvas,
    onBackground = DarkInk,
    surface = if (isAmoled) AmoledSurface else DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = if (isAmoled) AmoledSurfaceVariant else DarkSurfaceVariant,
    onSurfaceVariant = DarkInkMuted,
    surfaceContainer = if (isAmoled) AmoledSurface else DarkSurfaceContainer,
    surfaceContainerHigh = if (isAmoled) AmoledSurfaceVariant else DarkSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineSubtle,
    error = DangerCrimson
)

@Composable
fun SwiftWebTheme(
    themeMode: ThemeMode = ThemeMode.AMOLED,
    useDynamicColor: Boolean = false,
    accentPreset: AccentPreset = AccentPreset.COBALT,
    isIncognito: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when {
        isIncognito -> true
        themeMode == ThemeMode.DARK || themeMode == ThemeMode.AMOLED -> true
        themeMode == ThemeMode.LIGHT -> false
        else -> systemDark
    }
    val isAmoled = !isIncognito && themeMode == ThemeMode.AMOLED
    val context = LocalContext.current

    val colorScheme = when {
        isIncognito -> incognitoColorScheme()
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDark) {
                val base = dynamicDarkColorScheme(context)
                if (isAmoled) base.copy(background = AmoledCanvas, surface = AmoledSurface) else base
            } else {
                dynamicLightColorScheme(context)
            }
        }
        useDark -> buildDarkScheme(accentPreset, isAmoled)
        else -> buildLightScheme(accentPreset)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !useDark
                controller.isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SwiftWebShapes,
        content = content
    )
}
