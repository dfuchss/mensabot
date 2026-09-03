package org.fuchss.matrix.mensa.kit

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.fuchss.matrix.mensa.api.Canteen
import org.fuchss.matrix.mensa.api.CanteenApi
import org.fuchss.matrix.mensa.api.CanteenLine
import org.fuchss.matrix.mensa.api.Meal
import org.fuchss.matrix.mensa.numberOfWeek
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime

@ExperimentalTime
class SwkaMensa : CanteenApi {
    companion object {
        private val logger = LoggerFactory.getLogger(SwkaMensa::class.java)
        private const val SWKA_WEBSITE = "https://www.sw-ka.de/en/hochschulgastronomie/speiseplan/mensa_adenauerring/"
        private const val SWKA_WEBSITE_API = //
            "https://www.sw-ka.de/de/hochschulgastronomie/speiseplan/mensa_adenauerring/?view=ok&c=adenauerring&STYLE=popup_plain&kw=%%%WoY%%%"
        private const val REQUEST_TIMEOUT_IN_MS = 30_000L

        private val LINES_TO_CONSIDER = listOf("Linie ", "Schnitzel", "[pizza]werk", "[kœri]werk")

        /** The header of a line may contain its opening hours on an own row, e.g., "[pizza]werk", "Pizza", "11-14 Uhr". */
        private val OPENING_HOURS = Regex("""\d{1,2}([.:]\d{2})?\s*-\s*\d{1,2}([.:]\d{2})?\s*Uhr""")

        /** The allergens of a meal are appended to its name, e.g., "(1,Se,We)". */
        private val ALLERGENS = Regex("""\(([^()]*)\)\s*$""")

        /** The dietary markers of a meal, e.g., "[VG]" or "[VG,MV]". */
        private val MARKERS = Regex("""\[([^\[\]]*)]""")

        private const val CLOSED_KEYWORD = "geschlossen"

        // "Freiwillige Angaben", see the legend at the bottom of the menu page.
        private val PORK_MARKERS = setOf("S", "SAT", "RS", "RSAT")
        private val COW_MARKERS = setOf("R", "RAT", "RS", "RSAT")
        private val CHICKEN_MARKERS = setOf("G", "GAT")
        private val FISH_MARKERS = setOf("MSC")
        private val VEGAN_MARKERS = setOf("VG")
        private val VEGETARIAN_MARKERS = setOf("VEG")

        /** Fish & seafood is only marked as "[MSC]" if it is certified. These allergens catch the remaining meals. */
        private val FISH_ALLERGENS = setOf("Fi", "Kr", "Wt")
    }

    override fun canteen() = Canteen("adenauerring", "Mensa am Adenauerring", link = SWKA_WEBSITE)

    override suspend fun foodAtDate(date: LocalDate): List<CanteenLine> {
        val html = request(numberOfWeek(date)) ?: return emptyList()
        return parseMenu(html, date)
    }

    /**
     * Extract the lines of a certain day from the weekly menu.
     * Everything that cannot be understood is skipped instead of failing the whole menu.
     */
    internal fun parseMenu(
        html: String,
        date: LocalDate
    ): List<CanteenLine> {
        val tableOfDay = tableOfDay(Jsoup.parse(html), date) ?: return emptyList()
        return tableOfDay
            .select("td[width=20%] + td")
            .mapNotNull { parseLine(it) }
            .sortedBy { it.name }
    }

    private fun tableOfDay(
        document: Document,
        date: LocalDate
    ): Element? {
        val day = "${date.day.pad()}.${date.month.pad()}."
        val headings = document.select("h1").filter { it.text().contains(day) }
        if (headings.isEmpty()) {
            logger.info("Found no menu for {}. Probably the canteen has not published it (yet).", day)
            return null
        }
        if (headings.size > 1) {
            logger.warn("Found {} menus for {}. Using the first one.", headings.size, day)
        }

        val table = headings.first().nextElementSibling()
        if (table == null || table.tagName() != "table") {
            logger.error("Found no menu table for {}", day)
            return null
        }
        return table
    }

    private fun parseLine(lineCell: Element): CanteenLine? {
        val name = lineCell.previousElementSibling()?.let { nameOfLine(it) } ?: return null
        if (LINES_TO_CONSIDER.none { name.startsWith(it) }) {
            return null
        }

        val meals =
            lineCell
                .select("tr")
                .mapNotNull { parseMeal(it) }
                .filterNot { it.name.contains(CLOSED_KEYWORD, ignoreCase = true) }

        return if (meals.isEmpty()) null else CanteenLine(name, meals)
    }

