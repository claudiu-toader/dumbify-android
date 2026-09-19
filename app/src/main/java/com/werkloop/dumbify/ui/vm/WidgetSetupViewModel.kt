package com.werkloop.dumbify.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.werkloop.dumbify.data.DeviceData
import com.werkloop.dumbify.data.DumbifyRepository
import com.werkloop.dumbify.domain.DumbClock
import com.werkloop.dumbify.domain.allowedApps
import com.werkloop.dumbify.ui.Fmt
import com.werkloop.dumbify.ui.screens.WidgetPreviewRow
import com.werkloop.dumbify.ui.screens.WidgetSetupUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WidgetSetupViewModel @Inject constructor(
    repository: DumbifyRepository,
    device: DeviceData,
    clock: DumbClock,
) : ViewModel() {

    val state: StateFlow<WidgetSetupUiState> =
        combine(device.apps, device.today, repository.state) { apps, usage, saved ->
            WidgetSetupUiState(
                dateLine = Fmt.longDate(clock.localNow()),
                clock = Fmt.clock(clock.localNow()),
                // The user's own apps, with their own numbers. A preview of
                // someone else's phone teaches nothing (home-widget "Preview
                // matches the allowlist").
                preview = allowedApps(apps, saved.allowedPackages).take(4).map {
                    WidgetPreviewRow(it.label, Fmt.duration(usage?.perPackage?.get(it.packageName)))
                },
            )
        }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000),
            WidgetSetupUiState(Fmt.longDate(clock.localNow()), Fmt.clock(clock.localNow()), emptyList()),
        )
}
