package com.werkloop.dumbify.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.werkloop.dumbify.ui.theme.DumbType
import com.werkloop.dumbify.ui.theme.Spacing
import com.werkloop.dumbify.ui.theme.theme

enum class ButtonVariant { Primary, Secondary, Ghost }

/**
 * The one button in the system. Square, hairline-bordered, condensed label.
 *
 * **No registration marks.** In the design the corner marks belong to
 * `.blueprint` *panels* — the before/after comparison, the resulting-window
 * bar, the widget preview, the summary table, the statistics figures and the
 * dialog. Not one `<button>` in the prototype carries them. A button wearing
 * survey ticks reads as a framed object rather than as something to press, and
 * it competes with the six panels that are meant to be the only marked things
 * on screen. The parameter that used to allow it is gone rather than defaulted
 * off, so the drift cannot come back.
 *
 * The touch target is floored at 44dp via [minimumInteractiveComponentSize],
 * which grows the *target* without growing the drawn box — the design-system
 * spec asks for exactly that, and several buttons here are drawn at 26–30dp.
 */
@Composable
fun DumbButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true,
    /** Stretch the drawn box to the full width — the `.btn-block` treatment. */
    fillWidth: Boolean = false,
    textStyle: TextStyle = DumbType.Button,
    contentPadding: PaddingValues? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val fill: Color
    val content: Color
    val border: Color
    when (variant) {
        ButtonVariant.Primary -> {
            fill = if (pressed) theme.accent700 else theme.accent
            // Not `bg`: the label sits on the accent fill, so it follows the
            // reverse token, which does not move with the ground.
            content = theme.reverseFg
            border = theme.accent
        }
        ButtonVariant.Secondary -> {
            fill = if (pressed) theme.text.copy(alpha = 0.14f) else Color.Transparent
            content = theme.text
            border = theme.divider
        }
        ButtonVariant.Ghost -> {
            fill = if (pressed) theme.accent.copy(alpha = 0.18f) else Color.Transparent
            content = theme.accent
            border = Color.Transparent
        }
    }

    // `.btn-ghost` narrows its inline padding to `--space-1` so a ghost action
    // hugs its label instead of sitting in an invisible box.
    val padding = contentPadding ?: when (variant) {
        ButtonVariant.Ghost -> PaddingValues(horizontal = Spacing.s1, vertical = Spacing.s2)
        else -> PaddingValues(horizontal = ButtonInlinePadding, vertical = Spacing.s2)
    }

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .minimumInteractiveComponentSize()
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val widthMod = if (fillWidth) Modifier.fillMaxWidth() else Modifier
        Box(
            modifier = widthMod.then(
                if (variant == ButtonVariant.Ghost) Modifier
                else Modifier.border(BorderStroke(1.dp, border), RectangleShape)
            ),
        ) {
            Box(
                modifier = widthMod
                    .background(fill, RectangleShape)
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = textStyle, color = content, textAlign = TextAlign.Center)
            }
        }
    }
}

/** `padding: var(--space-2) calc(var(--space-3) * 1.2)` — the `.btn` default. */
private val ButtonInlinePadding = 12.24.dp

/** The type every full-size action uses: 15sp condensed, wide-tracked, uppercase. */
private val ActionText = DumbType.Button.copy(fontSize = 15.sp, letterSpacing = 0.1.em)

/** `padding: var(--space-4)` on all four sides, as the design's footers set it. */
private val ActionPadding = PaddingValues(Spacing.s4)

/** A full-width primary action — the `.btn-block` of the design system. */
@Composable
fun DumbBlockButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true,
) {
    DumbButton(
        label = label,
        onClick = onClick,
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
        variant = variant,
        enabled = enabled,
        fillWidth = true,
        textStyle = ActionText,
        contentPadding = ActionPadding,
    )
}

/**
 * The `.btn-ghost.btn-icon` of the design system: a 44x44 square holding an
 * 18dp stroked icon, no border and no fill.
 *
 * The only icon button in the app, and deliberately so — the launcher's
 * settings affordance is the one place the design admits a glyph, and it is
 * header chrome rather than part of the list (`dumb-launcher` "The list is
 * typographic"). [contentDescription] is required, not defaulted: an unlabelled
 * icon is the one control on this surface a screen reader cannot read from its
 * own text.
 */
@Composable
fun DumbIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = theme.neutral600,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier = modifier
            .size(IconButtonSize)
            .background(
                if (pressed) theme.accent.copy(alpha = 0.18f) else Color.Transparent,
                RectangleShape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(IconSize),
        )
    }
}

/** `width:44px;height:44px` — the design's own icon-button box, and the floor. */
private val IconButtonSize = 44.dp

/** `<svg width="18" height="18">`. */
private val IconSize = 18.dp

/**
 * The setup footer: Back beside a stretching primary action.
 *
 * Five of the six setup screens end in exactly this row, with exactly this
 * treatment — `flex:none` on Back, `flex:1` on Continue, `--space-4` padding
 * and a 15px tracked label on both. Encoding it once is what stops the five
 * from drifting apart the next time one of them is edited.
 */
@Composable
fun SetupFooter(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    backLabel: String = "BACK",
    continueLabel: String = "CONTINUE",
    continueEnabled: Boolean = true,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        DumbButton(
            label = backLabel,
            onClick = onBack,
            variant = ButtonVariant.Secondary,
            textStyle = ActionText,
            contentPadding = ActionPadding,
        )
        DumbButton(
            label = continueLabel,
            onClick = onContinue,
            modifier = Modifier.weight(1f),
            enabled = continueEnabled,
            fillWidth = true,
            textStyle = ActionText,
            contentPadding = ActionPadding,
        )
    }
}
