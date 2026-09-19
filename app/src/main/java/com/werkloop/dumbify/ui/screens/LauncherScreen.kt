package com.werkloop.dumbify.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.werkloop.dumbify.R
import com.werkloop.dumbify.ui.components.Blueprint
import com.werkloop.dumbify.ui.components.ButtonVariant
import com.werkloop.dumbify.ui.components.DumbButton
import com.werkloop.dumbify.ui.components.DumbIconButton
import com.werkloop.dumbify.ui.theme.DumbType
import com.werkloop.dumbify.ui.theme.Spacing
import com.werkloop.dumbify.ui.theme.theme

data class LauncherRow(
    val packageName: String,
    val name: String,
    /** Today's time in this app; a neutral dash when unused today. */
    val meta: String,
    /** A temporary grant is appended at the end and marked. */
    val granted: Boolean = false,
)

data class LauncherUiState(
    /** "TUE 2 SEP · DUMB UNTIL 21:00", or without an end time when always on. */
    val dateLine: String,
    val clock: String,
    val apps: List<LauncherRow>,
    val todayTotal: String,
    val bigClock: Boolean = true,
    /**
     * Whether Dumbify actually holds the home role right now.
     *
     * False is the state where the product quietly does nothing: the list is
     * still here if you open the app, but the Home button goes somewhere else
     * and every hidden app is one tap away again. The launcher says so rather
     * than rendering as though it were in force (dumb-launcher "The launcher
     * states when it is not the home screen").
     */
    val isDefaultHome: Boolean = true,
)

@Composable
fun LauncherScreen(
    state: LauncherUiState,
    onOpenApp: (String) -> Unit,
    onRequest: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onMakeHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The launcher is the permanent bottom of the back stack, so Back has
    // nothing to pop to. Consuming it keeps the home surface up rather than
    // finishing the Activity (dumb-launcher "Back does not escape").
    BackHandler(enabled = true) { /* deliberately nothing */ }

    Column(modifier = modifier.fillMaxSize().padding(Spacing.s6)) {
        // The date line and the settings affordance share one 44dp row — the
        // gear is the launcher's third and last exit, and it is header chrome,
        // not a row of the list (dumb-launcher "Three exits only").
        Row(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                state.dateLine,
                style = DumbType.Mono.copy(letterSpacing = 0.12.em),
                color = theme.neutral600,
                modifier = Modifier.weight(1f),
            )
            DumbIconButton(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = "Settings",
                onClick = onSettings,
            )
        }
        Text(
            state.clock,
            style = if (state.bigClock) DumbType.Clock else DumbType.H2,
            color = theme.text,
            modifier = Modifier.padding(top = Spacing.s2, bottom = Spacing.s6),
        )

        if (!state.isDefaultHome) {
            NotHomeNotice(onMakeHome, Modifier.padding(bottom = Spacing.s6))
        }

        HorizontalDivider(color = theme.divider)
        LazyColumn(Modifier.weight(1f)) {
            items(state.apps, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (app.granted) theme.accent100 else Color.Transparent,
                            RectangleShape,
                        )
                        .clickable { onOpenApp(app.packageName) }
                        .padding(vertical = Spacing.s4),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                ) {
                    // The design baseline-aligns the 9sp meta against the 27sp
                    // name; aligning bottoms instead drops it below the name.
                    Text(
                        app.name.uppercase(),
                        style = DumbType.LauncherApp,
                        color = if (app.granted) theme.accent700 else theme.text,
                        modifier = Modifier.weight(1f).alignByBaseline(),
                    )
                    Text(
                        app.meta,
                        style = DumbType.MonoSmall,
                        color = if (app.granted) theme.accent700 else theme.neutral500,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
                HorizontalDivider(color = theme.divider)
            }
        }

        // The footer's two exits — request an app, and today's usage. Settings
        // is the third, and lives in the header rather than here so this row
        // stays a pair.
        HorizontalDivider(Modifier.padding(top = Spacing.s4), color = theme.divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.s4),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DumbButton(
                label = "REQUEST AN APP",
                onClick = onRequest,
                variant = ButtonVariant.Secondary,
                textStyle = DumbType.Button.copy(fontSize = 11.sp, letterSpacing = 0.12.em),
            )
            DumbButton(
                label = "TODAY ${state.todayTotal}",
                onClick = onStats,
                variant = ButtonVariant.Ghost,
                textStyle = DumbType.Button.copy(fontSize = 11.sp, letterSpacing = 0.12.em),
            )
        }
    }
}

/**
 * Shown when Dumbify is not the home screen.
 *
 * It is a blueprint panel rather than a row, because it is not an app and must
 * not read as one — and it sits above the list rather than below it, since a
 * list of allowed apps is misleading while the hidden ones are all still one
 * Home press away.
 *
 * The copy states the situation and stops. No count of what is exposed, no
 * urging, no "you're slipping" — the product's whole argument is absence, not
 * nagging (settings "Every setting states its effect").
 */
@Composable
private fun NotHomeNotice(onMakeHome: () -> Unit, modifier: Modifier = Modifier) {
    Blueprint(modifier.fillMaxWidth(), borderColor = theme.accent) {
        Column(
            Modifier.padding(Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            Text(
                "NOT YOUR HOME SCREEN",
                style = DumbType.MonoSmall.copy(letterSpacing = 0.14.em),
                color = theme.accent700,
            )
            Text(
                "The Home button goes to your old launcher, so every hidden app is " +
                    "still one tap away. Dumbify is not enforcing anything until it " +
                    "is the home screen.",
                style = DumbType.Body.copy(fontSize = 12.sp, lineHeight = 18.sp),
                color = theme.text.copy(alpha = 0.72f),
            )
            DumbButton(
                label = "MAKE DUMBIFY HOME",
                onClick = onMakeHome,
                fillWidth = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = DumbType.Button.copy(fontSize = 12.sp, letterSpacing = 0.12.em),
            )
        }
    }
}
