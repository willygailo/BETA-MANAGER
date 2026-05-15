package beta.manager.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BetaColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DarkBackground,
    primaryContainer = NeonCyan.copy(alpha = 0.15f),
    onPrimaryContainer = NeonCyan,
    secondary = NeonPurple,
    onSecondary = DarkBackground,
    secondaryContainer = NeonPurple.copy(alpha = 0.15f),
    onSecondaryContainer = NeonPurple,
    tertiary = NeonPink,
    onTertiary = DarkBackground,
    tertiaryContainer = NeonPink.copy(alpha = 0.15f),
    onTertiaryContainer = NeonPink,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceTint = DarkSurface,
    error = NeonRed,
    onError = DarkBackground,
    errorContainer = NeonRed.copy(alpha = 0.15f),
    onErrorContainer = NeonRed,
    outline = CardBorder,
    outlineVariant = DividerColor,
    inverseSurface = TextPrimary,
    inverseOnSurface = DarkBackground,
    inversePrimary = NeonCyan.copy(alpha = 0.8f),
    scrim = Color(0xFF000000).copy(alpha = 0.5f)
)

@Composable
fun BetaManagerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = BetaColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as androidx.activity.ComponentActivity).window
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = BetaTypography,
        content = content
    )
}
