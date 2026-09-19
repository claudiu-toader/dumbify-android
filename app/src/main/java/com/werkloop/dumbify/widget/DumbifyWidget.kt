package com.werkloop.dumbify.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.dp
import com.werkloop.dumbify.domain.WindowEvaluator
import com.werkloop.dumbify.domain.allowedApps
import com.werkloop.dumbify.ui.Fmt
import dagger.hilt.android.EntryPointAccessors
import java.time.Duration

/**
 * The list, carried off the launcher.
 *
 * Android has had no third-party lock-screen widget since API 21, so this is a
 * home-screen widget: its job is to put the list on whatever surface the device
 * actually lands on, including another launcher if Dumbify is not the home app
 * (design decision 2).
 *
 * It is drawn on the deep accent ground under both themes — the widget is an
 * object on someone else's home screen, and it reads as one by not following
 * the page.
 */
class DumbifyWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val deps = EntryPointAccessors.fromApplication(
            context.applicationContext, WidgetEntryPoint::class.java,
        )
        val clock = deps.clock()
        val saved = deps.repository().state.value
        val apps = deps.appCatalog().installedApps()
        val usage = deps.usageReader().today()

        val now = clock.now()
        val localNow = clock.localNow()
        val window = WindowEvaluator.windowStateAt(localNow, saved.schedule)
        val grant = saved.grant?.takeIf { it.isActive(now) }

        val rows = if (window.inForce) {
            allowedApps(apps, saved.allowedPackages).take(MAX_ROWS).map { app ->
                app.label to Fmt.duration(usage?.perPackage?.get(app.packageName))
            } + listOfNotNull(
                grant?.let { active ->
                    val label = apps.firstOrNull { it.packageName == active.packageName }?.label
                        ?: active.packageName
                    label to Fmt.countdown(active.remainingSeconds(now))
                }
            )
        } else {
            emptyList()
        }

        // Outside a window there is no list — only how long until there is one.
        // Never a hidden app's name, never a count of them
        // (home-widget "The widget does not leak hidden apps").
        val countdown = if (window.inForce) null else {
            window.nextWindowStart
                ?.let { Fmt.coarseRemaining(Duration.between(localNow, it)) + " TO THE NEXT WINDOW" }
                ?: "NO WINDOW SCHEDULED"
        }

        provideContent {
            WidgetBody(
                dateLine = Fmt.longDate(localNow),
                clock = Fmt.clock(localNow),
                rows = rows,
                countdown = countdown,
            )
        }
    }

    private companion object {
        /** More than this and the widget stops being a glance. */
        const val MAX_ROWS = 5
    }
}

private val Ground = ColorProvider(Color(0xFF1D2D3D))
private val Rule = ColorProvider(Color(0xFF416180))
private val Ink = ColorProvider(Color(0xFFF2F2F3))
private val InkMuted = ColorProvider(Color(0x8CF2F2F3))

@androidx.compose.runtime.Composable
private fun WidgetBody(
    dateLine: String,
    clock: String,
    rows: List<Pair<String, String>>,
    countdown: String?,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(Ground)
            .padding(horizontal = 14.dp, vertical = 20.dp)
    ) {
        Text(dateLine, style = TextStyle(color = InkMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium))
        Text(clock, style = TextStyle(color = Ink, fontSize = 40.sp, fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.height(10.dp))
        Spacer(GlanceModifier.fillMaxWidth().height(1.dp).background(Rule))
        Spacer(GlanceModifier.height(10.dp))

        if (countdown != null) {
            Text(countdown, style = TextStyle(color = InkMuted, fontSize = 12.sp))
        } else {
            rows.forEach { (name, meta) ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Text(
                        name.uppercase(),
                        style = TextStyle(color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Text(meta, style = TextStyle(color = InkMuted, fontSize = 10.sp))
                }
            }
        }
    }
}
