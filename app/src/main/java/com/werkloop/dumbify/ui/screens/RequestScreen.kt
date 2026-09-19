package com.werkloop.dumbify.ui.screens

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
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.werkloop.dumbify.ui.components.ButtonVariant
import com.werkloop.dumbify.ui.components.DumbButton
import com.werkloop.dumbify.ui.components.DumbDialog
import com.werkloop.dumbify.ui.components.DumbTextField
import com.werkloop.dumbify.ui.theme.DumbType
import com.werkloop.dumbify.ui.theme.Spacing
import com.werkloop.dumbify.ui.theme.theme

@Immutable
data class HiddenAppRow(
    val packageName: String,
    val name: String,
    /** Category and the usage this app had before Dumbify. */
    val meta: String,
    /** True while this app's grant is running — the row offers Open, not 15 min. */
    val granted: Boolean,
)

@Immutable
data class RequestUiState(
    val query: String,
    val rows: List<HiddenAppRow>,
    val remaining: Int,
    val perDay: Int,
    /** The app awaiting confirmation, if the dialog is open. */
    val pendingApp: HiddenAppRow? = null,
) {
    val canGrant: Boolean get() = remaining > 0

    val allowanceCopy: String get() = when {
        perDay == 0 -> "No requests are available. A hidden app stays hidden until you allow it."
        else -> "$remaining of $perDay requests left today. " +
            "A grant lasts 15 minutes, then the app disappears again."
    }
}

@Composable
fun RequestScreen(
    state: RequestUiState,
    onQueryChange: (String) -> Unit,
    onRequest: (HiddenAppRow) -> Unit,
    onOpen: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(vertical = Spacing.s6)) {
        Column(Modifier.padding(horizontal = Spacing.s6)) {
            Text(
                "REQUEST ACCESS",
                style = DumbType.H2.copy(fontSize = 30.sp),
                color = theme.text,
                modifier = Modifier.padding(top = Spacing.s4, bottom = Spacing.s1),
            )
            Text(
                state.allowanceCopy,
                style = DumbType.Body.copy(lineHeight = 20.sp),
                color = theme.text.copy(alpha = 0.72f),
                modifier = Modifier.padding(bottom = Spacing.s4),
            )
            DumbTextField(state.query, onQueryChange, "Search hidden apps")
        }

        HorizontalDivider(Modifier.padding(top = Spacing.s4), color = theme.divider)
        LazyColumn(Modifier.weight(1f)) {
            items(state.rows, key = { it.packageName }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.s6, vertical = Spacing.s4),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            row.name.uppercase(),
                            style = DumbType.RowTitle.copy(fontSize = 17.sp),
                            color = theme.text.copy(alpha = 0.8f),
                        )
                        Text(
                            row.meta,
                            style = DumbType.MonoSmall,
                            color = theme.neutral500,
                            modifier = Modifier.padding(top = Spacing.s1),
                        )
                    }
                    DumbButton(
                        label = if (row.granted) "OPEN" else "15 MIN",
                        onClick = { if (row.granted) onOpen(row.packageName) else onRequest(row) },
                        variant = if (row.granted) ButtonVariant.Primary else ButtonVariant.Secondary,
                        // With no allowance left there is nothing to offer, and
                        // a live button that opens a dialog it cannot confirm
                        // would be a lie (app-requests "Allowance exhausted").
                        enabled = row.granted || state.canGrant,
                        textStyle = DumbType.Button.copy(fontSize = 11.sp, letterSpacing = 0.1.em),
                    )
                }
                HorizontalDivider(color = theme.divider)
            }
        }
    }

    state.pendingApp?.let { pending ->
        DumbDialog(
            kicker = "TEMPORARY GRANT",
            title = "${pending.name.uppercase()} FOR 15 MINUTES",
            body = "It appears at the bottom of the launcher with a countdown, " +
                "then disappears again. This uses one of today's requests.",
            confirmLabel = "Grant",
            dismissLabel = "Never mind",
            onConfirm = onConfirm,
            onDismiss = onCancel,
            confirmEnabled = state.canGrant,
        )
    }
}
