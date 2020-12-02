package com.github.shirleh.statistics.message

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
import com.influxdb.query.dsl.functions.restriction.Restrictions.*
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.util.Color
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import mu.KotlinLogging
import org.koin.core.inject
import java.time.temporal.ChronoUnit

class MessageStatistic : AbstractCommandCategory(
    name = "message",
    help = """Show message statistics."""
)

class ActivityCommand : AbstractCommand(
    name = "activity",
    help = """Show channel activity."""
) {
    private val logger = KotlinLogging.logger { }

    private val queryApi: QueryKotlinApi by inject()

    private val range by option("-r", "--range", help = "time range in days (1-30); defaults to 7").long().default(7L)
        .validate {
            require(it > 0) { "time range must be 1 or higher" }
            require(it <= 30) { "time range must be 30 or lower" }
        }
    private val isPersonal by option("--me", help = "show only your personal statistics").flag()
    private val channelFilter by option("-c", "--channel", help = "filter statistics by the given channel")

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun execute(event: MessageCreateEvent) {
        logger.entry(event)

        val guildId = event.guildId.map(Snowflake::asString).orElseNull() ?: return
        val userId = event.message.author.map(User::getId).orElseNull() ?: return
        val channelId = channelFilter?.removeSurrounding("<#", ">")

        val flux = Flux.from("raw_discord_data")
            .range(-range, ChronoUnit.DAYS)
            .filter(measurement().equal("message"))
            .filter(field().equal("wordCount"))
            .filter(tag("guildId").equal(guildId))
            .let { if (isPersonal) it.filter(tag("userId").equal(userId.asString())) else it }
            .let { if (channelId != null) it.filter(tag("channel").equal(channelId)) else it }
            .groupBy("channel")
            .sum()
            .sort(true)
            .limit(10)
        val topChannels = flux.toString().also { logger.debug { "Query: $it" } }

        val fluxRecords = queryApi.query(topChannels)
        val results = mutableMapOf<String, Long>().apply {
            fluxRecords.consumeEach {
                val channel = "<#${it.getValueByKey("channel")}>"
                val count = it.value as Long
                put(channel, count)
            }
        }

        event.message.channel.await()
            .createEmbed { spec ->
                val channelList = results.keys
                    .fold(StringBuilder()) { acc, s -> acc.appendLine(s) }
                    .let { if (it.isEmpty()) "-" else it.toString() }
                val frequencyList = results.values
                    .fold(StringBuilder()) { acc, l -> acc.appendLine(l) }
                    .let { if (it.isEmpty()) "-" else it.toString() }

                spec.addField("Channel", channelList, true)
                    .addField("Messages", frequencyList, true)
                    .setColor(Color.WHITE)
            }
            .await()

        logger.exit()
    }
}
