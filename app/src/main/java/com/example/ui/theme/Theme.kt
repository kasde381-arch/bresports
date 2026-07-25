package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    tertiary = TertiaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = TextOnOrange,
    onSecondary = TextPrimary,
    onTertiary = SlateDarkBg,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = SlateDarkBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Force our esports custom dark theme for authentic experience
    content: @Composable () -> Unit
) {
    // We enforce the customized Dark theme because gaming/esports apps are traditionally dark, eye-safe, and high-impact
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
