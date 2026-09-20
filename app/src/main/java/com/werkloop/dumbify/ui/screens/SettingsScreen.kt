package com.werkloop.dumbify.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.werkloop.dumbify.ui.components.Blueprint
import com.werkloop.dumbify.ui.components.ButtonVariant
import com.werkloop.dumbify.ui.components.DumbButton
import com.werkloop.dumbify.ui.components.DumbDialog
import com.werkloop.dumbify.ui.components.DumbSwitch
import com.werkloop.dumbify.ui.components.DumbTopBar
import com.werkloop.dumbify.ui.theme.DumbType
import com.werkloop.dumbify.ui.theme.Spacing
import com.werkloop.dumbify.ui.theme.theme

/** The five switches, identified so the switch block cannot silently grow a sixth. */
enum class SettingKey { DarkTheme, BigClock, Greyscale, NotificationDigest, RemovalDelay }

@Immutable
data class SettingToggle(val key: SettingKey, val name: String, val description: String, val on: Boolean)

@Immutable
data class SettingsUiState(
    val toggles: List<SettingToggle>,
    /** Set while a removal is waiting out its delay. */
    val removalCountdown: String? = null,
    val removalConfirmable: Boolean = false,
    val confirmingRemoval: Boolean = false,
    /** Shown after enabling greyscale — the app cannot apply it device-wide. */
    val greyscaleNotice: Boolean = false,
    /**
     * False once the home alias has been switched off — Dumbify is no longer a
     * candidate for the Home button and the screen offers to put it back.
     */
    val offeredAsHome: Boolean = true,
    /** "3 / 6" — the allowlist against the user's cap. */
    val allowedSummary: String = "",
    /** "21:00–07:00", "Always on", … — the schedule in one phrase. */
    val scheduleSummary: String = "",
)

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onToggle: (SettingKey, Boolean) -> Unit,
    onOpenColorCorrection: () -> Unit,
    onStartRemoval: () -> Unit,
    onCancelRemoval: () -> Unit,
    onResumeAsHome: () -> Unit,
    onEditAllowlist: () -> Unit,
    onEditFocusRules: () -> Unit,
    onConfirmRemoval: () -> Unit,
    onDismissRemovalDialog: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        // `MainActivity` applies `safeDrawingPadding()` to the whole tree, so the
        // bars are already accounted for once. Letting the Scaffold add them
        // again is the double-gap this zeroing exists to prevent — the same
        // reason `DumbTopBar` zeroes its own.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            DumbTopBar(title = "SETTINGS", onBack = onBack, backDescription = "Back to launcher")
        },
    ) { barPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Consumed before the scroll, so the content starts below the
                // bar rather than under it.
                .padding(barPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.s6)
                .padding(bottom = Spacing.s6)
        ) {
            // The setup decisions, editable here rather than only once during
            // setup (settings "Every setup decision is editable"). These open
            // the real setup screens, so there is one allowlist picker and one
            // schedule editor rather than two that drift apart.
            Text(
                "RULES",
                style = DumbType.MonoSmall.copy(letterSpacing = 0.14.em),
                color = theme.neutral600,
                modifier = Modifier.padding(top = Spacing.s2, bottom = Spacing.s3),
            )
            HorizontalDivider(color = theme.divider)
            NavRow("ALLOWED APPS", state.allowedSummary, onEditAllowlist)
            NavRow("FOCUS RULES", state.scheduleSummary, onEditFocusRules)

            Text(
                "APPEARANCE AND STRICTNESS",
                style = DumbType.MonoSmall.copy(letterSpacing = 0.14.em),
                color = theme.neutral600,
                modifier = Modifier.padding(top = Spacing.s6, bottom = Spacing.s3),
            )
            HorizontalDivider(color = theme.divider)
            state.toggles.forEach { toggle ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.s4),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            toggle.name.uppercase(),
                            style = DumbType.RowTitle,
                            color = theme.text,
                        )
                        // One neutral line each. Nothing here praises, congratulates
                        // or urges — settings "Every setting states its effect".
                        Text(
                            toggle.description,
                            style = DumbType.Body.copy(fontSize = 12.sp, lineHeight = 17.sp),
                            color = theme.text.copy(alpha = 0.65f),
                            modifier = Modifier.padding(top = Spacing.s1),
                        )
                    }
                    DumbSwitch(toggle.on, { onToggle(toggle.key, it) })
                }
                HorizontalDivider(color = theme.divider)
            }

            if (state.greyscaleNotice) {
                GreyscaleNotice(onOpenColorCorrection, Modifier.padding(top = Spacing.s4))
            }

            RemovalSection(
                state = state,
                onStartRemoval = onStartRemoval,
                onCancelRemoval = onCancelRemoval,
                onResumeAsHome = onResumeAsHome,
                modifier = Modifier.padding(top = Spacing.s6),
            )
        }
    }

    if (state.confirmingRemoval) {
        DumbDialog(
            kicker = "STOP ENFORCING",
            title = "TURN DUMBIFY OFF",
            body = "The launcher goes back to your normal home screen and every hidden " +
                "app returns. Your allowlist and schedule are kept.",
            confirmLabel = "Turn it off",
            dismissLabel = "Keep it on",
            onConfirm = onConfirmRemoval,
            onDismiss = onDismissRemovalDialog,
        )
    }
}

