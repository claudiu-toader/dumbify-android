package com.werkloop.dumbify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.werkloop.dumbify.ui.components.Blueprint
import com.werkloop.dumbify.ui.components.SetupFooter
import com.werkloop.dumbify.ui.theme.DumbType
import com.werkloop.dumbify.ui.theme.Spacing
import com.werkloop.dumbify.ui.theme.theme

@Immutable
data class WidgetPreviewRow(val name: String, val meta: String)

@Immutable
data class WidgetSetupUiState(
    val dateLine: String,
    val clock: String,
    /** The user's own allowed apps — never a mock list. */
    val preview: List<WidgetPreviewRow>,
)

/**
 * Step 4 of setup.
 *
 * Retitled from the prototype's "LOCK SCREEN": Android has had no third-party
 * lock-screen widget since API 21, and the placement steps below are the
 * Android ones, not the iOS ones the design still carries (design decision 2).
 */
@Composable
fun WidgetSetupScreen(
    state: WidgetSetupUiState,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(Spacing.s6)) {
        Text("STEP 4 / 4", style = DumbType.Mono.copy(letterSpacing = 0.14.em), color = theme.neutral600)
        Text(
            "HOME SCREEN",
            style = DumbType.H2.copy(fontSize = 30.sp),
            color = theme.text,
            modifier = Modifier.padding(top = Spacing.s3, bottom = Spacing.s1),
        )
        Text(
            "The list goes on the home screen, so the phone lands you on words, not icons.",
            style = DumbType.Body.copy(lineHeight = 20.sp),
            color = theme.text.copy(alpha = 0.72f),
            modifier = Modifier.padding(bottom = Spacing.s6),
        )

        WidgetPreview(state)

        Column(
            modifier = Modifier.padding(top = Spacing.s6),
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            PLACEMENT_STEPS.forEachIndexed { i, step ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
                    Text(
                        "%02d".format(i + 1),
                        style = DumbType.Mono,
                        color = theme.accent700,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(
                        step,
                        style = DumbType.Body.copy(lineHeight = 20.sp),
                        color = theme.text.copy(alpha = 0.75f),
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))
        SetupFooter(onBack = onBack, onContinue = onContinue, continueLabel = "ADD WIDGET")
    }
}

/**
 * The widget as it will look, on the deep accent ground under **both** themes.
 *
 * It does not follow the page: the widget is an object sitting on someone
 * else's home screen, and it reads as one by staying inverted
 * (home-widget "The preview panel is inverted under both grounds").
 */
@Composable
private fun WidgetPreview(state: WidgetSetupUiState, modifier: Modifier = Modifier) {
    Blueprint(
        modifier = modifier.fillMaxWidth(),
        borderColor = theme.accent900,
        markColor = theme.accent700,
    ) {
        Column(
            Modifier
                .background(theme.accent900, RectangleShape)
                .padding(horizontal = Spacing.s4, vertical = Spacing.s6)
        ) {
            Text(
                state.dateLine,
                style = DumbType.Button.copy(fontSize = 12.sp, letterSpacing = 0.12.em),
                color = theme.reverseFg.copy(alpha = 0.6f),
            )
            Text(
                state.clock,
                style = DumbType.Clock.copy(fontSize = 54.sp),
                color = theme.reverseFg,
                modifier = Modifier.padding(top = 2.dp, bottom = Spacing.s4),
            )
            // The rule under the clock is drawn in accent-700 rather than the
            // page divider: it is inside an inverted object, so it has to read
            // against the accent ground, not against the page.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(theme.accent700, RectangleShape)
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.s4),
                verticalArrangement = Arrangement.spacedBy(Spacing.s2),
            ) {
                state.preview.forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            row.name.uppercase(),
                            style = DumbType.RowTitle.copy(fontSize = 15.sp, letterSpacing = 0.06.em),
                            color = theme.reverseFg,
                            modifier = Modifier.weight(1f).alignByBaseline(),
                        )
                        Text(
                            row.meta,
                            style = DumbType.MonoSmall,
                            color = theme.reverseFg.copy(alpha = 0.55f),
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Android placement, not the prototype's iOS copy. Nothing here mentions
 * customising a lock screen, a widget row, a dock or an app library — none of
 * those exist to instruct on this platform (home-widget "Setup is instructed").
 */
private val PLACEMENT_STEPS = listOf(
    "Long-press an empty area of the home screen, tap Widgets, and add \"Dumbify — List\".",
    "Open Settings › Apps › Default apps and set Dumbify as the home app.",
    "Clear the remaining icon pages from your old launcher so there is nothing to swipe back to.",
)
