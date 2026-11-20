package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val TindPhotoDarkColorScheme = darkColorScheme(
    primary = FlameCoral,
    onPrimary = Color.White,
    primaryContainer = FlameContainerDark,
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = KeepEmerald,
    onSecondary = Color(0xFF042619),
    secondaryContainer = KeepEmeraldDark,
    onSecondaryContainer = KeepEmeraldLight,
    tertiary = FlameOrange,
    onTertiary = Color.White,
    error = TrashCrimson,
    onError = Color.White,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = StudioSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = StudioSurface,
    surfaceContainer = StudioSurfaceElevated,
    surfaceContainerHigh = StudioSurfaceHighest,
    outline = StudioOutlineDark,
    outlineVariant = Color(0xFF282836),
)

private val TindPhotoLightColorScheme = lightColorScheme(
    primary = FlameCoral,
    onPrimary = Color.White,
    primaryContainer = FlameContainerLight,
    onPrimaryContainer = Color(0xFF6E001A),
    secondary = Color(0xFF009B64),
    onSecondary = Color.White,
    secondaryContainer = KeepEmeraldLight,
    onSecondaryContainer = Color(0xFF003822),
    tertiary = FlameOrange,
    onTertiary = Color.White,
    error = TrashCrimson,
    onError = Color.White,
    background = AlabasterBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = AlabasterBg,
    surfaceContainer = LightSurfaceElevated,
    surfaceContainerHigh = LightSurfaceHighest,
    outline = StudioOutlineLight,
    outlineVariant = Color(0xFFEBE4E6),
)

val TindPhotoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun TindPhotoTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) TindPhotoDarkColorScheme else TindPhotoLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TindPhotoTypography,
        shapes = TindPhotoShapes,
        content = content,
    )
}
