package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// M3 Expressive Lively Pastel Color Scheme
private val ExpressiveFintechDarkColorScheme = darkColorScheme(
    primary = Color(0xFF34D399),            // Mint Emerald Accent
    onPrimary = Color(0xFF022C22),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFFC084FC),          // Lavender Accent
    onSecondary = Color(0xFF3B0764),
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = Color(0xFFFB923C),           // Warm Peach
    onTertiary = Color(0xFF7C2D12),
    tertiaryContainer = Color(0xFF9A3412),
    onTertiaryContainer = Color(0xFFFFEDD5),
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder
)

private val ExpressiveFintechLightColorScheme = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF3B0764),
    tertiary = Color(0xFFEA580C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEDD5),
    onTertiaryContainer = Color(0xFF7C2D12),
    background = LightBackground,
    onBackground = Color(0xFF0F172A),
    surface = LightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = LightBorder
)

// Material 3 Expressive Shapes (Generous, playful curves)
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    palette: String = "DEFAULT",
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        palette == "AMOLED" -> ExpressiveFintechDarkColorScheme.copy(
            background = Color.Black,
            surface = Color(0xFF0A0A0E),
            surfaceVariant = Color(0xFF141418),
            primary = Color(0xFF2DD4BF),      // Pure Cyan Mint for AMOLED
            onPrimary = Color(0xFF003731),
            primaryContainer = Color(0xFF005047),
            onPrimaryContainer = Color(0xFF99F6E4),
            secondary = Color(0xFFA78BFA),
            outline = Color(0xFF27272A)
        )
        palette == "MINT" -> ExpressiveFintechDarkColorScheme.copy(
            primary = Color(0xFF10B981),      // Emerald Mint
            onPrimary = Color(0xFF022C22),
            primaryContainer = Color(0xFF064E3B),
            onPrimaryContainer = Color(0xFFA7F3D0),
            secondary = Color(0xFF2DD4BF),
            surface = Color(0xFF061A16),
            surfaceVariant = Color(0xFF0D2923)
        )
        palette == "LAVENDER" -> ExpressiveFintechDarkColorScheme.copy(
            primary = Color(0xFFA855F7),      // Deep Electric Lavender
            onPrimary = Color(0xFF3B0764),
            primaryContainer = Color(0xFF581C87),
            onPrimaryContainer = Color(0xFFF3E8FF),
            secondary = Color(0xFFC084FC),
            surface = Color(0xFF120C1F),
            surfaceVariant = Color(0xFF1E1530)
        )
        palette == "ROSE" -> ExpressiveFintechDarkColorScheme.copy(
            primary = Color(0xFFF43F5E),      // Vivid Rose Pink
            onPrimary = Color(0xFF4C0519),
            primaryContainer = Color(0xFF881337),
            onPrimaryContainer = Color(0xFFFFE4E6),
            secondary = Color(0xFFFB7185),
            surface = Color(0xFF1A0A10),
            surfaceVariant = Color(0xFF2B121C)
        )
        palette == "SLATE" -> ExpressiveFintechDarkColorScheme.copy(
            primary = Color(0xFF38BDF8),      // Sky Electric Slate
            onPrimary = Color(0xFF032B45),
            primaryContainer = Color(0xFF075985),
            onPrimaryContainer = Color(0xFFE0F2FE),
            secondary = Color(0xFF818CF8),
            surface = Color(0xFF0B1424),
            surfaceVariant = Color(0xFF162338)
        )
        palette == "PEACH" -> ExpressiveFintechDarkColorScheme.copy(
            primary = Color(0xFFF97316),      // Vivid Peach Amber
            onPrimary = Color(0xFF431407),
            primaryContainer = Color(0xFF7C2D12),
            onPrimaryContainer = Color(0xFFFFEDD5),
            secondary = Color(0xFFFBBF24),
            surface = Color(0xFF1A0F0A),
            surfaceVariant = Color(0xFF2A1912)
        )
        darkTheme -> ExpressiveFintechDarkColorScheme
        else -> ExpressiveFintechLightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ExpressiveShapes,
        content = content
    )
}
