package com.licount.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils

private val LocalDarkTheme = staticCompositionLocalOf { false }

private fun seedColorScheme(seed: Long, darkTheme: Boolean): ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed.toInt(), hsl)
    val hue = hsl[0]
    val saturation = hsl[1].coerceAtMost(0.85f)
    val tertiaryHue = (hue + 60f) % 360f

    fun tone(h: Float, s: Float, l: Float): Color =
        Color(ColorUtils.HSLToColor(floatArrayOf(h, s, l)))

    fun neutral(light: Float, dark: Float): Color =
        tone(hue, saturation * 0.08f, if (darkTheme) dark else light)

    val darkOnAccent = tone(hue, saturation * 0.08f, 0.10f)
    val onAccent = if (darkTheme) darkOnAccent else Color.White

    fun accent(h: Float, s: Float, dark: Boolean = darkTheme): Color {
        val foreground = if (dark) darkOnAccent else Color.White
        var lightness = if (dark) 0.78f else hsl[2].coerceIn(0.42f, 0.55f)
        var color = tone(h, s, lightness)
        while (ColorUtils.calculateContrast(color.toArgb(), foreground.toArgb()) < 4.5 &&
            lightness > 0f && lightness < 1f
        ) {
            lightness = (lightness + if (dark) 0.01f else -0.01f).coerceIn(0f, 1f)
            color = tone(h, s, lightness)
        }
        return color
    }

    val primary = accent(hue, saturation)
    val secondary = accent(hue, saturation * 0.35f)
    val tertiary = accent(tertiaryHue, saturation * 0.70f)
    val onContainer = neutral(0.10f, 0.92f)
    val background = neutral(0.965f, 0.055f)
    val surface = if (darkTheme) neutral(1f, 0.09f) else Color.White
    val onSurface = neutral(0.105f, 0.92f)
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()

    return base.copy(
        primary = primary,
        onPrimary = onAccent,
        primaryContainer = tone(hue, saturation * 0.60f, if (darkTheme) 0.26f else 0.93f),
        onPrimaryContainer = onContainer,
        inversePrimary = accent(hue, saturation, !darkTheme),
        secondary = secondary,
        onSecondary = onAccent,
        secondaryContainer = tone(hue, saturation * 0.25f, if (darkTheme) 0.24f else 0.93f),
        onSecondaryContainer = onContainer,
        tertiary = tertiary,
        onTertiary = onAccent,
        tertiaryContainer = tone(tertiaryHue, saturation * 0.45f, if (darkTheme) 0.25f else 0.93f),
        onTertiaryContainer = onContainer,
        background = background,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = neutral(0.92f, 0.15f),
        onSurfaceVariant = neutral(0.36f, 0.73f),
        surfaceTint = primary,
        inverseSurface = neutral(0.18f, 0.90f),
        inverseOnSurface = neutral(0.95f, 0.16f),
        error = if (darkTheme) Color(0xFFFFB4AB) else Color(0xFFBA1A1A),
        onError = if (darkTheme) Color(0xFF690005) else Color.White,
        errorContainer = if (darkTheme) Color(0xFF93000A) else Color(0xFFFFDAD6),
        onErrorContainer = if (darkTheme) Color(0xFFFFDAD6) else Color(0xFF410002),
        outline = neutral(0.47f, 0.58f),
        outlineVariant = neutral(0.84f, 0.25f),
        scrim = Color.Black,
        surfaceBright = neutral(1f, 0.20f),
        surfaceDim = neutral(0.87f, 0.055f),
        surfaceContainerLowest = neutral(1f, 0.035f),
        surfaceContainerLow = neutral(0.985f, 0.075f),
        surfaceContainer = neutral(0.97f, 0.10f),
        surfaceContainerHigh = neutral(0.95f, 0.125f),
        surfaceContainerHighest = neutral(0.93f, 0.15f)
    )
}

/** 供卡片阴影等处读取当前是否深色主题。 */
@Composable
fun rememberIsDarkTheme(): Boolean = LocalDarkTheme.current

/** 轻记主题：FlClash 风格，浅色背景/白色表面，或深色背景/略亮表面。 */
@Composable
fun LiCountTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    seed: Long = 0xFF249B76L,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> remember(seed, darkTheme) { seedColorScheme(seed, darkTheme) }
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
