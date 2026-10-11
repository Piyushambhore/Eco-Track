package com.ecotrack.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = BrightCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = PurpleAccent,
    background = DarkBgPrimary,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkBgSecondary,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkBgCard,
    onSurfaceVariant = TextMuted,
    error = RoseError,
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = EcoBrand,
    onPrimary = EcoBrandDark,
    primaryContainer = EcoTint,
    onPrimaryContainer = EcoBrandDark,
    secondary = EcoBrandDark,
    onSecondary = Color.White,
    secondaryContainer = EcoBadgeNeutral,
    onSecondaryContainer = EcoText,
    tertiary = EcoSuccess,
    background = EcoBg,
    onBackground = EcoText,
    surface = EcoCard,
    onSurface = EcoText,
    surfaceVariant = EcoBadgeNeutral,
    onSurfaceVariant = EcoMuted,
    outline = EcoBorder,
    error = EcoWarning,
    onError = Color.White
)

@Composable
fun EcoTrackTheme(
    darkTheme: Boolean = false, // ui.md: Light mode design system
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
        content = content
    )
}
