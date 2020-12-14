package com.github.shirleh.statistics.ban

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.long
import com.github.shirleh.command.cli.AbstractCommand
import com.github.shirleh.command.cli.AbstractCommandCategory
import com.github.shirleh.extensions.await
import com.github.shirleh.extensions.orElseNull
import com.influxdb.client.kotlin.QueryKotlinApi
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.util.Color
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.receiveOrNull
import mu.KotlinLogging
import org.koin.core.inject
import java.time.temporal.ChronoUnit

class BanStatisticsCommands : AbstractCommandCategory(
    name = "ban",
    help = """Show ban statistics.""",
)

class ShowBanRankingCommand : AbstractCommand(
    name = "ranking",
    help = """Show the top moderators with the most bans""",
) {
    private val logger = KotlinLogging.logger { }

    private val queryApi: QueryKotlinApi by inject()

    private val range by option("-r", "--range", help = "time range in days (1-30); defaults to 7").long().default(7L)
        .validate {
            require(it > 0) { "time range must be 1 or higher" }
            require(it <= 30) { "time range must be 30 or lower" }
        }
    private val excludeBots by option("--exclude-bots").flag()

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun execute(event: MessageCreateEvent) {
        logger.entry(event)

        val guildId = event.guildId.map(Snowflake::asString).orElseNull() ?: return

        val flux = Flux.from("raw_discord_data")
            .range(-range, ChronoUnit.DAYS)
            .filter(Restrictions.measurement().equal("ban"))
            .filter(Restrictions.tag("guildId").equal(guildId))
            .let { if (excludeBots) it.filter(Restrictions.tag("isBot").equal("${false}")) else it }
            .filter(Restrictions.field().equal("count"))
            .sum()
            .sort(true)
            .limit(10)
        val topBanners = flux.toString().also { logger.debug { "Query: $it" } }

        val fluxRecords = queryApi.query(topBanners)
        val results = mutableMapOf<String, Long>().apply {
            fluxRecords.consumeEach {
                val author = "<@${it.getValueByKey("author")}>"
                val count = it.value as Long
                put(author, count)
            }
        }

        event.message.channel.await()
            .createEmbed { spec ->
                val authorList = results.keys
                    .fold(StringBuilder()) { acc, s -> acc.appendLine(s) }
                    .let { if (it.isEmpty()) "-" else it.toString() }
                val frequencyList = results.values
                    .fold(StringBuilder()) { acc, l -> acc.appendLine(l) }
                    .let { if (it.isEmpty()) "-" else it.toString() }

                spec.addField("Moderator", authorList, true)
                    .addField("Bans", frequencyList, true)
                    .setColor(Color.WHITE)
            }
            .await()

        logger.exit()
    }
}

class ShowBanCountCommand : AbstractCommand(
    name = "count",
    help = """Show number of bans""",
) {
    private val logger = KotlinLogging.logger { }

    private val queryApi: QueryKotlinApi by inject()

    private val range by option("-r", "--range", help = "time range in days (1-30); defaults to 7").long().default(7L)
        .validate {
            require(it > 0) { "time range must be 1 or higher" }
            require(it <= 30) { "time range must be 30 or lower" }
        }
    private val isPersonal by option("--me", help = "show only your personal statistics").flag()
    private val author by option("-mod", "--moderator", help = "show ban count of moderator with specified id")

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun execute(event: MessageCreateEvent) {
        logger.entry(event)

        val guildId = event.guildId.map(Snowflake::asString).orElseNull() ?: return
        val userId = event.message.author.map(User::getId).orElseNull() ?: return
        val author = author

        val flux = Flux.from("raw_discord_data")
            .range(-range, ChronoUnit.DAYS)
            .filter(Restrictions.measurement().equal("ban"))
            .filter(Restrictions.tag("guildId").equal(guildId))
            .filter(Restrictions.field().equal("count"))
            .let { if (isPersonal) it.filter(Restrictions.tag("author").equal(userId.asString())) else it }
            .let { if (author != null) it.filter(Restrictions.tag("author").equal(author)) else it }
            .sum()
            .sort(true)
            .limit(10)
        val banCount = flux.toString().also { logger.debug { "Query: $it" } }

        val fluxRecords = queryApi.query(banCount)
        val result = fluxRecords.receiveOrNull()?.let { it.value as Long } ?: 0L

        event.message.channel.await()
            .createMessage("$result")
            .await()

        logger.exit()
    }
}
