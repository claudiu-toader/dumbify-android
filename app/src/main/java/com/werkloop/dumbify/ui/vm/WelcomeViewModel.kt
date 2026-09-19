package com.werkloop.dumbify.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.werkloop.dumbify.data.DeviceData
import com.werkloop.dumbify.data.DumbifyRepository
import com.werkloop.dumbify.domain.countsFor
import com.werkloop.dumbify.ui.screens.WelcomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    repository: DumbifyRepository,
    device: DeviceData,
) : ViewModel() {
    val state: StateFlow<WelcomeUiState> =
        combine(device.apps, repository.state) { apps, saved ->
            val counts = countsFor(apps, saved.allowedPackages)
            WelcomeUiState(counts.installed, counts.allowed)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WelcomeUiState(0, 0))
}