/**
 * A settings row that opens another screen, with its current value beside it.
 *
 * The value is the point: a row reading "ALLOWED APPS   3 / 6" answers the
 * question most visits are asking without the visit.
 */
@Composable
private fun NavRow(name: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 56.dp)
            .padding(vertical = Spacing.s4),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = DumbType.RowTitle, color = theme.text, modifier = Modifier.weight(1f))
        Text(
            value,
            style = DumbType.RowTitle.copy(fontSize = 13.sp, letterSpacing = 0.08.em),
            color = theme.accent700,
        )
        Text("›", style = DumbType.RowTitle, color = theme.neutral500)
    }
    HorizontalDivider(color = theme.divider)
}

/**
 * Device-wide greyscale is `Settings.Secure`, behind a signature permission.
 * Saying so and deep-linking is the compliant reading of the spec's own
 * "Greyscale cannot be applied" scenario (design decision 3).
 */
@Composable
private fun GreyscaleNotice(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Blueprint(modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            Text(
                "Dumbify drains the colour from its own screens. Draining it from every " +
                    "app needs the system's own colour correction, which only you can turn on.",
                style = DumbType.Body.copy(fontSize = 12.sp, lineHeight = 18.sp),
                color = theme.text.copy(alpha = 0.72f),
            )
            DumbButton(
                label = "OPEN COLOUR CORRECTION",
                onClick = onOpen,
                variant = ButtonVariant.Secondary,
                textStyle = DumbType.Button.copy(fontSize = 11.sp, letterSpacing = 0.1.em),
            )
        }
    }
}

/**
 * The delay guards enforcement, not the APK.
 *
 * Blocking an uninstall needs device-admin, which this build does not have, so
 * the copy never claims the app cannot be removed — only that turning it off
 * here takes a day (design decision 4).
 */
@Composable
private fun RemovalSection(
    state: SettingsUiState,
    onStartRemoval: () -> Unit,
    onCancelRemoval: () -> Unit,
    onResumeAsHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        if (!state.offeredAsHome) {
            // Already stopped. The copy does not congratulate and does not
            // argue — it states where the phone now stands and offers the way
            // back (settings "Every setting states its effect").
            Text(
                "Dumbify is not your home screen. The Home button goes to your old " +
                    "launcher and every hidden app is back. Your allowlist and " +
                    "schedule are kept.",
                style = DumbType.Body.copy(fontSize = 12.sp, lineHeight = 18.sp),
                color = theme.accent700,
            )
            DumbButton(
                label = "USE AS HOME SCREEN",
                onClick = onResumeAsHome,
                variant = ButtonVariant.Secondary,
                textStyle = DumbType.Button.copy(fontSize = 12.sp, letterSpacing = 0.12.em),
            )
        } else if (state.removalCountdown != null) {
            Text(
                if (state.removalConfirmable) {
                    "The delay has elapsed. Confirm again to stop enforcing."
                } else {
                    "Removal pending — ${state.removalCountdown} to go. " +
                        "Dumbify keeps enforcing until then."
                },
                style = DumbType.Body.copy(fontSize = 12.sp, lineHeight = 18.sp),
                color = theme.accent700,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                DumbButton(
                    label = "CANCEL REMOVAL",
                    onClick = onCancelRemoval,
                    variant = ButtonVariant.Secondary,
                    textStyle = DumbType.Button.copy(fontSize = 11.sp, letterSpacing = 0.1.em),
                )
                if (state.removalConfirmable) {
                    DumbButton(
                        label = "CONFIRM",
                        onClick = onStartRemoval,
                        textStyle = DumbType.Button.copy(fontSize = 11.sp, letterSpacing = 0.1.em),
                    )
                }
            }
        } else {
            Text(
                "Turning Dumbify off takes 24 hours and a second confirmation. " +
                    "The wait is deliberate. You can still uninstall the app from " +
                    "Settings at any time — this delay is not a lock.",
                style = DumbType.Body.copy(fontSize = 12.sp, lineHeight = 18.sp),
                color = theme.textMuted,
            )
            DumbButton(
                label = "STOP ENFORCING",
                onClick = onStartRemoval,
                variant = ButtonVariant.Secondary,
                textStyle = DumbType.Button.copy(fontSize = 12.sp, letterSpacing = 0.12.em),
            )
        }
    }
}
