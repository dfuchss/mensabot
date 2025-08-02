package org.fuchss.matrix.mensa.kit

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.fuchss.matrix.mensa.numberOfWeek
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@ExperimentalTime
class SwkaMensaTest {
    /**
     * Check if the CanteenAPI of SWKA works.
     */
    @Test
    fun testSWKACanteenAPI() {
        runBlocking {
            val mensa = SwkaMensa()
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val someWorkDay = if (today.dayOfWeek.isoDayNumber > 5) today.plus(2, DateTimeUnit.DAY) else today
            val food = mensa.foodAtDate(someWorkDay)
            assertNotNull(food)
        }
    }

    /**
     * Check if the week in year is calculated correctly.
     */
    @Test
    fun testWeekInYear() {
        assertEquals(52, numberOfWeek(LocalDate(2023, 1, 1)))
        assertEquals(1, numberOfWeek(LocalDate(2023, 1, 2)))
        assertEquals(1, numberOfWeek(LocalDate(2023, 1, 3)))
        assertEquals(1, numberOfWeek(LocalDate(2023, 1, 4)))
        assertEquals(1, numberOfWeek(LocalDate(2023, 1, 5)))
        assertEquals(1, numberOfWeek(LocalDate(2023, 1, 6)))
        assertEquals(1, numberOfWeek(LocalDate(2023, 1, 7)))
        assertEquals(1, numberOfWeek(LocalDate(2023, 1, 8)))
        assertEquals(2, numberOfWeek(LocalDate(2023, 1, 9)))
        assertEquals(15, numberOfWeek(LocalDate(2023, 4, 14)))
        assertEquals(1, numberOfWeek(LocalDate(2024, 1, 1)))
        assertEquals(1, numberOfWeek(LocalDate(2025, 1, 1)))
        assertEquals(1, numberOfWeek(LocalDate(2026, 1, 1)))
        assertEquals(53, numberOfWeek(LocalDate(2027, 1, 1)))
    }
}
