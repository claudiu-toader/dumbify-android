package com.werkloop.dumbify.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The Industry palette, transcribed from the design system's styles.css.
 *
 * Two grounds only — this and [IndustryDarkPalette]. Neither is derived from
 * the device: there is no Material dynamic colour anywhere in the app. See
 * design decisions 9 and 13.
 */
object IndustryPalette {
    val Bg = Color(0xFFF2F2F3)
    val Surface = Color(0xFFE9E9EA)
    val Text = Color(0xFF1D1F20)
    val Accent = Color(0xFF5980A6)
    val Accent2 = Color(0xFF728FAB)

    /** `color-mix(in srgb, #1d1f20 16%, transparent)` */
    val Divider = Text.copy(alpha = 0.16f)

    val Neutral100 = Color(0xFFF5F5F8)
    val Neutral200 = Color(0xFFE7E7EA)
    val Neutral300 = Color(0xFFD4D4D7)
    val Neutral400 = Color(0xFFB7B7BA)
    val Neutral500 = Color(0xFF98989B)
    val Neutral600 = Color(0xFF7A7A7D)
    val Neutral700 = Color(0xFF5D5D60)
    val Neutral800 = Color(0xFF424244)
    val Neutral900 = Color(0xFF2B2B2D)

    val Accent100 = Color(0xFFEEF6FF)
    val Accent200 = Color(0xFFD6EBFF)
    val Accent300 = Color(0xFFB5D9FD)
    val Accent400 = Color(0xFF94BCE3)
    val Accent500 = Color(0xFF749DC4)
    val Accent600 = Color(0xFF597EA3)
    val Accent700 = Color(0xFF416180)
    val Accent800 = Color(0xFF2C455D)
    val Accent900 = Color(0xFF1D2D3D)

    val Accent2_100 = Color(0xFFEEF6FF)
    val Accent2_200 = Color(0xFFD6EBFF)
    val Accent2_300 = Color(0xFFBDD8F2)
    val Accent2_400 = Color(0xFF9EBBD8)
    val Accent2_500 = Color(0xFF7E9CB8)
    val Accent2_600 = Color(0xFF627D98)
    val Accent2_700 = Color(0xFF486077)
    val Accent2_800 = Color(0xFF314457)
    val Accent2_900 = Color(0xFF1F2D3A)

    /** `.text-muted` — text at 55%. */
    val TextMuted = Text.copy(alpha = 0.55f)

    /**
     * The label colour on an accent fill (`--dmb-reverse-fg`). Identical under
     * both grounds, which is why it is not `Bg`: under the dark ground `Bg` is
     * near-black and would render the primary button's label invisible.
     */
    val ReverseFg = Color(0xFFF2F2F3)
}

/**
 * The dark ground, transcribed from the prototype's `themeVars` override.
 *
 * These are not the light ramp inverted. The light neutral steps are mid-greys
 * chosen to sit on a near-white page; their dark counterparts are the *text*
 * colour at decreasing opacity. The role each step plays is preserved; the
 * value is not. [IndustryPalette.Accent] and [IndustryPalette.ReverseFg] do
 * not move at all.
 */
object IndustryDarkPalette {
    val Bg = Color(0xFF16181A)
    val Surface = Color(0xFF1D1F20)
    val Text = Color(0xFFECECED)

    val Divider = Text.copy(alpha = 0.20f)
    val TextMuted = Text.copy(alpha = 0.55f)

    val Neutral200 = Text.copy(alpha = 0.14f)
    val Neutral300 = Text.copy(alpha = 0.20f)
    val Neutral400 = Text.copy(alpha = 0.34f)
    val Neutral500 = Text.copy(alpha = 0.52f)
    val Neutral600 = Text.copy(alpha = 0.60f)
    val Neutral900 = Color(0xFF0B0C0D)

    val Accent100 = IndustryPalette.Accent.copy(alpha = 0.18f)
    val Accent300 = IndustryPalette.Accent.copy(alpha = 0.45f)
    val Accent700 = Color(0xFF9CB9D4)
    val Accent900 = Color(0xFF22364A)

    /** Unchanged from the light ground — the prototype overrides neither. */
    val Accent600 = IndustryPalette.Accent600
}
