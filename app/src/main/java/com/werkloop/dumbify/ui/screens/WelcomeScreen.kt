package com.werkloop.dumbify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.werkloop.dumbify.ui.components.Blueprint
import com.werkloop.dumbify.ui.components.DumbBlockButton
import com.werkloop.dumbify.ui.theme.DumbType
import com.werkloop.dumbify.ui.theme.Spacing
import com.werkloop.dumbify.ui.theme.theme

data class WelcomeUiState(
    val installedCount: Int,
    val allowedCount: Int,
)

@Composable
fun WelcomeScreen(
    state: WelcomeUiState,
    onBegin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = Spacing.s6, end = Spacing.s6, top = Spacing.s8, bottom = Spacing.s6),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                "DUMBIFY / SETUP 0.1",
                style = DumbType.Kicker,
                color = theme.accent700,
            )
            Text(
                "MAKE THIS\nPHONE DUMB.",
                style = DumbType.Display,
                color = theme.text,
                modifier = Modifier.padding(top = Spacing.s6),
            )
            Text(
                "Dumbify replaces your home screen with a list. Apps you do not " +
                    "allow are not dimmed and not one tap away. They are absent.",
                style = DumbType.BodyLarge.copy(fontSize = 14.sp, lineHeight = 22.sp),
                color = theme.text.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = Spacing.s6),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s6)) {
            BeforeAfterPanel(state)
            DumbBlockButton("BEGIN SETUP", onBegin)
            Text(
                "3 PERMISSIONS · 2 MINUTES",
                style = DumbType.Mono.copy(fontSize = 11.sp, letterSpacing = 0.sp),
                color = theme.neutral600,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The "16 icons become 6 lines" panel — the thesis, drawn. */
@Composable
private fun BeforeAfterPanel(state: WelcomeUiState, modifier: Modifier = Modifier) {
    Blueprint(modifier = modifier.fillMaxWidth().height(112.dp)) {
        Row(Modifier.fillMaxSize()) {
            // BEFORE — a grid of undifferentiated icon blocks.
            val dividerColor = theme.divider
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .drawBehind {
                        val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
                        drawLine(
                            color = dividerColor,
                            start = Offset(size.width, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dash,
                        )
                    }
                    .padding(horizontal = Spacing.s4),
                verticalArrangement = Arrangement.Center,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                    repeat(2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                            repeat(4) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(12.dp)
                                        .background(theme.neutral300, RectangleShape)
                                )
                            }
                        }
                    }
                }
                Text(
                    "BEFORE / ${state.installedCount} ICONS",
                    style = DumbType.MonoSmall,
                    color = theme.neutral600,
                    modifier = Modifier.padding(top = Spacing.s3),
                )
            }
            // AFTER — typeset rules of varying measure.
            Column(
                modifier = Modifier.weight(1f).fillMaxSize().padding(horizontal = Spacing.s4),
                verticalArrangement = Arrangement.Center,
            ) {
                listOf(0.70f, 0.52f, 0.61f, 0.44f).forEach { fraction ->
                    Box(
                        Modifier
                            .padding(bottom = 5.dp)
                            .fillMaxWidth(fraction)
                            .height(1.dp)
                            .background(theme.text, RectangleShape)
                    )
                }
                Text(
                    "AFTER / ${state.allowedCount} LINES",
                    style = DumbType.MonoSmall,
                    color = theme.accent700,
                    modifier = Modifier.padding(top = Spacing.s3),
                )
            }
        }
    }
}
