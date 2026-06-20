@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package org.cycb.canvas.ui.theme

import android.app.Activity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,

    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,

    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,

    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,

    background = Background,
    onBackground = OnBackground,

    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,

    outline = Outline,
    outlineVariant = OutlineVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,

    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,

    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,

    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,

    background = BackgroundDark,
    onBackground = OnBackgroundDark,

    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,

    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

@Composable
fun CYCBChatTheme(
    settingsViewModel: org.cycb.canvas.viewmodel.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val customThemesPrefs = remember { org.cycb.canvas.data.preferences.CustomThemesPreferences(context) }
    val customThemes by customThemesPrefs.customThemes.collectAsState(initial = emptyList())

    val darkMode by settingsViewModel.darkMode.collectAsState()
    val dynamicColors by settingsViewModel.dynamicColors.collectAsState()
    val selectedThemeName by settingsViewModel.selectedTheme.collectAsState()
    val highContrast by settingsViewModel.highContrast.collectAsState()
    val largeText by settingsViewModel.largeText.collectAsState()

    val darkTheme = darkMode

    val selectedTheme = remember(selectedThemeName, customThemes) {
        customThemes.find { it.name == selectedThemeName } ?: getThemeByName(selectedThemeName)
    }

    var colorScheme = when {
        dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> selectedTheme.darkScheme
        else -> selectedTheme.lightScheme
    }

    if (highContrast) {
        colorScheme = colorScheme.copy(
            surface = if (darkTheme) Color(0xFF000000) else Color(0xFFFFFFFF),
            background = if (darkTheme) Color(0xFF000000) else Color(0xFFFFFFFF),
            primary = if (darkTheme) Color(0xFFFFFFFF) else Color(0xFF000000),
            onPrimary = if (darkTheme) Color(0xFF000000) else Color(0xFFFFFFFF),
            onSurface = if (darkTheme) Color(0xFFFFFFFF) else Color(0xFF000000),
            onBackground = if (darkTheme) Color(0xFFFFFFFF) else Color(0xFF000000)
        )
    }
    
    // Fix pure white background issue: If background is pure white (#FFFFFF or #FFFBFF)
    // and dynamic colors are off or not available, use a tinted version
    // based on the primary color if we are in light mode.
    if (!darkTheme && !dynamicColors && 
        (colorScheme.background == Color(0xFFFFFFFF) || colorScheme.background == Color(0xFFFFFBFF))) {
        colorScheme = colorScheme.copy(
            background = colorScheme.primaryContainer.copy(alpha = 0.05f),
            surface = colorScheme.primaryContainer.copy(alpha = 0.05f)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = if (largeText) getLargeTypography(Typography) else Typography,
        shapes = Shapes,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}
