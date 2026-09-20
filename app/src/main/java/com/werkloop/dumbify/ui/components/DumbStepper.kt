package com.werkloop.dumbify.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.werkloop.dumbify.ui.theme.DumbType
import com.werkloop.dumbify.ui.theme.Spacing
import com.werkloop.dumbify.ui.theme.theme

/**
 * A whole-number control: minus, the value, plus.
 *
 * The one numeric input in the app, and a stepper rather than a slider or a
 * text field because the range is small and every value is a deliberate choice
 * — dragging past a number you meant is exactly the wrong feel for a cap.
 *
 * [min] is a live floor, not a constant: the app cap cannot be lowered below
 * what is already allowed, so the minus arm disables as the allowlist grows
 * (app-allowlist "The cap is never enforced by removal").
 */
@Composable
fun DumbStepper(
    value: Int,
    onChange: (Int) -> Unit,
    min: Int,
    max: Int,
    modifier: Modifier = Modifier,
    label: String = "Value",
) {
    val canDecrease = value > min
    val canIncrease = value < max

    Row(
        modifier = modifier.semantics { contentDescription = "$label, $value" },
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DumbButton(
            label = "−",
            onClick = { if (canDecrease) onChange(value - 1) },
            enabled = canDecrease,
            variant = ButtonVariant.Secondary,
            textStyle = DumbType.Button.copy(fontSize = 16.sp),
            contentPadding = PaddingValues(horizontal = Spacing.s3, vertical = Spacing.s1),
        )
        Text(
            value.toString(),
            style = DumbType.H2.copy(fontSize = 22.sp),
            color = theme.text,
            textAlign = TextAlign.Center,
            // Fixed width so the row does not jump when 9 becomes 10.
            modifier = Modifier.widthIn(min = 32.dp),
        )
        DumbButton(
            label = "+",
            onClick = { if (canIncrease) onChange(value + 1) },
            enabled = canIncrease,
            variant = ButtonVariant.Secondary,
            textStyle = DumbType.Button.copy(fontSize = 16.sp),
            contentPadding = PaddingValues(horizontal = Spacing.s3, vertical = Spacing.s1),
        )
    }
}
