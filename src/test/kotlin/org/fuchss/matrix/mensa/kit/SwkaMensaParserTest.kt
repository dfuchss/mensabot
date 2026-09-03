package org.fuchss.matrix.mensa.kit

import kotlinx.datetime.LocalDate
import org.fuchss.matrix.mensa.api.CanteenLine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

/**
 * Tests for the parsing of the menu of the "Mensa am Adenauerring" based on a stored menu of the canteen.
 */
@ExperimentalTime
class SwkaMensaParserTest {
    private val mensa = SwkaMensa()
    private val menu = SwkaMensaParserTest::class.java.getResource("/swka-adenauerring-2026-kw36.html")!!.readText()

    private fun linesAt(
        year: Int,
        month: Int,
        day: Int
    ) = mensa.parseMenu(menu, LocalDate(year, month, day))

    private fun List<CanteenLine>.line(name: String) = firstOrNull { it.name == name }

    @Test
    fun `parses all relevant lines of a day`() {
        val lines = linesAt(2026, 9, 3)
        assertEquals(
            listOf(
                "Linie 1 Gut & Günstig",
                "Linie 2 Vegane Linie",
                "Linie 3",
                "Linie 4",
                "Schnitzelbar",
                "[pizza]werk Pizza (11-14 Uhr)",
                "[pizza]werk Salate / Vorspeisen"
            ),
            lines.map { it.name }
        )
    }

    @Test
    fun `keeps the description of a meal`() {
        val meals = linesAt(2026, 9, 3).line("Linie 4")!!.meals.map { it.name }
        assertTrue(meals.contains("Hausgemachte Gnocchi – vegane Veloute mit Gemüse Kokos"), "Description of the meal is missing: $meals")
        assertTrue(meals.contains("Hausgemachte Gnocchi con Salsiccia"), "Meal without description is broken: $meals")
        assertTrue(meals.contains("Blattsalat – Gärtnerinsalat"), "Description of the meal is missing: $meals")
    }

    @Test
    fun `strips the allergens from the name of a meal`() {
        val meals = linesAt(2026, 9, 3).line("Linie 4")!!.meals.map { it.name }
        assertTrue(meals.none { it.contains("(") }, "Allergens leaked into the name of a meal: $meals")
    }

    @Test
    fun `omits the opening hours from the name of a line`() {
        val lines = linesAt(2026, 9, 4).map { it.name }
        assertTrue(lines.contains("[pizza]werk Pizza (11-14 Uhr)"), "Opening hours are not normalized: $lines")
    }

    @Test
    fun `skips closed lines`() {
        val lines = linesAt(2026, 9, 3).map { it.name }
        // Linie 5, Linie 6 and the [kœri]werk are closed at that day
        assertFalse(lines.contains("Linie 5"), lines.toString())
        assertFalse(lines.contains("Linie 6"), lines.toString())
        assertTrue(lines.none { it.startsWith("[kœri]werk") }, lines.toString())
    }

    @Test
    fun `parses the dietary markers of a meal`() {
        val meals = linesAt(2026, 9, 3).line("Linie 4")!!.meals.associateBy { it.name }

        val pork = meals["Hausgemachte Gnocchi con Salsiccia"]!!
        assertTrue(pork.pork)
        assertFalse(pork.vegan)

        val vegan = meals["Hausgemachte Gnocchi – vegane Veloute mit Gemüse Kokos"]!!
        assertTrue(vegan.vegan)
        assertFalse(vegan.pork)

        val cow = linesAt(2026, 9, 3).line("Linie 1 Gut & Günstig")!!.meals.first { it.name == "Pasta Bolognese" }
        assertTrue(cow.cow, "[RAT] is not recognized as beef")
    }

    @Test
    fun `returns nothing for a day that is not part of the menu`() {
        assertTrue(linesAt(2026, 9, 7).isEmpty())
    }

    @Test
    fun `recognizes meals that contain both pork and beef`() {
        val meals = mealsOf("""<td>[RS]</td><td class="first dot"><span class="bg"><b>Frikadelle mit Brötchen</b>(Ei,We,Ge)</span></td>""")
        assertEquals(1, meals.size)
        assertTrue(meals[0].pork, "[RS] is not recognized as pork")
        assertTrue(meals[0].cow, "[RS] is not recognized as beef")
    }

    @Test
    fun `recognizes fish by its allergens`() {
        val meals = mealsOf("""<td></td><td class="first dot"><span class="bg"><b>Insalata piccola</b> <span>mit Thunfisch</span>(Fi,Sn)</span></td>""")
        assertEquals(1, meals.size)
        assertTrue(meals[0].fish, "Fish is not recognized by its allergens")
    }

    @Test
    fun `survives unknown markup`() {
        // A row without any meal, a row with an unexpected structure and a meal without a title
        val meals =
            mealsOf(
                """<td colspan="2"><div>-</div></td>""",
                """<td>[VG]</td><td class="first dot">Pommes</td>""",
                """<td>[VG]</td><td class="first dot"><span class="bg">Pommes (We)</span></td>"""
            )
        assertEquals(1, meals.size)
        assertEquals("Pommes", meals[0].name)
        assertTrue(meals[0].vegan)
    }

    @Test
    fun `ignores lines that are not considered`() {
        assertNull(linesAt(2026, 9, 3).line("Cafeteria (11-14 Uhr)"))
        assertNotNull(linesAt(2026, 9, 3).line("Linie 3"))
    }

    /** Build a minimal menu of a single day that contains the given rows within "Linie 1". */
    private fun mealsOf(vararg rows: String): List<org.fuchss.matrix.mensa.api.Meal> {
        val html =
            """
            <h1>Do 03.09.</h1>
            <table><tr><td width="20%">Linie 1</td><td><table>
            ${rows.joinToString("\n") { "<tr>$it</tr>" }}
            </table></td></tr></table>
            """.trimIndent()
        return mensa.parseMenu(html, LocalDate(2026, 9, 3)).flatMap { it.meals }
    }
}
