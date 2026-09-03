package org.fuchss.matrix.mensa.kit

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
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
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime

@ExperimentalTime
class MriMensa : CanteenApi {
    companion object {
        private val logger = LoggerFactory.getLogger(MriMensa::class.java)
        private const val MRI_WEBSITE = "https://casinocatering.de/speiseplan/"
        private const val REQUEST_TIMEOUT_IN_MS = 30_000L
    }

    override fun canteen() = Canteen("mri", "Max Rubner-Institut", link = MRI_WEBSITE)

    override suspend fun foodAtDate(date: LocalDate): List<CanteenLine> {
        val mealsThisWeek = parseCanteen(date)
        val mealsToday = mealsThisWeek[date]?.let { listOf(it) } ?: emptyList()
        return mealsToday
    }

    private suspend fun parseCanteen(date: LocalDate): Map<LocalDate, CanteenLine> {
        val body = request() ?: return emptyMap()
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

    /** Load the menu of the current week. Returns null if the canteen cannot be reached. */
    private suspend fun request(): String? =
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_IN_MS
                connectTimeoutMillis = REQUEST_TIMEOUT_IN_MS
                socketTimeoutMillis = REQUEST_TIMEOUT_IN_MS
            }
        }.use { client ->
            try {
                val response = client.request(MRI_WEBSITE) { method = HttpMethod.Get }
                if (response.status.isSuccess()) {
                    response.body<String>()
                } else {
                    logger.error("Could not load the menu: {}", response.status)
                    null
                }
            } catch (e: Exception) {
                logger.error("Could not load the menu", e)
                null
            }
        }

    private fun toMeal(name: String): Meal {
        // e.g., "(a,b,c)"
        val additionals = Regex("\\(([^)]+)\\)").findAll(name).toList().flatMap { it.groupValues[1].split(",") }
        return Meal(name, additionals.contains("d") || additionals.contains("b"), false, false, false, false, false)
    }
}
