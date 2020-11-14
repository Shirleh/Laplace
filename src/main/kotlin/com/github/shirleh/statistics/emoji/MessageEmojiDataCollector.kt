package com.github.shirleh.statistics.emoji

import com.github.shirleh.administration.ChannelRepository
import com.github.shirleh.extensions.orElseNull
import com.github.shirleh.persistence.influx.DataPointRepository
import com.github.shirleh.statistics.privacy.PrivacySettingsRepository
import com.vdurmont.emoji.EmojiParser
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.koin.core.KoinComponent
import org.koin.core.inject

private const val CUSTOM_EMOJI_PREFIX = "<:"
private const val CUSTOM_EMOJI_SUFFIX = ">"

object MessageEmojiDataCollector : KoinComponent {

    private data class Context(
        val guildId: Snowflake,
        val channelId: Snowflake,
        val user: User,
        val message: String,
    )

    private val channelRepository: ChannelRepository by inject()
    private val dataPointRepository: DataPointRepository by inject()

    private val logger = KotlinLogging.logger { }

    /**
     * Collects emoji data from the incoming [events].
     */
    fun addListener(events: Flow<MessageCreateEvent>) = events
        .buffer()
        .mapNotNull { event ->
            Context(
                guildId = event.guildId.orElseNull() ?: return@mapNotNull null,
                channelId = event.message.channelId,
                user = event.message.author.orElseNull() ?: return@mapNotNull null,
                message = event.message.content,
            )
        }
        .filter { context -> channelRepository.findAll(context.guildId.asLong()).contains(context.channelId.asLong()) }
        .filter { context -> !context.user.isBot }
        .map { context ->
            parseToEmojis(
                message = context.message,
                guildId = context.guildId,
                channelId = context.channelId
            )
        }
        .filter(List<Emoji>::isNotEmpty)
        .onEach { logger.debug { "Emojis $it" } }
        .map { emojis -> emojis.map(Emoji::toDataPoint) }
        .onEach(dataPointRepository::save)
        .catch { error -> logger.catching(error) }

    @OptIn(ExperimentalStdlibApi::class)
    private fun parseToEmojis(message: String, guildId: Snowflake, channelId: Snowflake): List<Emoji> {
        logger.entry(message, guildId, channelId)

        val result = buildList {
            addAll(extractUnicodeEmojis(message, guildId, channelId))
            addAll(extractCustomEmojis(message, guildId, channelId))
        }

        return logger.exit(result)
    }

    private fun extractUnicodeEmojis(input: String, guildId: Snowflake, channelId: Snowflake) =
        EmojiParser.extractEmojis(input)
            .map(MessageEmojiDataCollector::removeFitzPatrickModifier)
            .map {
                Emoji(
                    guildId = guildId.asString(),
                    channelId = channelId.asString(),
                    source = Source.MESSAGE,
                    type = Type.UNICODE,
                    value = it
                )
            }

    private fun removeFitzPatrickModifier(emojiFullUnicode: String) =
        EmojiParser.parseFromUnicode(emojiFullUnicode) { it.emoji.unicode }

    private fun extractCustomEmojis(input: String, guildId: Snowflake, channelId: Snowflake) =
        input.split(" ")
            .filter { it.startsWith(CUSTOM_EMOJI_PREFIX) && it.endsWith(CUSTOM_EMOJI_SUFFIX) }
            .map { it.removeSurrounding(CUSTOM_EMOJI_PREFIX, CUSTOM_EMOJI_SUFFIX).split(":") }
            .filter { it.size == 2 }
            .map {
                Emoji(
                    guildId = guildId.asString(),
                    channelId = channelId.asString(),
                    source = Source.MESSAGE,
                    type = Type.CUSTOM,
                    value = it[1]
                )
            }
}
