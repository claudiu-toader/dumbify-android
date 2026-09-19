package com.werkloop.dumbify.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.werkloop.dumbify.R
import com.werkloop.dumbify.ui.theme.DumbType
import com.werkloop.dumbify.ui.theme.theme

/**
 * The app's one toolbar: a back arrow and a title.
 *
 * It is a real Material 3 [TopAppBar] — the height, the title slot and the
 * navigation-icon slot are all the standard ones, so the back affordance sits
 * exactly where a Compose user expects it. Everything visible is overridden to
 * the Industry tokens: a flat `bg` container rather than Material's tonal
 * surface, and the condensed heading face rather than Material's title style.
 * Without that it would be the one screen in the app that looked like someone
 * else's.
 *
 * The navigation icon is [DumbIconButton] rather than Material's `IconButton`,
 * so it matches the launcher's gear exactly — same 44dp box, same 18dp stroked
 * mark, same ghost press state.
 *
 * Insets are deliberately empty: `MainActivity` already applies
 * `safeDrawingPadding()` to the whole tree, and letting the bar add the status
 * bar again would double the gap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DumbTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backDescription: String = "Back",
) {
    TopAppBar(
        modifier = modifier,
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = theme.bg,
            navigationIconContentColor = theme.neutral600,
            titleContentColor = theme.text,
        ),
        navigationIcon = {
            DumbIconButton(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = backDescription,
                onClick = onBack,
            )
        },
        title = {
            Text(title, style = DumbType.H2.copy(fontSize = 24.sp), color = theme.text)
        },
    )
}
