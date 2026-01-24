package org.fuchss.matrix.mensa

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.connect2x.trixnity.core.model.RoomId
import org.fuchss.matrix.bots.IConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

/**
 * This is the configuration template of the mensa bot.
 * @param[prefix] the command prefix the bot listens to. By default, "mensa"
 * @param[baseUrl] the base url of the matrix server the bot shall use
 * @param[username] the username of the bot's account
 * @param[password] the password of the bot's account
 * @param[dataDirectory] the path to the databases and media folder
 * @param[timeToSendUpdates] the time the bot shall send updates about the meals every day (to subscribed rooms)
 * @param[admins] the matrix ids of the admins. E.g. "@user:invalid.domain"
 * @param[subscribers] the room ids of rooms that subscribed updates
 * @param[canteensForSubscribers] the canteens the bot shall provide information about (only affects subscribers)
 * @param[translation] the configuration for translations (optional, alpha)
 */
data class Config(
    @param:JsonProperty override val prefix: String = "mensa",
    @param:JsonProperty override val baseUrl: String,
    @param:JsonProperty override val username: String,
    @param:JsonProperty override val password: String,
    @param:JsonProperty override val dataDirectory: String,
    @param:JsonProperty override val admins: List<String>,
    @param:JsonProperty override val users: List<String> = listOf(),
    @param:JsonProperty val timeToSendUpdates: LocalTime,
    @param:JsonProperty val subscribers: List<String>,
    @param:JsonProperty val canteensForSubscribers: List<String> = listOf("adenauerring"),
    @param:JsonProperty val translation: TranslationConfig? = null
) : IConfig {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(Config::class.java)

        /**
         * Load the config from the file path. You can set "CONFIG_PATH" in the environment to override the default location ("./config.json").
         */
        fun load(): Config {
            val configPath = System.getenv("CONFIG_PATH") ?: "./config.json"
            val configFile = File(configPath)
            if (!configFile.exists()) {
                error("Config ${configFile.absolutePath} does not exist!")
            }

            val config: Config = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule()).readValue(configFile)
            log.info("Loaded config ${configFile.absolutePath}")
            config.validate()
            return config
        }
    }

    /**
     * Get all subscriptions (rooms that want daily meal updates) as [RoomId].
     */
    fun subscriptions() = subscribers.map { RoomId(it) }

    /**
     * Get the next time of the next reminder (notification to [subscriptions]).
     */
    fun nextReminder(): Date {
        var nextUpdate = timeToSendUpdates.atDate(LocalDate.now()).atZone(ZoneId.systemDefault())
        if (!nextUpdate.toInstant().isAfter(Instant.now())) {
            // Else use tomorrow ..
            nextUpdate = nextUpdate.plusDays(1)
        }
        return Date.from(nextUpdate.toInstant())
    }
}
