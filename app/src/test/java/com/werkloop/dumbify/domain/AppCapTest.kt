package com.werkloop.dumbify.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppCapTest {

    @Test
    fun `range is one to the installed total`() {
        assertEquals(1..16, AppCap.range(16))
    }

    @Test
    fun `range never collapses on an empty catalogue`() {
        // A device mid-enumeration reports zero apps; 1..0 would be empty and
        // every clamp against it would throw.
        assertEquals(1..1, AppCap.range(0))
    }

    @Test
    fun `clamp pulls a cap down when apps are uninstalled`() {
        // A cap of 12 was legal when 16 were installed; after a cull it is not.
        assertEquals(5, AppCap.clamp(12, installed = 5))
    }

    @Test
    fun `clamp never returns zero`() {
        assertEquals(1, AppCap.clamp(0, installed = 16))
        assertEquals(1, AppCap.clamp(-3, installed = 16))
    }

    @Test
    fun `lower bound is what is already allowed`() {
        assertEquals(4, AppCap.lowerBound(allowed = 4))
    }

    @Test
    fun `lower bound is one when nothing is allowed`() {
        // Never zero: a cap of zero would make the launcher unusable with no
        // way back short of raising it again.
        assertEquals(1, AppCap.lowerBound(allowed = 0))
    }

    @Test
    fun `more may be allowed below the cap`() {
        assertTrue(AppCap.canAllowMore(allowed = 5, cap = 6))
    }

    @Test
    fun `nothing more may be allowed at the cap`() {
        assertFalse(AppCap.canAllowMore(allowed = 6, cap = 6))
    }

    @Test
    fun `nothing more may be allowed above the cap`() {
        // Reachable if the cap were ever lowered past the allowlist; the guard
        // is >= rather than == so that state resolves by deselecting.
        assertFalse(AppCap.canAllowMore(allowed = 8, cap = 6))
    }

    @Test
    fun `the default is the six the old guidance advised`() {
        assertEquals(6, AppCap.DEFAULT)
    }
}
