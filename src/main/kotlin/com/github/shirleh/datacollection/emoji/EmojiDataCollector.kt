package com.github.shirleh.datacollection.emoji

import com.github.shirleh.datacollection.DataPointRepository
import com.vdurmont.emoji.EmojiParser
import discord4j.common.util.Snowflake
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.ReactionAddEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import org.koin.core.KoinComponent
import org.koin.core.inject

private const val CUSTOM_EMOJI_PREFIX = "<:"
private const val CUSTOM_EMOJI_SUFFIX = ">"

object EmojiDataCollector : KoinComponent {

    private val logger = KotlinLogging.logger { }

    private val dataPointRepository: DataPointRepository by inject()

    /**
     * Collects emoji data from the incoming [events].
     */
    suspend fun collectFromMessages(events: Flow<MessageCreateEvent>) {
        events
            .filter { it.guildId.isPresent }
            .map(EmojiDataCollector::parseToEmojis)
            .map { it.map(Emoji::toDataPoint) }
            .collect(dataPointRepository::save)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun parseToEmojis(event: MessageCreateEvent): List<Emoji> {
        logger.entry(event)

        val guildId = event.guildId.map { it.asString() }.orElseThrow()
        val channelId = event.message.channelId.asString()
        val messageContent = event.message.content

        val result = buildList {
            addAll(extractUnicodeEmojis(messageContent, guildId, channelId))
            addAll(extractCustomEmojis(messageContent, guildId, channelId))
        }

        return logger.exit(result)
    }

    private fun extractUnicodeEmojis(input: String, guildId: String, channelId: String) =
        EmojiParser.extractEmojis(input)
            .map(EmojiDataCollector::removeFitzPatrickModifier)
            .map {
                Emoji(
                    guildId = guildId,
                    channelId = channelId,
                    source = Source.MESSAGE,
                    type = Type.UNICODE,
                    value = it
                )
            }

    private fun removeFitzPatrickModifier(emojiFullUnicode: String) =
        EmojiParser.parseFromUnicode(emojiFullUnicode) { it.emoji.unicode }

    private fun extractCustomEmojis(input: String, guildId: String, channelId: String) =
        input.split(" ")
            .filter { it.startsWith(CUSTOM_EMOJI_PREFIX) && it.endsWith(CUSTOM_EMOJI_SUFFIX) }
            .map { it.removeSurrounding(CUSTOM_EMOJI_PREFIX, CUSTOM_EMOJI_SUFFIX).split(":") }
            .filter { it.size == 2 }
            .map {
                Emoji(
                    guildId = guildId,
                    channelId = channelId,
                    source = Source.MESSAGE,
                    type = Type.CUSTOM,
                    value = it[1]
                )
            }

    /**
     * Collects emoji data from the incoming [events].
     */
    suspend fun collectFromReactionAdds(events: Flow<ReactionAddEvent>) {
        events
            .filter { it.guildId.isPresent }
            .map {
                toEmoji(
                    reactionEmoji = it.emoji,
                    guildId = it.guildId.map(Snowflake::asString).orElseThrow(),
                    channelId = it.channelId.asString()
                )
            }
            .map(Emoji::toDataPoint)
            .collect(dataPointRepository::save)
    }

    private fun toEmoji(reactionEmoji: ReactionEmoji, guildId: String, channelId: String): Emoji {
        logger.entry(reactionEmoji, guildId, channelId)

        val (type, value) =
            reactionEmoji.asUnicodeEmoji()
                .map { Pair(Type.UNICODE, it.raw) }
                .orElseGet {
                    reactionEmoji.asCustomEmoji()
                        .map { Pair(Type.CUSTOM, it.id.asString()) }
                        .orElseThrow()
                }
        val result = Emoji(
            guildId = guildId,
            channelId = channelId,
            source = Source.REACTION,
            type = type,
            value = value
        )

        return logger.exit(result)
    }
}