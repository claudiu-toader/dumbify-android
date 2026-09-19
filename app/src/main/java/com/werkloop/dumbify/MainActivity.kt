package com.werkloop.dumbify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.werkloop.dumbify.ui.DumbifyHost
import com.werkloop.dumbify.ui.theme.DumbifyTheme
import com.werkloop.dumbify.ui.vm.RootViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val root: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Usage access and the home role are both revocable from Settings with
        // no callback to us, so every resume re-reads them rather than trusting
        // anything stored (design decision 8).
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) root.refresh()
            }
        )

        setContent {
            val state by root.state.collectAsStateWithLifecycle()
            DumbifyTheme(darkTheme = state.darkTheme) {
                // Nothing is composed until the first read lands: starting on
                // setup and then jumping to the launcher would be a visible lie
                // about which one the user is in.
                state.startOnLauncher?.let { startOnLauncher ->
                    DumbifyHost(
                        startOnLauncher = startOnLauncher,
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .then(if (state.greyscale) Modifier.greyscale() else Modifier),
                    )
                }
            }
        }
    }
}

/**
 * Drains the colour from Dumbify's own surfaces.
 *
 * Device-wide greyscale needs `WRITE_SECURE_SETTINGS`, which a normal app
 * cannot hold — the settings screen says so and deep-links to the system
 * setting instead of pretending otherwise (design decision 3).
 */
private fun Modifier.greyscale(): Modifier = this.then(
    Modifier.drawWithContent {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            }
            // Saturation has to be applied to the composited result, not to
            // each draw call, or overlapping surfaces desaturate twice.
            canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
            drawContent()
            canvas.restore()
        }
    }
)
