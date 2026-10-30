package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Smartly Muted Satisfying Pastel Palette (Non-minty, quiet luxury fintech aesthetic)
val PastelSand = Color(0xFFE5DDD0)          // Warm cashmere sand (Primary brand accent)
val PastelSandDark = Color(0xFF38332B)

val PastelClay = Color(0xFFD8C1B0)          // Subdued terracotta / warm clay
val PastelClayDark = Color(0xFF3E3028)

val PastelLavender = Color(0xFFCBC4E2)      // Soft lavender mist
val PastelLavenderDark = Color(0xFF2E293F)

val PastelSlate = Color(0xFFB8C7D9)         // Muted steel slate
val PastelSlateDark = Color(0xFF222B36)

val PastelSage = Color(0xFFC3CEC2)          // Soft sage stone (Understated, non-neon)
val PastelSageDark = Color(0xFF232E24)

// Backward-compatible semantic pastel aliases (remapped to muted, elegant non-neon tones)
val PastelMint = PastelSand                 // Remapped away from neon green to sophisticated warm sand
val PastelMintDark = PastelSandDark
val PastelPeach = PastelClay
val PastelPeachDark = PastelClayDark
val PastelSky = PastelSlate
val PastelSkyDark = PastelSlateDark
val PastelRose = Color(0xFFD6B5BC)
val PastelRoseDark = Color(0xFF3A282D)
val PastelLemon = Color(0xFFDFD4B5)
val PastelLemonDark = Color(0xFF363022)

// Muted theme aliases for previous high-contrast tokens
val NeonCyan = PastelSand
val ElectricPurple = PastelLavender
val SunsetRose = PastelClay

// Pure AMOLED Black & Refined Low-Glare Surfaces
val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF0F0F12)
val DarkSurfaceVariant = Color(0xFF17171C)
val DarkSurfaceGlass = Color(0xF2121216)
val DarkBorder = Color(0xFF23232A)
val DarkDivider = Color(0xFF1E1E24)

// Light Theme (Clean Minimal)
val LightBackground = Color(0xFFF9F9FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F1F5)
val LightBorder = Color(0xFFE2E4EB)

val TextPrimary = Color(0xFFF4F4F6)
val TextSecondary = Color(0xFF9E9EA8)
val TextMuted = Color(0xFF6B6B78)

// Soft Card Gradients - Tactile, Physical, Refined Materials
// Tactile Card Finishes - Rich Luxury Gradients with High-Contrast Luminous Accents
object CardGradients {
    // 1. Warm Cashmere Sandstone
    val SandstoneBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF2C241E), Color(0xFF3B3028), Color(0xFF1E1712))
    )
    // 2. Midnight Lavender / Royal Amethyst
    val LavenderMistBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF241C35), Color(0xFF382956), Color(0xFF181224))
    )
    // 3. Terracotta Rust / Obsidian Amber
    val TerracottaBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF351F18), Color(0xFF4C2A1E), Color(0xFF20120D))
    )
    // 4. Cobalt Titanium / Deep Sapphire Slate
    val SlateSteelBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF162332), Color(0xFF1F3249), Color(0xFF0F1823))
    )
    // 5. Deep Forest Emerald
    val SageStoneBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF14291B), Color(0xFF1F3D29), Color(0xFF0E1C12))
    )
    // 6. Brushed Graphite Carbon
    val BrushedGraphiteBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF1B1B20), Color(0xFF282830), Color(0xFF121216))
    )
    val VelvetObsidianBrush = BrushedGraphiteBrush

    fun getBrush(colorKey: String): Brush {
        return when (colorKey) {
            "obsidian_gold", "peach_sunset" -> TerracottaBrush
            "electric_purple", "lavender_dream" -> LavenderMistBrush
            "sunset_blaze", "rose_blossom" -> TerracottaBrush
            "emerald_matrix", "lemon_zest" -> SageStoneBrush
            "deep_stealth", "sky_aura" -> SlateSteelBrush
            else -> SandstoneBrush
        }
    }

    fun getPrimaryColor(colorKey: String): Color {
        return when (colorKey) {
            "obsidian_gold", "peach_sunset" -> Color(0xFFE2B08C)
            "electric_purple", "lavender_dream" -> Color(0xFFD6CEF2)
            "sunset_blaze", "rose_blossom" -> Color(0xFFE8BAC4)
            "emerald_matrix", "lemon_zest" -> Color(0xFFBCE3C4)
            "deep_stealth", "sky_aura" -> Color(0xFFBAD2EC)
            else -> Color(0xFFF3ECE0)
        }
    }

    fun getAccentDarkColor(colorKey: String): Color {
        return when (colorKey) {
            "obsidian_gold", "peach_sunset" -> Color(0xFF4C2A1E)
            "electric_purple", "lavender_dream" -> Color(0xFF382956)
            "sunset_blaze", "rose_blossom" -> Color(0xFF45222A)
            "emerald_matrix", "lemon_zest" -> Color(0xFF1B3D25)
            "deep_stealth", "sky_aura" -> Color(0xFF192C40)
            else -> Color(0xFF3B3028)
        }
    }
}
