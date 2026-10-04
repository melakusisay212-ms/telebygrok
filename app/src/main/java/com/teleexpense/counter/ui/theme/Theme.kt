package com.teleexpense.counter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val EthioGreen = Color(0xFF008C45)
val EthioGreenDark = Color(0xFF006B35)
val EthioYellow = Color(0xFFFCDD09)
val SoftBackground = Color(0xFFF5F9F6)
val TextPrimary = Color(0xFF1A1A1A)
val TextSecondary = Color(0xFF5A6B5F)

private val Light = lightColorScheme(
    primary = EthioGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4F0E0),
    secondary = EthioYellow,
    background = SoftBackground,
    onBackground = TextPrimary,
    surface = Color.White,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFE8F2EC),
    onSurfaceVariant = TextSecondary
)

private val Dark = darkColorScheme(
    primary = Color(0xFF4CAF7A),
    onPrimary = Color.Black,
    background = Color(0xFF0F1A14),
    onBackground = Color(0xFFE8F2EC),
    surface = Color(0xFF1A2A20),
    onSurface = Color(0xFFE8F2EC)
)

@Composable
fun TeleExpenseTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) Dark else Light,
        typography = Typography(),
        content = content
    )
}
