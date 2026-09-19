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
import com.werkloop.dumbify.ui.components.DumbCheckbox
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
) {
    /** Derived, never authored — no two screens can disagree. */
    val ratio: String get() = "$allowedCount / $installedCount ALLOWED"
    val searchPlaceholder: String get() = "Search $installedCount installed apps"
}

@Composable
fun AllowlistScreen(
    state: AllowlistUiState,
    onQueryChange: (String) -> Unit,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(vertical = Spacing.s6)) {
        Column(Modifier.padding(horizontal = Spacing.s6)) {
            Text("STEP 2 / 4", style = DumbType.Mono.copy(letterSpacing = 0.14.em), color = theme.neutral600)
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
            // Guidance, not a rule. Nothing here counts the allowlist or warns
            // when it is exceeded — app-allowlist "states a target, not a limit".
            Text(
                "Aim for six or fewer — the ones a dumb phone would have had. " +
                    "Phone, Camera, Maps, Notes, Music. You can always request the rest.",
                style = DumbType.Body.copy(lineHeight = 20.sp),
                color = theme.accent700,
                modifier = Modifier.padding(bottom = Spacing.s4),
            )
            DumbTextField(state.query, onQueryChange, state.searchPlaceholder)
        }

        HorizontalDivider(Modifier.padding(top = Spacing.s6), color = theme.divider)
        LazyColumn(Modifier.weight(1f)) {
            items(state.rows, key = { it.packageName }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(row.packageName) }
                        .background(theme.bg, RectangleShape)
                        .padding(horizontal = Spacing.s6, vertical = Spacing.s3)
                        .alpha(if (row.allowed) 1f else 0.5f),
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
            SetupFooter(onBack = onBack, onContinue = onContinue)
        }
    }
}
