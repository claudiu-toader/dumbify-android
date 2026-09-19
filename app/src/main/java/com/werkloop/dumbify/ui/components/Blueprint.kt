package com.werkloop.dumbify.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.werkloop.dumbify.ui.theme.theme

/**
 * The blueprint frame: a hairline box with four registration marks drawn
 * *outside* its corners.
 *
 * In the CSS each mark is an 11x11 corner offset -6px from the box, holding a
 * 1px cross. That means every arm extends 6px beyond the border and 5px inside
 * it, so the marks read as survey ticks rather than as a second border.
 *
 * Nothing here clips, so callers must leave at least [MarkOverhang] of room
 * around a Blueprint or the marks will be drawn over by a neighbour.
 */
val MarkOverhang: Dp = 6.dp

@Composable
fun Blueprint(
    modifier: Modifier = Modifier,
    borderColor: Color = theme.divider,
    markColor: Color = theme.text.copy(alpha = 0.55f),
    showMarks: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .border(1.dp, borderColor)
            .then(if (showMarks) Modifier.registrationMarks(markColor) else Modifier),
        content = content,
    )
}

private fun Modifier.registrationMarks(color: Color): Modifier = drawWithContent {
    drawContent()
    val out = 6.dp.toPx()
    val inn = 5.dp.toPx()
    val w = 1.dp.toPx()
    val corners = listOf(
        Offset(0f, 0f),
        Offset(size.width, 0f),
        Offset(0f, size.height),
        Offset(size.width, size.height),
    )
    corners.forEach { c ->
        // Vertical arm of the cross, spanning the corner.
        drawLine(color, Offset(c.x, c.y - out), Offset(c.x, c.y + inn), strokeWidth = w)
        // Horizontal arm.
        drawLine(color, Offset(c.x - out, c.y), Offset(c.x + inn, c.y), strokeWidth = w)
    }
}
