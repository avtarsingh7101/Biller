package com.avtar.cabbilling.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.avtar.cabbilling.data.model.AppTheme

// ----- Clean Light -------------------------------------------------------------
private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E7FF), onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF0E7C66), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFF3EA), onSecondaryContainer = Color(0xFF05372C),
    tertiary = Color(0xFFB45309), onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F7FB), onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFEEF1F6), onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFFCBD5E1), outlineVariant = Color(0xFFE5E7EB),
    error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B)
)

// ----- Classic Dark ------------------------------------------------------------
private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC), onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF312E81), onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF5EEAD4), onSecondary = Color(0xFF04312A),
    secondaryContainer = Color(0xFF0B5A4B), onSecondaryContainer = Color(0xFFCFF3EA),
    tertiary = Color(0xFFFBBF77), onTertiary = Color(0xFF3A2000),
    background = Color(0xFF0F1420), onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF151B29), onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF1F2937), onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF374151), outlineVariant = Color(0xFF283040),
    error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18), onErrorContainer = Color(0xFFF9DEDC)
)

// ----- Vibrant Gold (warm dark) ------------------------------------------------
private val GoldColors = darkColorScheme(
    primary = Color(0xFFE7B84B), onPrimary = Color(0xFF241B00),
    primaryContainer = Color(0xFF4A3A08), onPrimaryContainer = Color(0xFFFCE9B8),
    secondary = Color(0xFFD6B25A), onSecondary = Color(0xFF2A2100),
    secondaryContainer = Color(0xFF3E3410), onSecondaryContainer = Color(0xFFF3E4B8),
    tertiary = Color(0xFFCBB486), onTertiary = Color(0xFF2A2100),
    background = Color(0xFF14110A), onBackground = Color(0xFFF3EAD6),
    surface = Color(0xFF1C1810), onSurface = Color(0xFFF3EAD6),
    surfaceVariant = Color(0xFF2A2418), onSurfaceVariant = Color(0xFFCDBE9A),
    outline = Color(0xFF5A4E33), outlineVariant = Color(0xFF3A3220),
    error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18), onErrorContainer = Color(0xFFF9DEDC)
)

// ----- Classic Blue (crisp light) ----------------------------------------------
private val BlueColors = lightColorScheme(
    primary = Color(0xFF1D4ED8), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBEAFE), onPrimaryContainer = Color(0xFF0B1D51),
    secondary = Color(0xFF0369A1), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F2FE), onSecondaryContainer = Color(0xFF082F49),
    tertiary = Color(0xFF7C3AED), onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF1F5FD), onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0), onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8), outlineVariant = Color(0xFFDCE3EC),
    error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B)
)

/**
 * Applies the chosen [appTheme]. [AppTheme.AUTO] mirrors the device's system
 * dark/light setting via [isSystemInDarkTheme]; the others force a palette.
 */
@Composable
fun CabBillingTheme(
    appTheme: AppTheme,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()

    val darkTheme = when (appTheme) {
        AppTheme.AUTO -> systemDark
        AppTheme.CLASSIC_DARK, AppTheme.VIBRANT_GOLD -> true
        AppTheme.CLEAN_LIGHT, AppTheme.CLASSIC_BLUE -> false
    }

    val colorScheme = when (appTheme) {
        AppTheme.AUTO -> if (systemDark) DarkColors else LightColors
        AppTheme.CLASSIC_DARK -> DarkColors
        AppTheme.CLEAN_LIGHT -> LightColors
        AppTheme.VIBRANT_GOLD -> GoldColors
        AppTheme.CLASSIC_BLUE -> BlueColors
    }

    val view = LocalView.current
    val activity = view.context as? Activity
    if (activity != null && !view.isInEditMode) {
        SideEffect {
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
