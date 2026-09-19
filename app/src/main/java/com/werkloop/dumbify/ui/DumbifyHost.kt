package com.werkloop.dumbify.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.werkloop.dumbify.ui.screens.AllowlistScreen
import com.werkloop.dumbify.ui.screens.FocusRulesScreen
import com.werkloop.dumbify.ui.screens.LauncherScreen
import com.werkloop.dumbify.ui.screens.PermissionsScreen
import com.werkloop.dumbify.ui.screens.RequestScreen
import com.werkloop.dumbify.ui.screens.SettingsScreen
import com.werkloop.dumbify.ui.screens.StatsScreen
import com.werkloop.dumbify.ui.screens.SummaryScreen
import com.werkloop.dumbify.ui.screens.WelcomeScreen
import com.werkloop.dumbify.ui.screens.WidgetSetupScreen
import com.werkloop.dumbify.ui.vm.AllowlistViewModel
import com.werkloop.dumbify.ui.vm.FocusRulesViewModel
import com.werkloop.dumbify.ui.vm.LauncherViewModel
import com.werkloop.dumbify.ui.vm.PermissionsViewModel
import com.werkloop.dumbify.ui.vm.RequestViewModel
import com.werkloop.dumbify.ui.vm.SettingsViewModel
import com.werkloop.dumbify.ui.vm.StatsViewModel
import com.werkloop.dumbify.ui.vm.SummaryViewModel
import com.werkloop.dumbify.ui.vm.WelcomeViewModel
import com.werkloop.dumbify.ui.vm.WidgetSetupViewModel

/**
 * One owned back stack, two logical flows.
 *
 * Setup runs welcome → … → summary; finishing it clears the list and seeds it
 * with [Launcher], which is why Back cannot replay setup: the entries are gone,
 * not merely popped past. In the shell, [Launcher] is the permanent bottom
 * entry, so no in-app screen ever sits behind it.
 */
@Composable
fun DumbifyHost(
    startOnLauncher: Boolean,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(if (startOnLauncher) Launcher else Welcome)

    fun go(key: NavKey) { backStack.add(key) }
    fun back() { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        // Back at the bottom of the stack is consumed rather than finishing the
        // Activity — the launcher is a home surface, and Home has nothing behind
        // it (dumb-launcher "Back does not escape the launcher").
        onBack = { back() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Welcome> {
                val vm = hiltViewModel<WelcomeViewModel>()
                val state by vm.state.collectAsStateWithLifecycle()
                WelcomeScreen(state, onBegin = { go(Permissions) })
            }
            entry<Permissions> {
                val vm = hiltViewModel<PermissionsViewModel>()
                val state by vm.state.collectAsStateWithLifecycle()
                PermissionsScreen(
                    state = state,
                    onGrant = rememberGrantRequest(state, openSettings = vm::grant),
                    onBack = { back() },
                    onContinue = { go(Allowlist) },
                )
            }
            entry<Allowlist> {
                val vm = hiltViewModel<AllowlistViewModel>()
                val state by vm.state.collectAsStateWithLifecycle()
                AllowlistScreen(
                    state = state,
                    onQueryChange = vm::setQuery,
                    onToggle = { vm.toggle(it) },
                    onBack = { back() },
                    onContinue = { go(FocusRules) },
                )
            }
            entry<FocusRules> {
                val vm = hiltViewModel<FocusRulesViewModel>()
                val state by vm.state.collectAsStateWithLifecycle()
                FocusRulesScreen(
                    state = state,
                    onModeChange = { vm.setMode(it) },
                    onRepeatChange = { vm.setRepeat(it) },
                    onSelectRow = vm::select,
                    onCycleRow = vm::cycle,
                    onBack = { back() },
                    onContinue = { go(WidgetSetup) },
                )
            }
            entry<WidgetSetup> {
                val vm = hiltViewModel<WidgetSetupViewModel>()
                val state by vm.state.collectAsStateWithLifecycle()
                WidgetSetupScreen(state, onBack = { back() }, onContinue = { go(Summary) })
            }
            entry<Summary> {
                val vm = hiltViewModel<SummaryViewModel>()
                val state by vm.state.collectAsStateWithLifecycle()
                SummaryScreen(
                    state = state,
                    onBack = { back() },
                    onFinish = {
                        vm.finishSetup {
                            // Setup is not popped, it is discarded.
                            backStack.clear()
                            backStack.add(Launcher)
                        }
                    },
                )
            }
            entry<Launcher> {
                val vm = hiltViewModel<LauncherViewModel>()
                val state by vm.state.collectAsStateWithLifecycle()
                LauncherScreen(
                    state = state,
                    onOpenApp = vm::open,
                    onRequest = { go(Request) },
                    onStats = { go(Stats) },
                    // The gear in the launcher header, not a button hanging off
                    // statistics — the design gave settings its own affordance
                    // on the home surface (design decision 7).
                    onSettings = { go(Settings) },
                    onMakeHome = vm::makeHome,
                )
            }
            entry<Request> {
                val vm = hiltViewModel<RequestViewModel>()
                val state by vm.state.collectAsStateWithLifecycle()
                RequestScreen(
                    state = state,
                    onQueryChange = vm::setQuery,
                    onRequest = vm::request,
                    onOpen = vm::open,
                    onConfirm = { vm.confirm { back() } },
                    onCancel = vm::cancel,
                )
            }
            entry<Stats> {
                val vm = hiltViewModel<StatsViewModel>()
                val state by vm.state.collectAsStateWithLifecycle()
                StatsScreen(
                    state = state,
                    onFixUsageAccess = vm::fixUsageAccess,
                )
            }
            entry<Settings> {
                val vm = hiltViewModel<SettingsViewModel>()
                val state by vm.state.collectAsStateWithLifecycle()
                SettingsScreen(
                    state = state,
                    onToggle = vm::toggle,
                    onOpenColorCorrection = vm::openColorCorrection,
                    onStartRemoval = vm::startRemoval,
                    onCancelRemoval = vm::cancelRemoval,
                    onResumeAsHome = vm::resumeAsHome,
                    onConfirmRemoval = vm::confirmRemoval,
                    onDismissRemovalDialog = vm::dismissRemovalDialog,
                    onBack = { back() },
                )
            }
        },
    )
}
