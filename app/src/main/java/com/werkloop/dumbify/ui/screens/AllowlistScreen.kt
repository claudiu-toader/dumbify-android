package com.werkloop.dumbify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.werkloop.dumbify.domain.AppCap
import com.werkloop.dumbify.ui.components.DumbBlockButton
import com.werkloop.dumbify.ui.components.DumbCheckbox
import com.werkloop.dumbify.ui.components.DumbStepper
import com.werkloop.dumbify.ui.components.DumbTextField
import com.werkloop.dumbify.ui.components.SetupFooter
import com.werkloop.dumbify.ui.theme.DumbType
import com.werkloop.dumbify.ui.theme.Spacing
import com.werkloop.dumbify.ui.theme.theme

data class AllowlistRow(
    /** The identity the allowlist is stored by — labels are not unique. */
    val packageName: String,
    val name: String,
    val category: String,
    val allowed: Boolean,
)

data class AllowlistUiState(
    val rows: List<AllowlistRow>,
    val query: String,
    val allowedCount: Int,
    val installedCount: Int,
    /** The user's cap. Chosen here, and editable again from settings. */
    val maxApps: Int = AppCap.DEFAULT,
    /** True when this screen was opened from settings rather than from setup. */
    val editing: Boolean = false,
) {
    /** Derived, never authored — no two screens can disagree. */
    val ratio: String get() = "$allowedCount / $maxApps ALLOWED"
    val searchPlaceholder: String get() = "Search $installedCount installed apps"

    /** No more may be allowed until the cap rises or something is deselected. */
    val atCap: Boolean get() = !AppCap.canAllowMore(allowedCount, maxApps)

    /** The cap cannot drop below what is already allowed. */
    val capFloor: Int get() = AppCap.lowerBound(allowedCount)
    val capCeiling: Int get() = AppCap.range(installedCount).last
}

@Composable
fun AllowlistScreen(
    state: AllowlistUiState,
    onQueryChange: (String) -> Unit,
    onToggle: (String) -> Unit,
    onMaxAppsChange: (Int) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(vertical = Spacing.s6)) {
        Column(Modifier.padding(horizontal = Spacing.s6)) {
            if (!state.editing) {
                Text("STEP 2 / 4", style = DumbType.Mono.copy(letterSpacing = 0.14.em), color = theme.neutral600)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.s3),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text("ALLOWED APPS", style = DumbType.H2.copy(fontSize = 30.sp), color = theme.text)
                Text(
                    state.ratio,
                    style = DumbType.RowTitle.copy(fontSize = 13.sp, letterSpacing = 0.08.em),
                    color = theme.accent700,
                )
            }
            Text(
                "Unchecked apps vanish from the launcher. They stay installed and keep their data.",
                style = DumbType.Body.copy(lineHeight = 20.sp),
                color = theme.text.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = Spacing.s3, bottom = Spacing.s3),
            )
            // The cap and the picks are one decision, so they share a screen.
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.s3),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(end = Spacing.s3)) {
                    Text(
                        "MAXIMUM APPS",
                        style = DumbType.RowTitle.copy(fontSize = 13.sp, letterSpacing = 0.08.em),
                        color = theme.text,
                    )
                    Text(
                        // The floor is stated only when it is actually biting,
                        // so the line is never noise.
                        if (state.maxApps <= state.capFloor && state.allowedCount > 0) {
                            "Deselect an app to lower this."
                        } else {
                            "Six is what a dumb phone had: Phone, Camera, Maps, Notes, Music."
                        },
                        style = DumbType.Body.copy(fontSize = 12.sp, lineHeight = 17.sp),
                        color = theme.text.copy(alpha = 0.65f),
                        modifier = Modifier.padding(top = Spacing.s1),
                    )
                }
                DumbStepper(
                    value = state.maxApps,
                    onChange = onMaxAppsChange,
                    min = state.capFloor,
                    max = state.capCeiling,
                    label = "Maximum apps",
                )
            }
            if (state.atCap) {
                Text(
                    "Cap reached. Deselect one, or raise the maximum, to allow another. " +
                        "Anything hidden can still be requested.",
                    style = DumbType.Body.copy(lineHeight = 20.sp),
                    color = theme.accent700,
                    modifier = Modifier.padding(bottom = Spacing.s4),
                )
            }
            DumbTextField(state.query, onQueryChange, state.searchPlaceholder)
        }

        HorizontalDivider(Modifier.padding(top = Spacing.s6), color = theme.divider)
        LazyColumn(Modifier.weight(1f)) {
            items(state.rows, key = { it.packageName }) { row ->
                // At the cap an unchecked row is inert: tapping it would have
                // to either refuse silently or drop someone else's pick, and
                // neither is honest. Allowed rows always stay tappable, so
                // deselecting is the way out.
                val selectable = row.allowed || !state.atCap
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = selectable) { onToggle(row.packageName) }
                        .background(theme.bg, RectangleShape)
                        .padding(horizontal = Spacing.s6, vertical = Spacing.s3)
                        .alpha(if (row.allowed) 1f else if (selectable) 0.5f else 0.3f),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DumbCheckbox(row.allowed)
                    Text(
                        row.name.uppercase(),
                        style = DumbType.RowTitle,
                        color = theme.text,
                        modifier = Modifier.weight(1f),
                    )
                    Text(row.category, style = DumbType.MonoSmall.copy(fontSize = 10.sp), color = theme.neutral500)
                }
                HorizontalDivider(color = theme.divider)
            }
        }

        Column(Modifier.padding(horizontal = Spacing.s6, vertical = Spacing.s4)) {
            if (state.editing) {
                DumbBlockButton(label = "DONE", onClick = onContinue)
            } else {
                SetupFooter(onBack = onBack, onContinue = onContinue)
            }
        }
    }
}
