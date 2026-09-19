package com.werkloop.dumbify.ui.theme

import androidx.compose.ui.unit.dp

/**
 * The Industry spacing scale, kept at its exact fractional values rather than
 * rounded to a 4dp grid — see design decision 9. The dense list rhythm the
 * design depends on does not survive rounding.
 */
object Spacing {
    val s1 = 3.4.dp
    val s2 = 6.8.dp
    val s3 = 10.2.dp
    val s4 = 13.6.dp
    val s6 = 20.4.dp
    val s8 = 27.2.dp
}

/** Minimum touch target required by the design-system spec. */
val MinTouchTarget = 44.dp
