package com.ashish.personalshopperagent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NebulaPurple,
    onPrimary = TextBright,
    primaryContainer = SpaceCard,
    onPrimaryContainer = TextBright,
    secondary = Emerald,
    onSecondary = DeepSpace,
    tertiary = GoldenStar,
    background = DeepSpace,
    surface = SpaceSurface,
    onBackground = TextBright,
    onSurface = TextBright,
    surfaceVariant = SpaceCard,
    outline = TextDim
)

@Composable
fun PersonalShopperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
