package com.werkloop.dumbify.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.werkloop.dumbify.ui.theme.theme

/**
 * A framed information panel: a hairline box, square-cornered, on the page
 * ground.
 *
 * **No registration marks, against the prototype.** `Dumbify.dc.html` draws
 * four corner crosses outside every `.blueprint` panel — an 11×11 mark offset
 * -6px, so each arm extends past the border and reads as a survey tick. This
 * implemented them faithfully and they are now gone everywhere, by decision:
 * at phone size they read as crosses stuck to the corners rather than as
 * draughting annotation, and eight marked panels turn a deliberately quiet
 * surface into a noisy one.
 *
 * The drawing code is deleted rather than left behind a `showMarks = false`
 * default, for the same reason the button variant was (design-system "Buttons
 * carry no registration marks"): a parameter nothing sets is how the marks
 * would drift back.
 *
 * What the frame still is: the one thing that says "this is a panel, not
 * content". Eight call sites rely on it for that, and on the border token.
 */
@Composable
fun Blueprint(
    modifier: Modifier = Modifier,
    borderColor: Color = theme.divider,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.border(1.dp, borderColor),
        content = content,
    )
}
