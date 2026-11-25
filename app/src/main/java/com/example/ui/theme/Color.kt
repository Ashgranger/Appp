package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Core Neutrals — Light
val LightCanvas = Color(0xFFF6F8FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEDF2F8)
val LightSurfaceContainer = Color(0xFFF1F5FA)
val LightInk = Color(0xFF0C1425)
val LightInkMuted = Color(0xFF526077)
val LightOutline = Color(0xFFC8D3E1)
val LightOutlineSubtle = Color(0xFFE3EAF3)

// Core Neutrals — Dark
val DarkCanvas = Color(0xFF090D16)
val DarkSurface = Color(0xFF121A29)
val DarkSurfaceVariant = Color(0xFF1A2538)
val DarkSurfaceContainer = Color(0xFF151F31)
val DarkInk = Color(0xFFF3F6FB)
val DarkInkMuted = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF334155)
val DarkOutlineSubtle = Color(0xFF223047)

// Pure AMOLED Black
val AmoledCanvas = Color(0xFF000000)
val AmoledSurface = Color(0xFF0B0E16)
val AmoledSurfaceVariant = Color(0xFF151B27)

// Shield & Telemetry Semantic Accents
val ShieldEmerald = Color(0xFF10B981)
val ShieldEmeraldDark = Color(0xFF34D399)
val WarningAmber = Color(0xFFF59E0B)
val DangerCrimson = Color(0xFFEF4444)

// Incognito Stealth Violet Palette
val IncognitoCanvas = Color(0xFF0E0A17)
val IncognitoSurface = Color(0xFF191227)
val IncognitoSurfaceVariant = Color(0xFF251B3A)
val IncognitoPrimary = Color(0xFFC084FC)
val IncognitoOnPrimary = Color(0xFF22073D)
val IncognitoPrimaryContainer = Color(0xFF3B225E)
val IncognitoOnPrimaryContainer = Color(0xFFF5EBFF)
val IncognitoInk = Color(0xFFF6F3FF)
val IncognitoInkMuted = Color(0xFFA89AC7)
val IncognitoOutline = Color(0xFF433263)

// 6 User-Selectable Accent Presets
enum class AccentPreset(
    val displayName: String,
    val lightPrimary: Color,
    val lightContainer: Color,
    val lightOnContainer: Color,
    val darkPrimary: Color,
    val darkContainer: Color,
    val darkOnContainer: Color
) {
    COBALT(
        displayName = "Electric Cobalt",
        lightPrimary = Color(0xFF2563EB),
        lightContainer = Color(0xFFDBEAFE),
        lightOnContainer = Color(0xFF1E3A8A),
        darkPrimary = Color(0xFF60A5FA),
        darkContainer = Color(0xFF1E3A8A),
        darkOnContainer = Color(0xFFDBEAFE)
    ),
    EMERALD(
        displayName = "Emerald Pulse",
        lightPrimary = Color(0xFF059669),
        lightContainer = Color(0xFFD1FAE5),
        lightOnContainer = Color(0xFF064E3B),
        darkPrimary = Color(0xFF34D399),
        darkContainer = Color(0xFF065F46),
        darkOnContainer = Color(0xFFD1FAE5)
    ),
    CRIMSON(
        displayName = "Crimson Spark",
        lightPrimary = Color(0xFFE11D48),
        lightContainer = Color(0xFFFFE4E6),
        lightOnContainer = Color(0xFF881337),
        darkPrimary = Color(0xFFFB7185),
        darkContainer = Color(0xFF881337),
        darkOnContainer = Color(0xFFFFE4E6)
    ),
    AMBER(
        displayName = "Solar Amber",
        lightPrimary = Color(0xFFD97706),
        lightContainer = Color(0xFFFEF3C7),
        lightOnContainer = Color(0xFF78350F),
        darkPrimary = Color(0xFFFBBF24),
        darkContainer = Color(0xFF78350F),
        darkOnContainer = Color(0xFFFEF3C7)
    ),
    VIOLET(
        displayName = "Royal Violet",
        lightPrimary = Color(0xFF7C3AED),
        lightContainer = Color(0xFFEDE9FE),
        lightOnContainer = Color(0xFF4C1D95),
        darkPrimary = Color(0xFFA78BFA),
        darkContainer = Color(0xFF4C1D95),
        darkOnContainer = Color(0xFFEDE9FE)
    ),
    TEAL(
        displayName = "Cyber Teal",
        lightPrimary = Color(0xFF0891B2),
        lightContainer = Color(0xFFCFFAFE),
        lightOnContainer = Color(0xFF164E63),
        darkPrimary = Color(0xFF22D3EE),
        darkContainer = Color(0xFF164E63),
        darkOnContainer = Color(0xFFCFFAFE)
    )
}
