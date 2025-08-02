package org.fuchss.matrix.mensa.kit

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.fuchss.matrix.mensa.api.Canteen
import org.fuchss.matrix.mensa.api.CanteenApi
import org.fuchss.matrix.mensa.api.CanteenLine
import org.fuchss.matrix.mensa.api.Meal
import org.jsoup.Jsoup
import kotlin.time.ExperimentalTime

@ExperimentalTime
class MriMensa : CanteenApi {
    companion object {
        private const val MRI_WEBSITE = "https://casinocatering.de/speiseplan/"
    }

    override fun canteen() = Canteen("mri", "Max Rubner-Institut", link = MRI_WEBSITE)

    override suspend fun foodAtDate(date: LocalDate): List<CanteenLine> {
        val mealsThisWeek = parseCanteen(date)
        val mealsToday = mealsThisWeek[date]?.let { listOf(it) } ?: emptyList()
        return mealsToday
    }

    private suspend fun parseCanteen(date: LocalDate): Map<LocalDate, CanteenLine> {
        val client = HttpClient()
        val response = client.request(MRI_WEBSITE) { method = HttpMethod.Get }
        val body: String = response.body()
        val document = Jsoup.parse(body)

        val mainContent = document.getElementById("content") ?: return emptyMap()

        val dateToFood = mutableMapOf<LocalDate, CanteenLine>()
        var dateForEntry = date.minus(DatePeriod(days = date.dayOfWeek.isoDayNumber - 1))

        val entries = mainContent.getElementsByClass("elementor-column")
        var startFound = false
        for (entry in entries) {
            val title = entry.getElementsByClass("elementor-heading-title").first()?.text() ?: continue
            if (title.contains("Montag")) {
                // First day of week
                startFound = true
            }
            if (!startFound) {
                continue
            }

            if (dateToFood.size == 5) {
                // We have all days of the week
                break
            }

            val foods = entry.getElementsByClass("elementor-icon-list-item").toList().map { it.text().replace("•", "").trim() }
            dateToFood[dateForEntry] = CanteenLine("", foods.map { toMeal(it) })
            dateForEntry = dateForEntry.plus(DatePeriod(days = 1))
        }
        return dateToFood
    }

    private fun toMeal(name: String): Meal {
        // e.g., "(a,b,c)"
        val additionals = Regex("\\(([^)]+)\\)").findAll(name).toList().flatMap { it.groupValues[1].split(",") }
        return Meal(name, additionals.contains("d") || additionals.contains("b"), false, false, false, false, false)
    }
}
