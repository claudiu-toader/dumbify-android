package com.werkloop.dumbify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.werkloop.dumbify.ui.components.DumbButton
import com.werkloop.dumbify.ui.theme.DumbType
import com.werkloop.dumbify.ui.theme.Spacing
import com.werkloop.dumbify.ui.theme.theme

@Immutable
data class DayBar(
    val initial: String,
    /** 0f..1f against the tallest day in the series. */
    val fraction: Float,
    val isToday: Boolean,
)

@Immutable
data class StatRow(val label: String, val value: String)

@Immutable
data class StatsUiState(
    val todayTotal: String,
    /** Null reads as "unavailable" — never as zero. */
    val baseline: String?,
    val week: List<DayBar>,
    val rows: List<StatRow>,
    /** Usage access has gone away; the figures cannot be computed. */
    val usageAccessMissing: Boolean = false,
)

@Composable
fun StatsScreen(
    state: StatsUiState,
    onFixUsageAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(Spacing.s6)) {
        Text(
            "SCREEN TIME",
            style = DumbType.H2.copy(fontSize = 30.sp),
            color = theme.text,
            modifier = Modifier.padding(top = Spacing.s4, bottom = Spacing.s4),
        )

        if (state.usageAccessMissing) {
            UsageUnavailable(onFixUsageAccess)
            return@Column
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Blueprint(Modifier.fillMaxWidth()) {
                Row {
                    Figure("TODAY", state.todayTotal, theme.text, Modifier.weight(1f))
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(theme.divider, RectangleShape)
                    )
                    // The baseline is deliberately quieter than today's figure:
                    // it is context, not a target (usage-stats "visually
                    // subordinate").
                    Figure(
                        "BEFORE DUMBIFY",
                        state.baseline ?: "UNAVAILABLE",
                        theme.neutral500,
                        Modifier.weight(1f),
                    )
                }
            }

            Text(
                "LAST 7 DAYS",
                style = DumbType.MonoSmall.copy(letterSpacing = 0.1.em),
                color = theme.neutral600,
                modifier = Modifier.padding(top = Spacing.s6, bottom = Spacing.s3),
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                verticalAlignment = Alignment.Bottom,
            ) {
                state.week.forEach { day ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(day.fraction.coerceIn(0.02f, 1f))
                            .background(
                                if (day.isToday) theme.accent else Color.Transparent,
                                RectangleShape,
                            )
                            .border(
                                1.dp,
                                if (day.isToday) theme.accent else theme.neutral400,
                                RectangleShape,
                            )
                    )
                }
            }
            HorizontalDivider(color = theme.divider)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.s2),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            ) {
                state.week.forEach { day ->
                    Text(
                        day.initial,
                        style = DumbType.MonoSmall,
                        color = theme.neutral500,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(Spacing.s6))
            HorizontalDivider(color = theme.divider)
            // Counters only. No points, no streak, no badge, no comparison —
            // usage-stats "Factual counters only".
            state.rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.s3),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        row.label,
                        style = DumbType.Body,
                        color = theme.text.copy(alpha = 0.75f),
                    )
                    Text(
                        row.value,
                        style = DumbType.RowTitle.copy(fontSize = 14.sp, letterSpacing = 0.04.em),
                        color = theme.text,
                    )
                }
                HorizontalDivider(color = theme.divider)
            }
        }
    }
}

@Composable
private fun Figure(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier.padding(Spacing.s4)) {
        Text(
            label,
            style = DumbType.MonoSmall.copy(letterSpacing = 0.1.em),
            color = theme.neutral600,
        )
        Text(
            value,
            style = DumbType.H2.copy(fontSize = 34.sp),
            color = valueColor,
            modifier = Modifier.padding(top = Spacing.s2),
        )
    }
}

/**
 * The permission can be revoked from Settings at any time with no callback to
 * us, so this screen has to survive it saying so rather than drawing zeroes
 * (usage-stats "Usage access is not granted").
 */
@Composable
private fun UsageUnavailable(onFix: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
        Text(
            "Usage access is off, so there are no figures to show. " +
                "Dumbify reads these from the device and sends them nowhere.",
            style = DumbType.Body.copy(lineHeight = 20.sp),
            color = theme.text.copy(alpha = 0.72f),
        )
        DumbButton(
            label = "OPEN USAGE ACCESS SETTINGS",
            onClick = onFix,
            fillWidth = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
