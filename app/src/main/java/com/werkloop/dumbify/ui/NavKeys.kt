package com.werkloop.dumbify.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The ten screens, as back-stack entries.
 *
 * Two of this app's navigation requirements are statements about the back stack
 * itself rather than about routes — the launcher must be impossible to escape
 * with Back, and setup must be unreplayable once complete. Navigation 3 makes
 * the stack a list the app owns, so both are facts about a list rather than
 * options passed to a navigate call (design decision 7).
 */
@Serializable data object Welcome : NavKey
@Serializable data object Permissions : NavKey
@Serializable data object Allowlist : NavKey
@Serializable data object FocusRules : NavKey
@Serializable data object WidgetSetup : NavKey
@Serializable data object Summary : NavKey
@Serializable data object Launcher : NavKey
@Serializable data object Request : NavKey
@Serializable data object Stats : NavKey
@Serializable data object Settings : NavKey

/**
 * The two setup screens, reached again from settings.
 *
 * Separate keys rather than a flag on the setup keys, so the back stack itself
 * records which flow the user is in — the same reason the stack is owned rather
 * than inferred (design decision 7).
 */
@Serializable data object AllowlistEdit : NavKey
@Serializable data object FocusRulesEdit : NavKey
