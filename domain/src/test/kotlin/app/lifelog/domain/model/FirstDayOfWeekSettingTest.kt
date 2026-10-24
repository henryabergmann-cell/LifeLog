package app.lifelog.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.util.Locale

class FirstDayOfWeekSettingTest {

    @Test
    fun `system setting follows the locale`() {
        assertEquals(DayOfWeek.MONDAY, FirstDayOfWeekSetting.SYSTEM.resolve(Locale.GERMANY))
        assertEquals(DayOfWeek.SUNDAY, FirstDayOfWeekSetting.SYSTEM.resolve(Locale.US))
        assertEquals(DayOfWeek.MONDAY, FirstDayOfWeekSetting.SYSTEM.resolve(Locale.UK))
    }

    @Test
    fun `explicit setting ignores the locale`() {
        assertEquals(DayOfWeek.SUNDAY, FirstDayOfWeekSetting.SUNDAY.resolve(Locale.GERMANY))
        assertEquals(DayOfWeek.MONDAY, FirstDayOfWeekSetting.MONDAY.resolve(Locale.US))
        assertEquals(DayOfWeek.SATURDAY, FirstDayOfWeekSetting.SATURDAY.resolve(Locale.US))
    }
}
