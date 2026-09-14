package dev.mahourigan.moonwidget.ui

import dev.mahourigan.moonwidget.data.AppFont
import dev.mahourigan.moonwidget.data.ThemeColors
import dev.mahourigan.moonwidget.render.CompassRenderer
import dev.mahourigan.moonwidget.render.MoonOrbitRenderer
import dev.mahourigan.moonwidget.render.MoonRenderer
import dev.mahourigan.moonwidget.render.SkyDomeRenderer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

/**
 * The current theme's colours, in Compose form.
 *
 * The three derived values are not stored in [ThemeColors] because they are
 * never worth choosing separately: muted text that clashes with body text, or
 * a rim that does not match the Moon, are mistakes rather than options.
 */
@Immutable
data class Palette(
    val background: Color,
    val surface: Color,
    val text: Color,
    val moon: Color,
    val accent: Color,
) {
    /** Labels and secondary lines. Alpha rather than a fixed grey, so it sits on any background. */
    val muted: Color get() = text.copy(alpha = MUTED_ALPHA)

    /** The Moon's unlit side. Shares [surface] so the Moon and the cards agree. */
    val moonShadow: Color get() = surface

    /** Faint rim, so a new moon is still a visible disc rather than nothing at all. */
    val moonOutline: Color get() = moon.copy(alpha = OUTLINE_ALPHA)

    companion object {
        const val MUTED_ALPHA = 0.62f
        const val OUTLINE_ALPHA = 0.30f
    }
}

fun ThemeColors.toPalette(): Palette = Palette(
    background = Color(background),
    surface = Color(surface),
    text = Color(text),
    moon = Color(moon),
    accent = Color(accent),
)

/**
 * The same colours in the form `android.graphics` wants, for the widget's
 * bitmap. Derived here so the widget and the app can't drift apart.
 */
fun Palette.toRenderPalette(): MoonRenderer.Palette = MoonRenderer.Palette(
    lit = moon.toArgb(),
    shadow = moonShadow.toArgb(),
    outline = moonOutline.toArgb(),
    symbolOnLit = background.toArgb(),
    symbolOnShadow = moon.toArgb(),
    symbolBlended = androidx.compose.ui.graphics.lerp(moon, moonShadow, 0.5f).toArgb(),
)

/** Colours for the widget's pass-across-the-sky dome. */
fun Palette.toDomePalette(): SkyDomeRenderer.Palette = SkyDomeRenderer.Palette(
    arc = moon.copy(alpha = 0.65f).toArgb(),
    horizon = muted.toArgb(),
    marker = moon.toArgb(),
)

/**
 * Colours for the rings round the widget's Moon.
 *
 * The arc borrows the Moon's own colour the way the dome does, so the ring
 * reads as the Moon's path rather than as furniture; the markers take the
 * accent, which is what the compass dial already uses for a bearing.
 */
fun Palette.toOrbitPalette(): MoonOrbitRenderer.Palette = MoonOrbitRenderer.Palette(
    track = moon.copy(alpha = 0.55f).toArgb(),
    trackFaint = moon.copy(alpha = 0.26f).toArgb(),
    horizon = muted.copy(alpha = 0.45f).toArgb(),
    north = muted.toArgb(),
    marker = accent.toArgb(),
    ground = background.toArgb(),
)

/** Colours for the widget's compass dial, matching `ui/CompassRose.kt`. */
fun Palette.toCompassPalette(): CompassRenderer.Palette = CompassRenderer.Palette(
    ring = muted.toArgb(),
    label = text.toArgb(),
    marker = accent.toArgb(),
    // The same dimming the app uses while the Moon is down, where the dot is
    // showing where it will rise rather than where it is.
    markerDim = muted.toArgb(),
)

/**
 * Static rather than dynamic: the theme changes rarely, and a static local
 * recomposes only what reads it instead of everything below the provider.
 */
val LocalPalette = staticCompositionLocalOf { ThemeColors.MIDNIGHT.toPalette() }

/**
 * The palette in scope.
 *
 * Named so call sites read `MoonColors.background` the way they used to read
 * a constant, but now resolving through the theme.
 */
val MoonColors: Palette
    @Composable
    @ReadOnlyComposable
    get() = LocalPalette.current

/**
 * The typeface for a chosen [AppFont].
 *
 * All four are families Android resolves itself, so nothing is bundled. The
 * condensed one is asked for by device family name, which needs API 26 — the
 * app's minimum — and falls back to the default sans if a device has no such
 * family, which is the right behaviour rather than an error.
 */
fun AppFont.toFontFamily(): FontFamily = when (this) {
    AppFont.SYSTEM -> FontFamily.Default
    AppFont.SERIF -> FontFamily.Serif
    AppFont.MONOSPACE -> FontFamily.Monospace
    AppFont.CONDENSED -> FontFamily(Font(DeviceFontFamilyName("sans-serif-condensed")))
}

/** The Glance equivalent of [toFontFamily], for widget labels. */
fun AppFont.toGlanceFontFamily(): androidx.glance.text.FontFamily = when (this) {
    AppFont.SYSTEM -> androidx.glance.text.FontFamily.SansSerif
    AppFont.SERIF -> androidx.glance.text.FontFamily.Serif
    AppFont.MONOSPACE -> androidx.glance.text.FontFamily.Monospace
    // Glance has no condensed constant, but it takes a family name directly.
    AppFont.CONDENSED -> androidx.glance.text.FontFamily("sans-serif-condensed")
}
