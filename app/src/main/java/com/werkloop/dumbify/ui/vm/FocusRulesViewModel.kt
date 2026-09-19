package com.werkloop.dumbify.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.werkloop.dumbify.data.DumbifyRepository
import com.werkloop.dumbify.domain.DumbClock
import com.werkloop.dumbify.domain.FocusMode
import com.werkloop.dumbify.domain.Repeat
import com.werkloop.dumbify.domain.Schedule
import com.werkloop.dumbify.domain.WindowEvaluator
import com.werkloop.dumbify.domain.WindowPreset
import com.werkloop.dumbify.ui.screens.FocusRulesUiState
import com.werkloop.dumbify.ui.screens.ScheduleRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

@HiltViewModel
class FocusRulesViewModel @Inject constructor(
    private val repository: DumbifyRepository,
    clock: DumbClock,
) : ViewModel() {

    private val selectedDay = MutableStateFlow(clock.localNow().dayOfWeek)

    val state: StateFlow<FocusRulesUiState> =
        combine(repository.state, selectedDay) { saved, day ->
            val schedule = saved.schedule
            val scope = WindowEvaluator.scopeOf(schedule, day)
            FocusRulesUiState(
                alwaysOn = schedule.mode == FocusMode.AlwaysOn,
                perDay = schedule.repeat == Repeat.PerDay,
                rows = rowsFor(schedule, day),
                dumbHours = WindowEvaluator.dumbHoursFor(scope, schedule),
                windowLabel = WindowEvaluator.scopeLabel(scope),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial(clock))

    private fun rowsFor(schedule: Schedule, selected: DayOfWeek): List<ScheduleRow> =
        if (schedule.repeat == Repeat.Daily) {
            listOf(
                ScheduleRow(
                    key = DAILY_KEY,
                    name = "Every day",
                    value = schedule.dailyPreset.label,
                    selected = false,
                    active = schedule.dailyPreset != WindowPreset.Off,
                )
            )
        } else {
            DayOfWeek.entries.map { day ->
                val preset = schedule.perDay[day] ?: WindowPreset.Off
                ScheduleRow(
                    key = day.name,
                    name = day.name.lowercase().replaceFirstChar(Char::titlecase),
                    value = preset.label,
                    selected = day == selected,
                    active = preset != WindowPreset.Off,
                )
            }
        }

    fun setMode(alwaysOn: Boolean) = update {
        it.copy(mode = if (alwaysOn) FocusMode.AlwaysOn else FocusMode.Scheduled)
    }

    fun setRepeat(perDay: Boolean) = update {
        // The per-day map is never cleared here, so switching back and forth
        // does not discard a week's worth of editing.
        it.copy(repeat = if (perDay) Repeat.PerDay else Repeat.Daily)
    }

    fun select(key: String) {
        if (key != DAILY_KEY) selectedDay.value = DayOfWeek.valueOf(key)
    }

    fun cycle(key: String) {
        if (key != DAILY_KEY) selectedDay.value = DayOfWeek.valueOf(key)
        update { schedule ->
            if (key == DAILY_KEY) {
                schedule.copy(dailyPreset = schedule.dailyPreset.next())
            } else {
                val day = DayOfWeek.valueOf(key)
                val current = schedule.perDay[day] ?: WindowPreset.Off
                schedule.copy(perDay = schedule.perDay + (day to current.next()))
            }
        }
    }

    private fun update(block: (Schedule) -> Schedule) = viewModelScope.launch {
        repository.setSchedule(block(repository.state.value.schedule))
    }

    private companion object {
        const val DAILY_KEY = "__daily__"

        fun initial(clock: DumbClock): FocusRulesUiState {
            val schedule = Schedule()
            val scope = WindowEvaluator.scopeOf(schedule, clock.localNow().dayOfWeek)
            return FocusRulesUiState(
                alwaysOn = false, perDay = false, rows = emptyList(),
                dumbHours = WindowEvaluator.dumbHoursFor(scope, schedule),
                windowLabel = WindowEvaluator.scopeLabel(scope),
            )
        }
    }
}
