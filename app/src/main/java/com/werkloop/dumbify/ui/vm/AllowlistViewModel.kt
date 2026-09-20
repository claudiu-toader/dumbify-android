package com.werkloop.dumbify.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.werkloop.dumbify.data.DeviceData
import com.werkloop.dumbify.data.DumbifyRepository
import com.werkloop.dumbify.domain.AppCap
import com.werkloop.dumbify.domain.countsFor
import com.werkloop.dumbify.ui.screens.AllowlistRow
import com.werkloop.dumbify.ui.screens.AllowlistUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllowlistViewModel @Inject constructor(
    private val repository: DumbifyRepository,
    device: DeviceData,
) : ViewModel() {

    private val query = MutableStateFlow("")

    /** Set by the host when the screen is reached from settings, not setup. */
    private val editing = MutableStateFlow(false)
    fun setEditing(value: Boolean) { editing.value = value }

    val state: StateFlow<AllowlistUiState> =
        combine(device.apps, repository.state, query, editing) { apps, saved, q, isEditing ->
            val counts = countsFor(apps, saved.allowedPackages)
            val needle = q.trim()
            val rows = apps
                .filter {
                    needle.isBlank() ||
                        it.label.contains(needle, ignoreCase = true) ||
                        it.category.contains(needle, ignoreCase = true)
                }
                .map {
                    AllowlistRow(
                        packageName = it.packageName,
                        name = it.label,
                        category = it.category,
                        allowed = it.packageName in saved.allowedPackages,
                    )
                }
            // The counts come from the whole catalogue, never from the filtered
            // rows — searching must not change what "6 / 16" means.
            AllowlistUiState(
                rows = rows,
                query = q,
                allowedCount = counts.allowed,
                installedCount = counts.installed,
                // Clamped on read: the installed list can shrink under a cap
                // that was legal when it was chosen.
                maxApps = AppCap.clamp(saved.settings.maxApps, counts.installed),
                editing = isEditing,
            )
        }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000),
            AllowlistUiState(emptyList(), "", 0, 0),
        )

    fun setQuery(value: String) { query.value = value }

    fun toggle(packageName: String) = viewModelScope.launch {
        val allowed = packageName in repository.state.value.allowedPackages
        // Guarded here as well as in the screen: the cap is a domain rule, and a
        // rule only the UI enforces is one a second caller can walk straight
        // past. Deselecting is never blocked.
        if (!allowed && state.value.atCap) return@launch
        // Un-allowing removes the app from Dumbify's surfaces and nothing else:
        // it stays installed with its data (app-allowlist "never uninstalls").
        repository.setAllowed(packageName, !allowed)
    }

    /**
     * Never lowered past what is already allowed — the floor comes from the
     * live count, so the cap can only be reduced by deselecting first
     * (app-allowlist "The cap is never enforced by removal").
     */
    fun setMaxApps(value: Int) = viewModelScope.launch {
        val current = state.value
        val clamped = value.coerceIn(current.capFloor, current.capCeiling)
        repository.setSettings(repository.state.value.settings.copy(maxApps = clamped))
    }
}
