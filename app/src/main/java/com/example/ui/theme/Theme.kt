package com.example.ui.theme

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = MintTeal,
    onPrimary = Color(0xFF052E27),
    primaryContainer = MintTealContainer,
    onPrimaryContainer = SoftMintText,
    secondary = CoralLight,
    onSecondary = Color(0xFF3E1208),
    secondaryContainer = CoralDarkContainer,
    onSecondaryContainer = Color(0xFFFFDAD2),
    tertiary = WarmAmber,
    background = ObsidianBg,
    onBackground = LightInk,
    surface = ObsidianSurface,
    onSurface = LightInk,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = LightInkSecondary,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutline
)

private val LightColorScheme = lightColorScheme(
    primary = ForestTeal,
    onPrimary = Color.White,
    primaryContainer = SoftSage,
    onPrimaryContainer = DeepForestText,
    secondary = TerracottaCoral,
    onSecondary = Color.White,
    secondaryContainer = SoftCoral,
    onSecondaryContainer = DeepCoralText,
    tertiary = WarmAmber,
    tertiaryContainer = SoftAmber,
    background = WarmCreamBg,
    onBackground = InkPrimary,
    surface = WarmSurface,
    onSurface = InkPrimary,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = WarmOutline,
    outlineVariant = WarmOutline
)

val HaftaPlanShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun HaftaPlanTheme(
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
        shapes = HaftaPlanShapes,
        content = content
    )
}
