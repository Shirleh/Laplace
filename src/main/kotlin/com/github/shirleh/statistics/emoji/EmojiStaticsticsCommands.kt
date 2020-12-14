package com.github.shirleh.statistics.emoji

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.long
import com.github.shirleh.command.cli.AbstractCommand
import com.github.shirleh.command.cli.AbstractCommandCategory
import com.github.shirleh.extensions.await
import com.github.shirleh.extensions.orElseNull
import com.influxdb.client.kotlin.QueryKotlinApi
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions.measurement
import com.influxdb.query.dsl.functions.restriction.Restrictions.tag
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.User
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.util.Color
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import mu.KotlinLogging
import org.koin.core.inject
import reactor.core.publisher.Mono
import java.time.temporal.ChronoUnit

class EmojiStatistics : AbstractCommandCategory(
    name = "emoji",
    help = """Show emoji statistics."""
)

class ShowEmojiRankingCommand : AbstractCommand(
    name = "ranking",
    help = """Show the top most-used emojis in the server."""
) {
    private val logger = KotlinLogging.logger { }

    private val queryApi: QueryKotlinApi by inject()

    private val range by option("-r", "--range", help = "time range in days (1-30); defaults to 7").long().default(7L)
    private val isPersonal by option("--me", help = "show only your personal statistics").flag()
    private val typeFilter by option("--type", help = "show only UNICODE or CUSTOM emojis").enum<Type>()
    private val sourceFilter by option("--source", help = "show only MESSAGE or REACTION emojis").enum<Source>()

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun execute(event: MessageCreateEvent) {
        logger.entry(event)

        val guildId = event.guildId.map(Snowflake::asString).orElseNull() ?: return
        val userId = event.message.author.map(User::getId).orElseNull() ?: return

        val flux = Flux.from("raw_discord_data")
            .range(-range, ChronoUnit.DAYS)
            .filter(measurement().equal("emoji"))
            .filter(tag("guildId").equal(guildId))
            .let { if (isPersonal) it.filter(tag("userId").equal(userId)) else it }
            .let { if (typeFilter != null) it.filter(tag("type").equal(typeFilter!!.name)) else it }
            .let { if (sourceFilter != null) it.filter(tag("source").equal(sourceFilter!!.name)) else it }
            .sum()
            .group()
            .sort(true)
            .limit(10)
        val topEmojis = flux.toString().also { logger.debug { "Query: $it" } }

        val fluxRecords = queryApi.query(topEmojis)
        val results = mutableMapOf<String, Long>().apply {
            fluxRecords.consumeEach {
                val emojiId = "${it.field}"
                val frequency = it.value as Long
                put(emojiId, frequency)
            }
        }

        val emojiList = results.keys
            .map { if (it.isCustomEmoji()) resolveCustomEmoji(event.guild, it) else it }
            .fold(StringBuilder()) { acc, s -> acc.appendLine(s) }.toString()
        val frequencyList = results.values
            .fold(StringBuilder()) { acc, l -> acc.appendLine(l) }.toString()

        event.message.channel.await()
            .createEmbed { spec ->
                spec.addField("Emoji", emojiList, true)
                    .addField("Frequency", frequencyList, true)
                    .setColor(Color.WHITE)
            }
            .await()

        logger.exit()
    }

    private fun String.isCustomEmoji() = toLongOrNull() != null

    private suspend fun resolveCustomEmoji(guild: Mono<Guild>, customEmojiId: String): String =
        guild.await()
            .getGuildEmojiById(Snowflake.of(customEmojiId)).await()
            .asFormat()
}
