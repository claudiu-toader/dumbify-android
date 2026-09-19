package com.werkloop.dumbify.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AppCountsTest {

    private fun app(pkg: String) = InstalledApp(pkg, pkg.uppercase(), "SYSTEM")

    private val catalog = listOf("phone", "camera", "maps", "social", "video").map(::app)

    @Test
    fun `counts derive from the catalogue`() {
        val counts = countsFor(catalog, setOf("phone", "camera"))
        assertEquals(5, counts.installed)
        assertEquals(2, counts.allowed)
        assertEquals(3, counts.hidden)
        assertEquals("2 / 5 ALLOWED", counts.ratio)
    }

    @Test
    fun `an allowed package that is no longer installed is not counted`() {
        // Otherwise the ratio reads "3 / 5" while the launcher shows two rows —
        // exactly the disagreement app-allowlist forbids.
        val counts = countsFor(catalog, setOf("phone", "camera", "uninstalled.app"))
        assertEquals(2, counts.allowed)
        assertEquals(3, counts.hidden)
        assertEquals(2, allowedApps(catalog, setOf("phone", "camera", "uninstalled.app")).size)
    }

    @Test
    fun `allowed and hidden partition the catalogue`() {
        val allowed = setOf("phone", "maps")
        assertEquals(
            catalog.size,
            allowedApps(catalog, allowed).size + hiddenApps(catalog, allowed).size,
        )
        assertEquals(listOf("phone", "maps"), allowedApps(catalog, allowed).map { it.packageName })
        assertEquals(listOf("camera", "social", "video"), hiddenApps(catalog, allowed).map { it.packageName })
    }

    @Test
    fun `catalogue order is preserved, not the allowlist's`() {
        assertEquals(
            listOf("phone", "maps"),
            allowedApps(catalog, linkedSetOf("maps", "phone")).map { it.packageName },
        )
    }

    @Test
    fun `an empty allowlist is not an error state`() {
        val counts = countsFor(catalog, emptySet())
        assertEquals(0, counts.allowed)
        assertEquals(5, counts.hidden)
    }
}
