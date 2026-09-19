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
