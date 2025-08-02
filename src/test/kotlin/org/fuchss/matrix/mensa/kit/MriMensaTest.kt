package org.fuchss.matrix.mensa.kit

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@ExperimentalTime
class MriMensaTest {
    /**
     * Check if the CanteenAPI of MRI works.
     */
    @Test
    fun testMriCanteenAPI() {
        runBlocking {
            val mensa = MriMensa()
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val someWorkDay = if (today.dayOfWeek.isoDayNumber > 5) today.minus(4, DateTimeUnit.DAY) else today
            val food = mensa.foodAtDate(someWorkDay)
            assertNotNull(food)
        }
    }
}
