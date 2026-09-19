package com.werkloop.dumbify.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.werkloop.dumbify.ui.components.Blueprint
import com.werkloop.dumbify.ui.components.SetupFooter
import com.werkloop.dumbify.ui.theme.DumbType
import com.werkloop.dumbify.ui.theme.Spacing
import com.werkloop.dumbify.ui.theme.theme

@Immutable
data class SummaryRow(val key: String, val value: String)

@Immutable
data class SummaryUiState(val rows: List<SummaryRow>)

@Composable
fun SummaryScreen(
    state: SummaryUiState,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = Spacing.s6, end = Spacing.s6, top = Spacing.s8, bottom = Spacing.s6)
    ) {
        Text("SETUP COMPLETE", style = DumbType.Kicker, color = theme.accent700)
        Text(
            "YOUR PHONE\nIS NOW DUMB.",
            style = DumbType.Display.copy(fontSize = 38.sp, lineHeight = 37.sp),
            color = theme.text,
            modifier = Modifier.padding(top = Spacing.s4, bottom = Spacing.s6),
        )

        Blueprint(Modifier.fillMaxWidth()) {
            Column {
                state.rows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.s4, vertical = Spacing.s3),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                    ) {
                        Text(
                            row.key,
                            style = DumbType.MonoSmall.copy(fontSize = 10.sp, letterSpacing = 0.08.em),
                            color = theme.neutral600,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            row.value.uppercase(),
                            style = DumbType.RowTitle.copy(fontSize = 14.sp, letterSpacing = 0.04.em),
                            color = theme.text,
                            textAlign = TextAlign.End,
                        )
                    }
                    HorizontalDivider(color = theme.divider)
                }
            }
        }

        Spacer(Modifier.weight(1f))
        // The delay is stated here, before it is ever needed, because the point
        // of it is that the user knew (settings "Removal takes 24 hours").
        Text(
            "Removing Dumbify takes 24 hours and a confirmation. That delay is the product.",
            style = DumbType.Body.copy(fontSize = 12.sp, lineHeight = 19.sp),
            color = theme.textMuted,
            modifier = Modifier.padding(bottom = Spacing.s4),
        )
        SetupFooter(onBack = onBack, onContinue = onFinish, continueLabel = "LOCK THE PHONE")
    }
}
