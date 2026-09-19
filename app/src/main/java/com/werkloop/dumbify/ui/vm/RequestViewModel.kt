package com.werkloop.dumbify.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.werkloop.dumbify.data.DeviceData
import com.werkloop.dumbify.data.DumbifyRepository
import com.werkloop.dumbify.domain.Allowance
import com.werkloop.dumbify.domain.DumbClock
import com.werkloop.dumbify.domain.Grant
import com.werkloop.dumbify.domain.InstalledApp
import com.werkloop.dumbify.domain.hiddenApps
import com.werkloop.dumbify.system.SystemNavigator
import com.werkloop.dumbify.system.UsageReader
import com.werkloop.dumbify.ui.Fmt
import com.werkloop.dumbify.ui.screens.HiddenAppRow
import com.werkloop.dumbify.ui.screens.RequestUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class RequestViewModel @Inject constructor(
    private val repository: DumbifyRepository,
    private val device: DeviceData,
    private val navigator: SystemNavigator,
    private val usage: UsageReader,
    private val clock: DumbClock,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val pending = MutableStateFlow<HiddenAppRow?>(null)
    private val baseline = MutableStateFlow<Map<String, Duration>>(emptyMap())

    init {
        viewModelScope.launch {
            baseline.value = usage.baselinePerApp(repository.state.value.setupCompletedOn)
        }
    }

    val state: StateFlow<RequestUiState> = combine(
        device.apps, repository.state, query, pending, baseline,
    ) { apps, saved, q, dialog, before ->
        val now = clock.now()
        val grant = saved.grant?.takeIf { it.isActive(now) }
        val needle = q.trim()
        RequestUiState(
            query = q,
            rows = hiddenApps(apps, saved.allowedPackages)
                .filter { needle.isBlank() || it.label.contains(needle, ignoreCase = true) }
                .map { it.toRow(before[it.packageName], grant) },
            remaining = Allowance.remaining(saved.ledger, clock.today()),
            perDay = Allowance.PER_DAY,
            pendingApp = dialog,
        )
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000),
        RequestUiState("", emptyList(), Allowance.PER_DAY, Allowance.PER_DAY),
    )

    private fun InstalledApp.toRow(before: Duration?, grant: Grant?) = HiddenAppRow(
        packageName = packageName,
        name = label,
        meta = category + " · " + (
            before?.let { "${Fmt.duration(it)}/day before" } ?: "no history"
            ),
        granted = grant?.packageName == packageName,
    )

    fun setQuery(value: String) { query.value = value }

    fun request(row: HiddenAppRow) { pending.value = row }

    /** Cancelling creates nothing and consumes nothing. */
    fun cancel() { pending.value = null }

    fun confirm(onGranted: () -> Unit) = viewModelScope.launch {
        val row = pending.value ?: return@launch
        val today = clock.today()
        val ledger = repository.state.value.ledger
        if (!Allowance.canGrant(ledger, today)) {
            pending.value = null
            return@launch
        }
        repository.setGrant(
            Grant(row.packageName, clock.now().plus(Grant.DURATION_MINUTES, ChronoUnit.MINUTES))
        )
        repository.setLedger(Allowance.spend(ledger, today))
        pending.value = null
        onGranted()
    }

    fun open(packageName: String) {
        if (!navigator.launchApp(packageName)) device.refresh()
    }
}
