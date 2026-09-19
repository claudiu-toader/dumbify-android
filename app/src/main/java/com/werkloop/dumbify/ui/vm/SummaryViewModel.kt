package com.werkloop.dumbify.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.werkloop.dumbify.data.DeviceData
import com.werkloop.dumbify.data.DumbifyRepository
import com.werkloop.dumbify.domain.Allowance
import com.werkloop.dumbify.domain.DumbClock
import com.werkloop.dumbify.domain.FocusMode
import com.werkloop.dumbify.domain.Repeat
import com.werkloop.dumbify.domain.Schedule
import com.werkloop.dumbify.domain.countsFor
import com.werkloop.dumbify.ui.screens.SummaryRow
import com.werkloop.dumbify.ui.screens.SummaryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val repository: DumbifyRepository,
    device: DeviceData,
    private val clock: DumbClock,
) : ViewModel() {

    val state: StateFlow<SummaryUiState> =
        combine(device.apps, repository.state) { apps, saved ->
            val counts = countsFor(apps, saved.allowedPackages)
            SummaryUiState(
                listOf(
                    SummaryRow("ALLOWED", "${counts.allowed} apps"),
                    SummaryRow("HIDDEN", "${counts.hidden} apps"),
                    SummaryRow("SCHEDULE", describe(saved.schedule)),
                    SummaryRow("REQUESTS", "${Allowance.PER_DAY} per day, 15 min each"),
                    SummaryRow("LAUNCHER", "Typeset list"),
                )
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummaryUiState(emptyList()))

    private fun describe(schedule: Schedule): String = when {
        schedule.mode == FocusMode.AlwaysOn -> "Always on"
        schedule.repeat == Repeat.Daily -> "Daily ${schedule.dailyPreset.label}"
        else -> "Day by day"
    }

    fun finishSetup(onDone: () -> Unit) = viewModelScope.launch {
        repository.completeSetup(clock.today())
        onDone()
    }
}
