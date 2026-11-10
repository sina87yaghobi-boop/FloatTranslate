package com.floattranslate.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection

object LavaColors {
    val Ember = Color(0xFFFF6A2B)
    val Magma = Color(0xFFC62E12)
    val Gold = Color(0xFFF2B84B)
}

private val LavaDark = darkColorScheme(
    primary = Color(0xFFFF7A33), onPrimary = Color(0xFF2B0E00),
    primaryContainer = Color(0xFF5C2200), onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFFF2B84B), onSecondary = Color(0xFF2E1F00),
    secondaryContainer = Color(0xFF3F2C0A), onSecondaryContainer = Color(0xFFFFE3A6),
    tertiary = Color(0xFFE4472C), onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFF0E0A09), onBackground = Color(0xFFF3E7E0),
    surface = Color(0xFF17100E), onSurface = Color(0xFFF3E7E0),
    surfaceVariant = Color(0xFF2A1C17), onSurfaceVariant = Color(0xFFD7C2B8),
    outline = Color(0xFF8A6B5C), outlineVariant = Color(0xFF4A342B),
    error = Color(0xFFFF8A7A), onError = Color(0xFF3B0A02),
)

private val LavaLight = lightColorScheme(
    primary = Color(0xFFC2410C), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBC8), onPrimaryContainer = Color(0xFF3A1100),
    secondary = Color(0xFF9A6A00), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE4B0), onSecondaryContainer = Color(0xFF2E1F00),
    tertiary = Color(0xFFB3261E), onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFF8F4), onBackground = Color(0xFF201A17),
    surface = Color(0xFFFFF3EC), onSurface = Color(0xFF201A17),
    surfaceVariant = Color(0xFFF4DED3), onSurfaceVariant = Color(0xFF52443C),
    outline = Color(0xFF85736B), outlineVariant = Color(0xFFD8C2B8),
    error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF),
)

private val ClassicTypography = Typography().let { t ->
    t.copy(
        headlineMedium = t.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun FloatTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) LavaDark else LavaLight
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme, typography = ClassicTypography, content = content)
    }
}
