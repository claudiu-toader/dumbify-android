package com.werkloop.dumbify.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.werkloop.dumbify.data.DeviceData
import com.werkloop.dumbify.data.DumbifyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** What the Activity needs before it can compose anything. */
data class RootUiState(
    /** Null until the first read lands — composing before it would flash setup. */
    val startOnLauncher: Boolean? = null,
    val darkTheme: Boolean? = null,
    val greyscale: Boolean = false,
)

@HiltViewModel
class RootViewModel @Inject constructor(
    repository: DumbifyRepository,
    private val device: DeviceData,
) : ViewModel() {

    val state: StateFlow<RootUiState> = repository.state
        .map {
            RootUiState(
                // Setup completion is durable: a second launch opens on the
                // launcher and never replays the four steps
                // (onboarding-permissions "Setup completion is durable").
                startOnLauncher = it.setupComplete,
                darkTheme = it.settings.darkTheme,
                greyscale = it.settings.greyscale,
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RootUiState())

    /** Called on every resume — permissions and usage are never cached across one. */
    fun refresh() = device.refresh()
}
