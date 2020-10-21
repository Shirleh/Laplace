package com.github.shirleh.statistics

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.long
import com.github.shirleh.command.cli.AbstractCommand
import com.github.shirleh.command.cli.AbstractCommandCategory
import com.github.shirleh.extensions.await
import com.github.shirleh.extensions.orElseNull
import com.influxdb.client.kotlin.QueryKotlinApi
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions
import discord4j.common.util.Snowflake
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

    private val range by option("-r", "--range", help = "time range in days (1-30); defaults to 1").long().default(1L)

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun execute(event: MessageCreateEvent) {
        val guildId = event.guildId.map(Snowflake::asString).orElseNull() ?: return

        val flux = Flux.from("raw_discord_data")
            .range(-range, ChronoUnit.DAYS)
            .filter(Restrictions.measurement().equal("message_count"))
            .filter(Restrictions.tag("guildId").equal(guildId))
            .groupBy("channel")
            .sum()
            .group()
            .sort(true)
            .limit(10)
        val topChannels = flux.toString().also { logger.debug { "Query: $it" } }
        val results = queryApi.query(topChannels)

        val channel = event.message.channel.await()
        val message = StringBuilder()
            .apply {
                results.consumeEach {
                    appendLine("Channel: ${it.getValueByKey("channel")} count: ${it.value}")
                }
            }
            .toString()
        channel
            .createEmbed { spec ->
                spec.setDescription(message)
                    .setColor(Color.WHITE)
            }
            .await()
    }
}
