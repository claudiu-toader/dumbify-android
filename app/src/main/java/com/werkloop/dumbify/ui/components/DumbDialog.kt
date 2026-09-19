package com.werkloop.dumbify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.werkloop.dumbify.ui.theme.DumbType
import com.werkloop.dumbify.ui.theme.Spacing
import com.werkloop.dumbify.ui.theme.theme

/**
 * The one dialog in the system — square, hairline, on the page ground rather
 * than on a raised surface.
 *
 * The design draws the backdrop inside the phone frame; on a device that is the
 * platform's scrim, so the composable takes the ground itself and lets the
 * `Dialog` window supply the dimming.
 *
 * **No registration marks, against the prototype.** `Dumbify.dc.html` gives the
 * dialog `class="dialog blueprint"` with four corner marks, and this followed
 * it. It is wrong here: the marks annotate a *panel* — something laid out on
 * the page for inspection — and a modal asking a yes/no question is not being
 * inspected. On a floating box over a scrim they read as crosses stuck to the
 * corners rather than as survey ticks, and they pull the eye outward exactly
 * when it should be on the question. The six information panels keep theirs.
 */
@Composable
fun DumbDialog(
    kicker: String,
    title: String,
    body: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(Spacing.s6),
            contentAlignment = Alignment.Center,
        ) {
            Blueprint(
                modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
                borderColor = theme.divider,
                // No registration marks, deliberately against the prototype —
                // see the note above.
                showMarks = false,
            ) {
                Column(
                    modifier = Modifier
                        .background(theme.bg, RectangleShape)
                        .padding(Spacing.s4),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s3),
                ) {
                    Text(
                        kicker,
                        style = DumbType.Mono.copy(letterSpacing = 0.14.em),
                        color = theme.accent700,
                    )
                    Text(title, style = DumbType.H3.copy(fontSize = 26.sp), color = theme.text)
                    Text(
                        body,
                        style = DumbType.Body.copy(fontSize = 14.sp),
                        color = theme.text.copy(alpha = 0.85f),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.s2),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                    ) {
                        DumbButton(
                            label = dismissLabel.uppercase(),
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            variant = ButtonVariant.Secondary,
                            fillWidth = true,
                            textStyle = DumbType.Button.copy(fontSize = 13.sp, letterSpacing = 0.1.em),
                            contentPadding = PaddingValues(Spacing.s3),
                        )
                        DumbButton(
                            label = confirmLabel.uppercase(),
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            enabled = confirmEnabled,
                            fillWidth = true,
                            textStyle = DumbType.Button.copy(fontSize = 13.sp, letterSpacing = 0.1.em),
                            contentPadding = PaddingValues(Spacing.s3),
                        )
                    }
                }
            }
        }
    }
}
