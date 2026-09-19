package com.werkloop.dumbify.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.werkloop.dumbify.R

/** Barlow Condensed — headings, app names, buttons, numerals. */
val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_regular, FontWeight.Normal),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

/** Barlow — body copy. */
val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_bold, FontWeight.Bold),
)

/**
 * Type styles beyond Material's slots. The design leans on a monospace
 * "technical label" voice that Material has no name for, and on heading sizes
 * (46sp/62sp display numerals) that sit outside the standard ramp.
 */
object DumbType {
    /** h1 — 42sp, per styles.css. */
    val H1 = TextStyle(
        fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold,
        fontSize = 42.sp, lineHeight = 47.sp, letterSpacing = (-0.015).em,
    )
    val H2 = TextStyle(
        fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.015).em,
    )
    val H3 = TextStyle(
        fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp, lineHeight = 28.sp, letterSpacing = (-0.015).em,
    )

    /** The welcome screen's 46sp/0.94 display setting. */
    val Display = TextStyle(
        fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold,
        fontSize = 46.sp, lineHeight = 43.sp, letterSpacing = (-0.02).em,
    )

    /** The launcher clock — 62sp/0.9, the largest thing on the surface. */
    val Clock = TextStyle(
        fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold,
        fontSize = 62.sp, lineHeight = 56.sp, letterSpacing = (-0.03).em,
    )

    /** A launcher app name — 27sp uppercase. */
    val LauncherApp = TextStyle(
        fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold,
        fontSize = 27.sp, lineHeight = 27.sp, letterSpacing = 0.02.em,
    )

    /** A row heading inside a picker or settings list — 15/16sp uppercase. */
    val RowTitle = TextStyle(
        fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 19.sp, letterSpacing = 0.02.em,
    )

    /** Body copy — Barlow 13sp, the design's dense reading size. */
    val Body = TextStyle(
        fontFamily = Barlow, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 20.sp,
    )

    /** Body copy at the design system's default 15sp. */
    val BodyLarge = TextStyle(
        fontFamily = Barlow, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 23.sp,
    )

    /** Button and segmented-control labels. */
    val Button = TextStyle(
        fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 17.sp,
    )

    /**
     * The monospace technical voice — step counters, category tags, axis
     * labels. Deliberately small, and never used for body copy, which the
     * design-system spec floors at 12sp.
     */
    val Mono = TextStyle(
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal,
        fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 0.12.em,
    )
    /**
     * Untracked by default — the design only widens the mono voice on section
     * labels ("REPEAT", "LAST 7 DAYS"), never on values like "12m", where
     * tracking makes the number read as two words.
     */
    val MonoSmall = TextStyle(
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal,
        fontSize = 9.sp, lineHeight = 11.sp, letterSpacing = 0.sp,
    )

    /** The accent kicker above a title — "DUMBIFY / SETUP 0.1". */
    val Kicker = TextStyle(
        fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp, lineHeight = 10.sp, letterSpacing = 0.22.em,
    )
}

/** Material's slots, filled with the Industry faces for M3 components. */
val DumbifyTypography = Typography(
    displayLarge = DumbType.Display,
    headlineLarge = DumbType.H1,
    headlineMedium = DumbType.H2,
    headlineSmall = DumbType.H3,
    titleMedium = DumbType.RowTitle,
    bodyLarge = DumbType.BodyLarge,
    bodyMedium = DumbType.Body,
    labelLarge = DumbType.Button,
    labelSmall = DumbType.Mono,
)
