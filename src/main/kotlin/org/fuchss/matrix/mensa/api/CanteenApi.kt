package org.fuchss.matrix.mensa.api

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * This interface defines the minimum functionality that a canteen provides.
 */
@ExperimentalTime
interface CanteenApi {
    fun canteen(): Canteen

    suspend fun foodToday() = foodAtDate(Clock.System.todayIn(TimeZone.currentSystemDefault()))

    /**
     * Retrieve foods of different canteens at a certain date.
     * @param[date] the date to consider
     * @return the lines of the canteen at a certain day
     */
    suspend fun foodAtDate(date: LocalDate): List<CanteenLine>
}
