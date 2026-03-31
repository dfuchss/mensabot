package org.fuchss.matrix.mensa

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Calculate the week in year for a certain date (according to ISO 8601).
 * @param[date] the date to consider
 * @return the week in year
 */
internal fun numberOfWeek(date: LocalDate): Int {
    // See ISO 8601
    val startOfYear = LocalDate(date.year, 1, 1)
    val firstDayOfWeekOne =
        when (startOfYear.dayOfWeek.isoDayNumber) {
            1 -> startOfYear

            // Monday -> Monday (same week)
            2 -> startOfYear.minus(1, DateTimeUnit.DAY)

            // Tuesday -> Monday (same week)
            3 -> startOfYear.minus(2, DateTimeUnit.DAY)

            // Wednesday -> Monday (same week)
            4 -> startOfYear.minus(3, DateTimeUnit.DAY)

            // Thursday -> Monday (same week)
            5 -> startOfYear.plus(3, DateTimeUnit.DAY)

            // Friday -> Monday (next week)
            6 -> startOfYear.plus(2, DateTimeUnit.DAY)

            // Saturday -> Monday (next week)
            7 -> startOfYear.plus(1, DateTimeUnit.DAY)

            // Sunday -> Monday (next week)
            else -> error("Impossible day of week")
        }

    val numberOfDays = date.toEpochDays() - firstDayOfWeekOne.toEpochDays()
    if (numberOfDays < 0) {
        // Last week of previous year
        return numberOfWeek(LocalDate(date.year - 1, 12, 31))
    }
    return numberOfDays.toInt() / 7 + 1
}
