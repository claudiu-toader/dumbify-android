package com.werkloop.dumbify.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The Industry ramp, carried in a CompositionLocal because Material's
 * ColorScheme has no slot for a nine-step neutral ramp alongside a nine-step
 * accent ramp — see design decision 9.
 *
 * These are token *names*. The two grounds — [IndustryLight] and
 * [IndustryDark] — are two sets of values behind them, so no screen or
 * component ever branches on which one is in effect (design decision 13).
 */
@Immutable
data class Industry(
    val bg: Color,
    val surface: Color,
    val text: Color,
    val textMuted: Color,
    val divider: Color,
    val accent: Color,
    val accent100: Color,
    val accent300: Color,
    val accent600: Color,
    val accent700: Color,
    val accent900: Color,
    val neutral200: Color,
    val neutral300: Color,
    val neutral400: Color,
    val neutral500: Color,
    val neutral600: Color,
    val neutral900: Color,
    /**
     * The label on an accent fill. Fixed under both grounds — under the light
     * ground it happens to equal [bg], under the dark ground it emphatically
     * does not.
     */
    val reverseFg: Color,
    /** True when the dark ground is in effect. For window chrome, not for UI. */
    val isDark: Boolean,
)

val IndustryLight = Industry(
    bg = IndustryPalette.Bg,
    surface = IndustryPalette.Surface,
    text = IndustryPalette.Text,
    textMuted = IndustryPalette.TextMuted,
    divider = IndustryPalette.Divider,
    accent = IndustryPalette.Accent,
    accent100 = IndustryPalette.Accent100,
    accent300 = IndustryPalette.Accent300,
    accent600 = IndustryPalette.Accent600,
    accent700 = IndustryPalette.Accent700,
    accent900 = IndustryPalette.Accent900,
    neutral200 = IndustryPalette.Neutral200,
    neutral300 = IndustryPalette.Neutral300,
    neutral400 = IndustryPalette.Neutral400,
    neutral500 = IndustryPalette.Neutral500,
    neutral600 = IndustryPalette.Neutral600,
    neutral900 = IndustryPalette.Neutral900,
    reverseFg = IndustryPalette.ReverseFg,
    isDark = false,
)

val IndustryDark = Industry(
    bg = IndustryDarkPalette.Bg,
    surface = IndustryDarkPalette.Surface,
    text = IndustryDarkPalette.Text,
    textMuted = IndustryDarkPalette.TextMuted,
    divider = IndustryDarkPalette.Divider,
    accent = IndustryPalette.Accent,
    accent100 = IndustryDarkPalette.Accent100,
    accent300 = IndustryDarkPalette.Accent300,
    accent600 = IndustryDarkPalette.Accent600,
    accent700 = IndustryDarkPalette.Accent700,
    accent900 = IndustryDarkPalette.Accent900,
    neutral200 = IndustryDarkPalette.Neutral200,
    neutral300 = IndustryDarkPalette.Neutral300,
    neutral400 = IndustryDarkPalette.Neutral400,
    neutral500 = IndustryDarkPalette.Neutral500,
    neutral600 = IndustryDarkPalette.Neutral600,
    neutral900 = IndustryDarkPalette.Neutral900,
    reverseFg = IndustryPalette.ReverseFg,
    isDark = true,
)

val LocalIndustry = staticCompositionLocalOf { IndustryLight }

/**
 * Two colour schemes, chosen by [darkTheme].
 *
 * `null` — the default — means "follow the device", which is what the settings
 * switch reports until the user touches it. Once touched it writes a non-null
 * value and the device setting no longer overrides it. There is no third
 * option and no dynamic colour.
 */
@Composable
fun DumbifyTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val dark = darkTheme ?: isSystemInDarkTheme()
    val industry = if (dark) IndustryDark else IndustryLight
    val scheme = if (dark) {
        darkColorScheme(
            primary = industry.accent,
            onPrimary = industry.reverseFg,
            secondary = industry.accent600,
            onSecondary = industry.reverseFg,
            background = industry.bg,
            onBackground = industry.text,
            surface = industry.bg,
            onSurface = industry.text,
            surfaceVariant = industry.surface,
            onSurfaceVariant = industry.text,
            outline = industry.divider,
            error = industry.accent700,
        )
    } else {
        lightColorScheme(
            primary = industry.accent,
            onPrimary = industry.reverseFg,
            secondary = industry.accent600,
            onSecondary = industry.reverseFg,
            background = industry.bg,
            onBackground = industry.text,
            surface = industry.bg,
            onSurface = industry.text,
            surfaceVariant = industry.surface,
            onSurfaceVariant = industry.text,
            outline = industry.divider,
            error = industry.accent900,
        )
    }
    CompositionLocalProvider(LocalIndustry provides industry) {
        MaterialTheme(
            colorScheme = scheme,
            typography = DumbifyTypography,
            content = content,
        )
    }
}

/** Shorthand for the ramp at a call site: `theme.accent700`. */
val theme: Industry
    @Composable get() = LocalIndustry.current
