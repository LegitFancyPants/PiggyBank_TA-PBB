package com.example.piggybank.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    onPrimary = Color.White,
    primaryContainer = SoftMint,
    onPrimaryContainer = DarkTeal,
    secondary = DarkTeal,
    onSecondary = Color.White,
    secondaryContainer = MintSurfaceTint,
    onSecondaryContainer = PrimaryTeal,
    tertiary = IncomeGreen,
    onTertiary = Color.White,
    tertiaryContainer = IncomeTint,
    onTertiaryContainer = IncomeGreen,
    background = BackgroundCanvas,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = MintSurfaceTint,
    onSurfaceVariant = TextSecondary,
    outline = OutlineGray,
    outlineVariant = CardBorder,
    error = ExpenseRed,
    onError = Color.White,
    errorContainer = ExpenseTint,
    onErrorContainer = ExpenseRed
)

@Composable
fun PiggyBankTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
