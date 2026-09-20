package com.werkloop.dumbify.domain

/**
 * Allowed, hidden and installed, derived in one place.
 *
 * app-allowlist requires that counts are derived and never authored, so that
 * no two screens can disagree. That only holds if there is one function — this
 * one — and every screen reads it rather than counting a list of its own.
 */
data class AppCounts(
    val installed: Int,
    val allowed: Int,
) {
    val hidden: Int get() = installed - allowed

    /** The allowlist step's header: "6 / 16 ALLOWED". */
    val ratio: String get() = "$allowed / $installed ALLOWED"
}

/**
 * [allowedPackages] may name apps that are no longer installed — an allowed app
 * can be uninstalled at any time. Only packages actually present in [catalog]
 * are counted, so the ratio can never read "7 / 6"
 * (app-allowlist "An allowed app is uninstalled").
 */
fun countsFor(catalog: List<InstalledApp>, allowedPackages: Set<String>): AppCounts =
    AppCounts(
        installed = catalog.size,
        allowed = catalog.count { it.packageName in allowedPackages },
    )

/** The allowed apps, in catalogue order. */
fun allowedApps(catalog: List<InstalledApp>, allowedPackages: Set<String>): List<InstalledApp> =
    catalog.filter { it.packageName in allowedPackages }

/** The hidden apps — everything the launcher does not render. */
fun hiddenApps(catalog: List<InstalledApp>, allowedPackages: Set<String>): List<InstalledApp> =
    catalog.filterNot { it.packageName in allowedPackages }

/**
 * The user's cap on how many apps may be allowed at once.
 *
 * A real limit, not the guidance it replaced: the allowlist step used to say
 * "aim for six or fewer" and enforce nothing (app-allowlist "The screen states
 * a target, not a limit"). The number is now the user's own, chosen on the same
 * screen as the picks because it is the same decision.
 *
 * Nothing here ever removes an app. Lowering the cap below what is already
 * allowed is refused rather than resolved by dropping apps the user chose —
 * [lowerBound] is the floor a control must respect, and deselecting is how it
 * moves (app-allowlist "The cap is never enforced by removal").
 */
object AppCap {

    /** Carried over from the guidance this replaced — six is still the advice. */
    const val DEFAULT: Int = 6

    /** One app minimum; a cap above the installed total would mean nothing. */
    fun range(installed: Int): IntRange = 1..maxOf(1, installed)

    fun clamp(value: Int, installed: Int): Int = value.coerceIn(range(installed))

    /**
     * The lowest the cap may be set to right now. Never below the number
     * already allowed, so lowering it can never orphan a pick.
     */
    fun lowerBound(allowed: Int): Int = maxOf(1, allowed)

    /** False once the cap is reached — the picker stops accepting additions. */
    fun canAllowMore(allowed: Int, cap: Int): Boolean = allowed < cap
}
