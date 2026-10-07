package com.aricansoft.sahatakip.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataRulesTest {
    @Test
    fun workItemNormalizationUsesTurkishCaseRules(){
        assertEquals("daire pano",normalizeWorkItemName("  DAİRE PANO  "))
        assertEquals("işlik",normalizeWorkItemName("İŞLİK"))
    }

    @Test
    fun targetDateRejectsImpossibleCalendarDates(){
        assertTrue(isValidTargetDate(""))
        assertTrue(isValidTargetDate("2026-10-07"))
        assertFalse(isValidTargetDate("2026-02-30"))
        assertFalse(isValidTargetDate("2026-99-99"))
    }

    @Test
    fun targetDateCanonicalizesWhitespaceAndNull(){
        assertEquals("2026-10-07",validateTargetDate(" 2026-10-07 "))
        assertNull(validateTargetDate("   "))
        assertNull(validateTargetDate(null))
    }

    @Test(expected=IllegalArgumentException::class)
    fun targetDateThrowsForInvalidInput(){
        validateTargetDate("2026-13-01")
    }
}
