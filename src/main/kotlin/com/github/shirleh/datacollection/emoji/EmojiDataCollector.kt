package com.github.shirleh.datacollection.emoji

import com.github.shirleh.datacollection.DataPointRepository
import com.vdurmont.emoji.EmojiParser
import discord4j.core.event.domain.message.MessageCreateEvent
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
}
