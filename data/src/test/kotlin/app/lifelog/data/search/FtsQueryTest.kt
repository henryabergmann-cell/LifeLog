package app.lifelog.data.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FtsQueryTest {

    @Test
    fun `each word becomes a lower-case prefix term`() {
        assertEquals("zahn* müller*", FtsQuery.from("  Zahn  Müller "))
    }

    @Test
    fun `operator characters and keywords are neutralized`() {
        assertEquals("a* or* b* near*", FtsQuery.from("\"a\" OR b* NEAR(-)"))
    }

    @Test
    fun `blank or punctuation-only input yields no query`() {
        assertNull(FtsQuery.from("   "))
        assertNull(FtsQuery.from("*\"-()"))
    }

    @Test
    fun `term count is capped`() {
        assertEquals(8, FtsQuery.from((1..20).joinToString(" ") { "w$it" })!!.split(" ").size)
    }
}
