package com.werkloop.dumbify.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import com.werkloop.dumbify.domain.AppSettings
import com.werkloop.dumbify.domain.FocusMode
import com.werkloop.dumbify.domain.Grant
import com.werkloop.dumbify.domain.PendingRemoval
import com.werkloop.dumbify.domain.Repeat
import com.werkloop.dumbify.domain.RequestLedger
import com.werkloop.dumbify.domain.Schedule
import com.werkloop.dumbify.domain.WindowPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
class DumbifyRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var store: DataStore<Preferences>
    private lateinit var repo: DumbifyRepository

    /** Every boot writes to and reads from this one file. */
    private val file by lazy { tmp.newFile("dumbify.preferences_pb").also { it.delete() } }

    @Before
    fun setUp() = boot()

    @After
    fun tearDown() {
        scope.cancel()
    }

    /**
     * Start a "process": a fresh scope, a fresh DataStore over the same file,
     * and a fresh repository.
     *
     * DataStore refuses two live instances on one file, which is exactly right
     * and is also why a restart has to be modelled by cancelling the old
     * scope rather than by opening a second store beside the first. Cancelling
     * is what releases the file, so this is a closer analogue of process death
     * than a second instance would have been.
     */
    private fun boot() {
        if (::scope.isInitialized) scope.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        store = PreferenceDataStoreFactory.create(scope = scope) { file }
        repo = DumbifyRepository(store, scope)
    }

    /** Restart, and hand back the repository the new process sees. */
    private fun restarted(): DumbifyRepository {
        boot()
        return repo
    }

    @Test
    fun `defaults are readable before anything is written`() = runTest {
        val state = repo.state.first()
        assertEquals(false, state.setupComplete)
        assertNull("no setup date until setup finishes", state.setupCompletedOn)
        assertEquals(emptySet<String>(), state.allowedPackages)
        assertEquals(FocusMode.Scheduled, state.schedule.mode)
        assertNull(state.grant)
        assertNull(state.pendingRemoval)
        assertNull("a null dark theme means follow the device", state.settings.darkTheme)
    }

    @Test
    fun `every field round-trips`() = runTest {
        val schedule = Schedule(
            mode = FocusMode.Scheduled,
            repeat = Repeat.PerDay,
            dailyPreset = WindowPreset.Evening,
            perDay = mapOf(
                DayOfWeek.MONDAY to WindowPreset.Overnight,
                DayOfWeek.SATURDAY to WindowPreset.Off,
            ),
        )
        val settings = AppSettings(
            darkTheme = true, bigClock = false, greyscale = true,
            notificationDigest = true, removalDelay = false,
        )
        val grant = Grant("com.example.social", Instant.parse("2026-09-13T10:00:00Z"))
        val ledger = RequestLedger(LocalDate.of(2026, 9, 13), 2)
        val pending = PendingRemoval(Instant.parse("2026-09-14T09:00:00Z"))

        repo.completeSetup(LocalDate.of(2026, 9, 13))
        repo.setAllowed("com.example.phone", true)
        repo.setAllowed("com.example.camera", true)
        repo.setSchedule(schedule)
        repo.setSettings(settings)
        repo.setGrant(grant)
        repo.setLedger(ledger)
        repo.setPendingRemoval(pending)

        val state = restarted().state.first()
        assertTrue(state.setupComplete)
        assertEquals(LocalDate.of(2026, 9, 13), state.setupCompletedOn)
        assertEquals(setOf("com.example.phone", "com.example.camera"), state.allowedPackages)
        assertEquals(schedule, state.schedule)
        assertEquals(settings, state.settings)
        assertEquals(grant, state.grant)
        assertEquals(ledger, state.ledger)
        assertEquals(pending, state.pendingRemoval)
    }

    @Test
    fun `a grant re-read after a restart still expires at the original instant`() = runTest {
        val start = Instant.parse("2026-09-13T09:41:00Z")
        repo.setGrant(Grant("com.example.video", start.plus(15, ChronoUnit.MINUTES)))

        val reread = restarted().state.first().grant!!
        // Nine minutes have passed while the process was dead. A countdown
        // persisted as "seconds left" would hand back all 900 here.
        assertEquals(360, reread.remainingSeconds(start.plus(9, ChronoUnit.MINUTES)))
        assertEquals(0, reread.remainingSeconds(start.plus(20, ChronoUnit.MINUTES)))
    }

    @Test
    fun `disallowing one app leaves the rest of the allowlist alone`() = runTest {
        repo.setAllowed("a", true)
        repo.setAllowed("b", true)
        repo.setAllowed("c", true)
        repo.setAllowed("b", false)
        assertEquals(setOf("a", "c"), repo.state.first().allowedPackages)
    }

    @Test
    fun `clearing a grant removes it rather than storing an empty one`() = runTest {
        repo.setGrant(Grant("x", Instant.now()))
        repo.setGrant(null)
        assertNull(restarted().state.first().grant)
    }

    @Test
    fun `state emits on every write`() = runTest {
        repo.state.test {
            assertEquals(emptySet<String>(), awaitItem().allowedPackages)
            repo.setAllowed("com.example.maps", true)
            assertEquals(setOf("com.example.maps"), awaitItem().allowedPackages)
            repo.completeSetup(LocalDate.of(2026, 9, 13))
            assertTrue(awaitItem().setupComplete)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an unreadable value falls back to its default rather than crashing`() = runTest {
        // This app is the home screen: a corrupt preference must not leave the
        // user with a phone that cannot render a launcher.
        store.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                set(androidx.datastore.preferences.core.stringPreferencesKey("schedule"), "{not json")
                set(androidx.datastore.preferences.core.stringPreferencesKey("grant"), "garbage")
            }
        }
        val state = restarted().state.first()
        assertEquals(Schedule(), state.schedule)
        assertNull(state.grant)
    }
}
