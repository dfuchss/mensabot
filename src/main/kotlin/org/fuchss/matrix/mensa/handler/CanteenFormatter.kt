package org.fuchss.matrix.mensa.handler

import de.connect2x.trixnity.core.model.RoomId
import org.fuchss.matrix.bots.MatrixBot
import org.fuchss.matrix.bots.helper.markdown
import org.fuchss.matrix.mensa.TranslationService
import org.fuchss.matrix.mensa.api.CanteenApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime

private val logger: Logger = LoggerFactory.getLogger(MatrixBot::class.java)

@OptIn(ExperimentalTime::class)
suspend fun sendCanteenEventToRoom(
    roomId: RoomId,
    matrixBot: MatrixBot,
    scheduled: Boolean,
    canteens: List<CanteenApi>,
    translationService: TranslationService
) {
    logger.info("Sending Mensa to Room ${roomId.full}")

    for (canteen in canteens) {
        val mensaToday = canteen.foodToday()
        val noFood = mensaToday.isEmpty() || mensaToday.all { (_, meals) -> meals.isEmpty() }

        if (noFood && scheduled) {
            logger.debug("Skipping sending of mensa plan to {} as there will be no food today.", roomId)
            return
        }

        val mensa = canteen.canteen()
        val title = if (mensa.link == null) "## ${mensa.name}\n" else "## [${mensa.name}](<${mensa.link}>)\n"

        var meals = ""
        for (l in mensaToday) {
            if (l.meals.isEmpty()) {
                continue
            }
            meals += if (l.name.isNotBlank()) "### ${l.name}\n" else ""
            for (meal in l.meals) meals += "* ${meal.entry()}\n"
        }

        if (meals.isEmpty()) {
            meals = "### Heute hier kein Essen.\n"
        } else {
            meals = translationService.translate(meals).trim()
            // Crop translation indications ..
            if (meals.contains("#") && !meals.startsWith("#")) {
                meals = meals.substring(meals.indexOf("#"))
            }
        }
        val response = title + meals
        matrixBot.room().sendMessage(roomId) { markdown(response) }
    }
}