    /**
     * Build the name of a line from its header, e.g., "[pizza]werk<br>Pizza<br>11-14 Uhr" becomes "[pizza]werk Pizza (11-14 Uhr)".
     */
    private fun nameOfLine(header: Element): String {
        // A <br> becomes a line break within the whole text of the header ..
        val segments =
            header
                .wholeText()
                .split('\n')
                .map { it.trim() }
                .filter { it.isNotBlank() }
        if (segments.isEmpty()) return header.text().trim()

        val (openingHours, nameSegments) = segments.partition { OPENING_HOURS.matches(it) }
        val name = nameSegments.joinToString(" ").ifBlank { segments.joinToString(" ") }
        return if (openingHours.isEmpty()) name else "$name (${openingHours.joinToString(", ")})"
    }

    private fun parseMeal(row: Element): Meal? {
        // <tr><td>[VG]</td><td class="first dot"><span class="bg">..</span></td><td><span class="bg">3,55 €</span></td></tr>
        val cells = row.children().filter { it.tagName() == "td" }
        if (cells.size < 2) {
            // No meal at all, e.g., the placeholder of a line without a menu
            if (logger.isDebugEnabled) logger.debug("Unknown data in $row")
            return null
        }

        val nameElement = cells[1].selectFirst("span.bg")
        if (nameElement == null) {
            if (logger.isDebugEnabled) logger.debug("Unknown data in $row")
            return null
        }

        val name = nameOfMeal(nameElement)
        if (name.isBlank()) {
            return null
        }

        val markers = parseMarkers(cells[0].text())
        val allergens = parseAllergens(nameElement)

        return Meal(
            name = name,
            fish = markers.any { it in FISH_MARKERS } || allergens.any { it in FISH_ALLERGENS },
            pork = markers.any { it in PORK_MARKERS },
            cow = markers.any { it in COW_MARKERS },
            vegan = markers.any { it in VEGAN_MARKERS },
            vegetarian = markers.any { it in VEGETARIAN_MARKERS },
            chicken = markers.any { it in CHICKEN_MARKERS }
        )
    }

    /**
     * Build the name of a meal from its title and its (optional) description.
     * E.g., "<b>Hausgemachte Gnocchi</b> <span>vegane Veloute mit Gemüse Kokos</span> (So,Se,Sf,We)"
     * becomes "Hausgemachte Gnocchi – vegane Veloute mit Gemüse Kokos".
     */
    private fun nameOfMeal(nameElement: Element): String {
        val children = nameElement.children()
        val title =
            children
                .firstOrNull { it.tagName() == "b" }
                ?.text()
                ?.trim()
                .orEmpty()
        val description =
            children
                .filter { it.tagName() == "span" }
                .joinToString(" ") { it.text().trim() }
                .trim()

        if (title.isBlank() && description.isBlank()) {
            // Unknown markup: use everything but the trailing allergens
            return ALLERGENS.replace(nameElement.text().trim(), "").trim()
        }
        return listOf(title, description).filter { it.isNotBlank() }.joinToString(" – ")
    }

    private fun parseMarkers(markers: String): Set<String> {
        if (markers.isBlank()) return emptySet()

        val parsedMarkers =
            MARKERS
                .findAll(markers)
                .flatMap { it.groupValues[1].split(",") }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()
        if (parsedMarkers.isEmpty() && logger.isDebugEnabled) {
            logger.debug("Found no markers in '{}'", markers)
        }
        return parsedMarkers
    }

    /** The allergens are the only text of the name element that does not belong to one of its children. */
    private fun parseAllergens(nameElement: Element): Set<String> {
        val allergens = ALLERGENS.find(nameElement.ownText().trim()) ?: return emptySet()
        return allergens.groupValues[1]
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    /** Load the menu of a week. Returns null if the canteen cannot be reached. */
    private suspend fun request(weekOfYear: Int): String? {
        val url = SWKA_WEBSITE_API.replace("%%%WoY%%%", weekOfYear.toString())
        return HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_IN_MS
                connectTimeoutMillis = REQUEST_TIMEOUT_IN_MS
                socketTimeoutMillis = REQUEST_TIMEOUT_IN_MS
            }
        }.use { client ->
            try {
                val response = client.request(url) { method = HttpMethod.Get }
                if (response.status.isSuccess()) {
                    response.body<String>()
                } else {
                    logger.error("Could not load the menu of week {}: {}", weekOfYear, response.status)
                    null
                }
            } catch (e: Exception) {
                logger.error("Could not load the menu of week $weekOfYear", e)
                null
            }
        }
    }
}

private fun Int.pad(): String = if (this < 10) "0$this" else this.toString()

private fun Month.pad(): String {
    val numberOfMonth = this.ordinal + 1
    return if (numberOfMonth < 10) "0$numberOfMonth" else numberOfMonth.toString()
}
