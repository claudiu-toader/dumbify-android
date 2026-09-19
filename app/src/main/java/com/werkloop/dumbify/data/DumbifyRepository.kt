package com.werkloop.dumbify.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.werkloop.dumbify.di.ApplicationScope
import com.werkloop.dumbify.domain.AppSettings
import com.werkloop.dumbify.domain.Grant
import com.werkloop.dumbify.domain.PendingRemoval
import com.werkloop.dumbify.domain.RequestLedger
import com.werkloop.dumbify.domain.Schedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only thing that writes to disk.
 *
 * Scalars are plain preference keys; the three composite values — schedule,
 * grant, ledger, pending removal — are JSON strings, because DataStore
 * Preferences has no structured value and the alternative is a protobuf
 * toolchain the app does not need (design decision 5).
 *
 * Nothing here leaves the device. There is no network permission in the
 * manifest to make that a policy rather than a promise.
 */
@Singleton
class DumbifyRepository @Inject constructor(
    private val store: DataStore<Preferences>,
    @ApplicationScope scope: CoroutineScope,
) {
    private object Keys {
        val SetupComplete = booleanPreferencesKey("setup_complete")
        val SetupCompletedOn = stringPreferencesKey("setup_completed_on")
        val AllowedPackages = stringSetPreferencesKey("allowed_packages")
        val Schedule = stringPreferencesKey("schedule")
        val Settings = stringPreferencesKey("settings")
        val Grant = stringPreferencesKey("grant")
        val Ledger = stringPreferencesKey("request_ledger")
        val PendingRemoval = stringPreferencesKey("pending_removal")
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val flow: Flow<DumbifyState> = store.data
        // A corrupt or unreadable file must not take the launcher down with it:
        // this app *is* the home screen, so failing closed means an unusable
        // phone. Falling back to defaults loses settings, which is recoverable.
        .catch { cause -> if (cause is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw cause }
        .map { prefs ->
            DumbifyState(
                setupComplete = prefs[Keys.SetupComplete] ?: false,
                setupCompletedOn = prefs[Keys.SetupCompletedOn]
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                allowedPackages = prefs[Keys.AllowedPackages] ?: emptySet(),
                schedule = prefs[Keys.Schedule].decodeOr(Schedule()),
                settings = prefs[Keys.Settings].decodeOr(AppSettings()),
                grant = prefs[Keys.Grant].decodeOrNull<Grant>(),
                ledger = prefs[Keys.Ledger].decodeOrNull<RequestLedger>(),
                pendingRemoval = prefs[Keys.PendingRemoval].decodeOrNull<PendingRemoval>(),
            )
        }

    val state: StateFlow<DumbifyState> =
        flow.stateIn(scope, SharingStarted.Eagerly, DumbifyState())

    private inline fun <reified T> String?.decodeOr(fallback: T): T =
        decodeOrNull<T>() ?: fallback

    private inline fun <reified T> String?.decodeOrNull(): T? =
        if (this == null) null else runCatching { json.decodeFromString<T>(this) }.getOrNull()

    // ── writes ──────────────────────────────────────────────────────────────

    suspend fun completeSetup(on: LocalDate) = store.edit {
        it[Keys.SetupComplete] = true
        it[Keys.SetupCompletedOn] = on.toString()
    }

    suspend fun setAllowed(packageName: String, allowed: Boolean) = store.edit { prefs ->
        val current = prefs[Keys.AllowedPackages] ?: emptySet()
        prefs[Keys.AllowedPackages] = if (allowed) current + packageName else current - packageName
    }

    suspend fun setSchedule(schedule: Schedule) =
        store.edit { it[Keys.Schedule] = json.encodeToString(schedule) }

    suspend fun setSettings(settings: AppSettings) =
        store.edit { it[Keys.Settings] = json.encodeToString(settings) }

    /** A new grant replaces any existing one — app-requests "One grant at a time". */
    suspend fun setGrant(grant: Grant?) = store.edit { prefs ->
        if (grant == null) prefs.remove(Keys.Grant) else prefs[Keys.Grant] = json.encodeToString(grant)
    }

    suspend fun setLedger(ledger: RequestLedger) =
        store.edit { it[Keys.Ledger] = json.encodeToString(ledger) }

    suspend fun setPendingRemoval(pending: PendingRemoval?) = store.edit { prefs ->
        if (pending == null) prefs.remove(Keys.PendingRemoval)
        else prefs[Keys.PendingRemoval] = json.encodeToString(pending)
    }
}
