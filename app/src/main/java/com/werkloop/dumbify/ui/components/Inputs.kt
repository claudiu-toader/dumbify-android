package com.werkloop.dumbify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.werkloop.dumbify.ui.theme.DumbType
import com.werkloop.dumbify.ui.theme.MinTouchTarget
import com.werkloop.dumbify.ui.theme.Spacing
import com.werkloop.dumbify.ui.theme.theme

/** `.input` — square, hairline, on the surface tone. */
@Composable
fun DumbTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MinTouchTarget)
            .background(theme.surface, RectangleShape)
            .border(1.dp, theme.divider, RectangleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        textStyle = LocalTextStyle.current.merge(
            DumbType.BodyLarge.copy(fontSize = 14.sp, color = theme.text)
        ),
        singleLine = true,
        cursorBrush = SolidColor(theme.accent),
        decorationBox = { field ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = DumbType.BodyLarge.copy(fontSize = 14.sp),
                        color = theme.textMuted,
                    )
                }
                field()
            }
        },
    )
}

/** `.seg` — a two-or-more option segmented control. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, theme.divider, RectangleShape),
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = MinTouchTarget)
                    .then(if (i > 0) Modifier.leftHairline(theme.divider) else Modifier)
                    .background(if (selected) theme.accent else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = Spacing.s3, horizontal = Spacing.s2),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label.uppercase(),
                    style = DumbType.Button.copy(letterSpacing = 0.12.em),
                    color = if (selected) theme.reverseFg else theme.text,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** The 1px rule between adjacent segmented-control options. */
private fun Modifier.leftHairline(color: Color) = drawBehind {
    drawRect(color, size = Size(1.dp.toPx(), size.height))
}

/**
 * The settings switch — a 42x22 square track with a 16dp knob, drawn from the
 * tokens rather than Material's pill.
 */
@Composable
fun DumbSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(MinTouchTarget)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .width(42.dp)
                .height(22.dp)
                .background(if (checked) theme.accent else Color.Transparent, RectangleShape)
                .border(1.dp, if (checked) theme.accent else theme.neutral400, RectangleShape)
                .padding(2.dp),
            horizontalArrangement = if (checked) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(16.dp)
                    .background(if (checked) theme.reverseFg else theme.neutral500, RectangleShape)
            )
        }
    }
}

/** The allowlist checkbox — a 16dp square that fills with accent when on. */
@Composable
fun DumbCheckbox(checked: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(16.dp)
            .background(if (checked) theme.accent else Color.Transparent, RectangleShape)
            .border(1.dp, if (checked) theme.accent else theme.neutral400, RectangleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            // The CSS builds the inner mark from stacked inset shadows: a ring
            // of background inside the accent fill. This one really is `bg` —
            // it reads as a hole punched through to the page, and follows the
            // ground with it.
            Box(
                Modifier
                    .size(10.dp)
                    .background(theme.bg, RectangleShape),
            ) {
                Box(Modifier.size(10.dp).padding(3.dp).background(theme.accent, RectangleShape))
            }
        }
    }
}
