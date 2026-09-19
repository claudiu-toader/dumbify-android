package com.werkloop.dumbify.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.werkloop.dumbify.ui.components.ButtonVariant
import com.werkloop.dumbify.ui.components.DumbButton
import com.werkloop.dumbify.ui.components.SetupFooter
import com.werkloop.dumbify.ui.theme.DumbType
import com.werkloop.dumbify.ui.theme.Spacing
import com.werkloop.dumbify.ui.theme.theme

data class PermissionRow(
    val key: String,
    val name: String,
    val description: String,
    val requirement: String,
    val granted: Boolean,
)

data class PermissionsUiState(
    val permissions: List<PermissionRow>,
) {
    /** Both required grants must exist before setup can continue. */
    val incomplete: Boolean
        get() = permissions.any { it.requirement == "REQUIRED" && !it.granted }

    val statusLine: String
        get() = if (incomplete) {
            "TWO REQUIRED GRANTS OUTSTANDING"
        } else {
            val notif = permissions.firstOrNull { it.key == "notif" }?.granted == true
            "READY · NOTIFICATIONS " + if (notif) "ON" else "SKIPPED"
        }
}

@Composable
fun PermissionsScreen(
    state: PermissionsUiState,
    onGrant: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.s6),
    ) {
        Text("STEP 1 / 4", style = DumbType.Mono.copy(letterSpacing = 0.14.em), color = theme.neutral600)
        Text(
            "PERMISSIONS",
            style = DumbType.H2.copy(fontSize = 30.sp),
            color = theme.text,
            modifier = Modifier.padding(top = Spacing.s3),
        )
        Text(
            "Dumbify enforces the block through the system, so it needs three " +
                "grants. Nothing leaves the phone.",
            style = DumbType.Body.copy(lineHeight = 20.sp),
            color = theme.text.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = Spacing.s3, bottom = Spacing.s6),
        )

        HorizontalDivider(color = theme.divider)
        state.permissions.forEach { p ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.s4),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(p.name.uppercase(), style = DumbType.RowTitle.copy(fontSize = 15.sp), color = theme.text)
                    Text(
                        p.description,
                        style = DumbType.Body.copy(fontSize = 12.sp, lineHeight = 18.sp),
                        color = theme.text.copy(alpha = 0.65f),
                        modifier = Modifier.padding(top = Spacing.s1),
                    )
                    Text(
                        p.requirement,
                        style = DumbType.MonoSmall,
                        color = theme.neutral500,
                        modifier = Modifier.padding(top = Spacing.s1),
                    )
                }
                DumbButton(
                    label = if (p.granted) "GRANTED" else "GRANT",
                    onClick = { onGrant(p.key) },
                    variant = if (p.granted) ButtonVariant.Secondary else ButtonVariant.Primary,
                    textStyle = DumbType.Button.copy(fontSize = 11.sp, letterSpacing = 0.1.em),
                )
            }
            HorizontalDivider(color = theme.divider)
        }

        Column(Modifier.weight(1f)) {}

        Text(
            state.statusLine,
            style = DumbType.Mono.copy(fontSize = 11.sp, letterSpacing = 0.sp),
            color = theme.neutral600,
            modifier = Modifier.padding(bottom = Spacing.s3),
        )
        // Both required grants must exist; the status line above says which are
        // outstanding (onboarding-permissions "Continue is blocked until both
        // required grants exist").
        SetupFooter(onBack = onBack, onContinue = onContinue, continueEnabled = !state.incomplete)
    }
}
