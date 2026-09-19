package com.werkloop.dumbify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.werkloop.dumbify.ui.components.Blueprint
import com.werkloop.dumbify.ui.components.ButtonVariant
import com.werkloop.dumbify.ui.components.DumbButton
import com.werkloop.dumbify.ui.components.SegmentedControl
import com.werkloop.dumbify.ui.components.SetupFooter
import com.werkloop.dumbify.ui.theme.DumbType
import com.werkloop.dumbify.ui.theme.Spacing
import com.werkloop.dumbify.ui.theme.theme

/** One editable row: "Every day" in daily mode, or one weekday. */
@Immutable
data class ScheduleRow(
    val key: String,
    val name: String,
    val value: String,
    val selected: Boolean,
    /** OFF reads as a secondary control; a real window reads as primary. */
    val active: Boolean,
)

@Immutable
data class FocusRulesUiState(
    val alwaysOn: Boolean,
    val perDay: Boolean,
    val rows: List<ScheduleRow>,
    /** 24 booleans — the resulting-window bar. */
    val dumbHours: List<Boolean>,
    val windowLabel: String,
)

@Composable
fun FocusRulesScreen(
    state: FocusRulesUiState,
    onModeChange: (alwaysOn: Boolean) -> Unit,
    onRepeatChange: (perDay: Boolean) -> Unit,
    onSelectRow: (String) -> Unit,
    onCycleRow: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(Spacing.s6)) {
        Text("STEP 3 / 4", style = DumbType.Mono.copy(letterSpacing = 0.14.em), color = theme.neutral600)
        Text(
            "FOCUS RULES",
            style = DumbType.H2.copy(fontSize = 30.sp),
            color = theme.text,
            modifier = Modifier.padding(top = Spacing.s3, bottom = Spacing.s4),
        )

        SegmentedControl(
            options = listOf("Always on", "Scheduled"),
            selectedIndex = if (state.alwaysOn) 0 else 1,
            onSelect = { onModeChange(it == 0) },
        )

        // The step opens on Scheduled so the user can see what a window is,
        // while the copy argues for always on (focus-schedule "Recommendation").
        Text(
            "Recommended: always on. Outside a scheduled window the normal home " +
                "screen comes back, and most people stop coming back to the list at all.",
            style = DumbType.Body.copy(fontSize = 12.sp, lineHeight = 18.sp),
            color = theme.accent700,
            modifier = Modifier.padding(top = Spacing.s3),
        )

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (state.alwaysOn) {
                Text(
                    "The list is the only home screen, all day, every day. " +
                        "The only way into a hidden app is a request.",
                    style = DumbType.Body.copy(lineHeight = 20.sp),
                    color = theme.text.copy(alpha = 0.72f),
                    modifier = Modifier.padding(top = Spacing.s4, bottom = Spacing.s6),
                )
            } else {
                Text(
                    "REPEAT",
                    style = DumbType.MonoSmall.copy(letterSpacing = 0.12.em),
                    color = theme.neutral600,
                    modifier = Modifier.padding(top = Spacing.s6, bottom = Spacing.s2),
                )
                SegmentedControl(
                    options = listOf("Every day", "Day by day"),
                    selectedIndex = if (state.perDay) 1 else 0,
                    onSelect = { onRepeatChange(it == 1) },
                )
                HorizontalDivider(Modifier.padding(top = Spacing.s4), color = theme.divider)
                state.rows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (row.selected) theme.accent100 else Color.Transparent,
                                RectangleShape,
                            )
                            .clickable { onSelectRow(row.key) }
                            .padding(vertical = Spacing.s3),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            row.name.uppercase(),
                            style = DumbType.RowTitle.copy(letterSpacing = 0.06.em),
                            color = theme.text,
                            modifier = Modifier.weight(1f),
                        )
                        DumbButton(
                            label = row.value,
                            onClick = { onCycleRow(row.key) },
                            variant = if (row.active) ButtonVariant.Primary else ButtonVariant.Secondary,
                            textStyle = DumbType.Button.copy(fontSize = 11.sp, letterSpacing = 0.08.em),
                        )
                    }
                    HorizontalDivider(color = theme.divider)
                }
                Text(
                    "Tap a window to cycle it. The week repeats until you change it.",
                    style = DumbType.Body.copy(fontSize = 12.sp, lineHeight = 18.sp),
                    color = theme.textMuted,
                    modifier = Modifier.padding(top = Spacing.s3, bottom = Spacing.s6),
                )
            }

            WindowBarPanel(state, Modifier.padding(bottom = Spacing.s4))
        }

        SetupFooter(onBack = onBack, onContinue = onContinue)
    }
}

/**
 * The resulting window, drawn.
 *
 * This is the one place the user sees what their choices add up to, so it is
 * shaded from the same evaluator the launcher and the widget read — the bar
 * cannot disagree with the behaviour it is describing.
 */
@Composable
private fun WindowBarPanel(state: FocusRulesUiState, modifier: Modifier = Modifier) {
    Blueprint(modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.s4)) {
            Text(
                "RESULTING WINDOW · ${state.windowLabel}",
                style = DumbType.MonoSmall.copy(letterSpacing = 0.14.em),
                color = theme.neutral600,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.s3)
                    .height(24.dp),
            ) {
                state.dumbHours.forEachIndexed { hour, dumb ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (dumb) theme.accent300 else Color.Transparent, RectangleShape)
                    )
                    // An hour rule between cells, so 24 discrete hours read as
                    // 24 rather than as one continuous smear.
                    if (hour < 23) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(theme.divider, RectangleShape)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.s1),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("00", style = DumbType.MonoSmall, color = theme.neutral600)
                Text("DUMB HOURS SHADED", style = DumbType.MonoSmall, color = theme.neutral600)
                Text("24", style = DumbType.MonoSmall, color = theme.neutral600)
            }
            if (!state.alwaysOn) {
                HorizontalDivider(Modifier.padding(top = Spacing.s3), color = theme.divider)
                Text(
                    "Unshaded hours: icons and the app library return, and the " +
                        "Dumbify widget just counts down to the next window.",
                    style = DumbType.Body.copy(fontSize = 12.sp, lineHeight = 17.sp),
                    color = theme.text.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = Spacing.s3),
                )
            }
        }
    }
}
