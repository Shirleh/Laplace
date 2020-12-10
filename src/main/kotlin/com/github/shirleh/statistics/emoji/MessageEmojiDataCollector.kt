package com.github.shirleh.statistics.emoji

import com.github.shirleh.administration.ChannelRepository
import com.github.shirleh.extensions.orElseNull
import com.github.shirleh.statistics.privacy.PrivacySettingsRepository
import com.vdurmont.emoji.EmojiParser
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.FlowPreview
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

    private val logger = KotlinLogging.logger { }

    private val channelRepository: ChannelRepository by inject()
    private val privacySettingsRepository: PrivacySettingsRepository by inject()
    private val emojiPointRepository: EmojiPointRepository by inject()


    /**
     * Collects emoji data from the incoming [events].
     */
    @OptIn(FlowPreview::class)
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
        .filter { context -> !context.user.isBot }
        .flatMapConcat { context ->
            flowOf(context)
                .filter { channelRepository.findAll(it.guildId.asLong()).contains(it.channelId.asLong()) }
                .map(this::aggregateEmojiData)
                .filter(List<EmojiPoint>::isNotEmpty)
                .onEach(emojiPointRepository::save)
                .catch { error -> logger.catching(error) }
        }

    private suspend fun aggregateEmojiData(context: Context): List<EmojiPoint> {
        val privacySettings = privacySettingsRepository
            .findByUserAndGuild(context.user.id.asLong(), context.guildId.asLong())

        return parseToEmojis(
            message = context.message,
            guildId = context.guildId,
            userId = if (privacySettings?.emoji == true) context.user.id else null
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun parseToEmojis(message: String, guildId: Snowflake, userId: Snowflake?): List<EmojiPoint> {
        logger.entry(message, guildId, userId)

        val result = buildList {
            addAll(extractUnicodeEmojis(message, guildId, userId))
            addAll(extractCustomEmojis(message, guildId, userId))
        }

        return logger.exit(result)
    }

    private fun extractUnicodeEmojis(input: String, guildId: Snowflake, userId: Snowflake?) =
        EmojiParser.extractEmojis(input)
            .map(MessageEmojiDataCollector::removeFitzPatrickModifier)
            .map {
                EmojiPoint(
                    guildId = guildId.asString(),
                    userId = userId?.asString(),
                    source = Source.MESSAGE,
                    type = Type.UNICODE,
                    id = it
                )
            }

    private fun removeFitzPatrickModifier(emojiFullUnicode: String) =
        EmojiParser.parseFromUnicode(emojiFullUnicode) { it.emoji.unicode }

    private fun extractCustomEmojis(input: String, guildId: Snowflake, userId: Snowflake?) =
        input.split(" ")
            .filter { it.startsWith(CUSTOM_EMOJI_PREFIX) && it.endsWith(CUSTOM_EMOJI_SUFFIX) }
            .map { it.removeSurrounding(CUSTOM_EMOJI_PREFIX, CUSTOM_EMOJI_SUFFIX).split(":") }
            .filter { it.size == 2 }
            .map {
                EmojiPoint(
                    guildId = guildId.asString(),
                    userId = userId?.asString(),
                    source = Source.MESSAGE,
                    type = Type.CUSTOM,
                    id = it[1]
                )
            }
}
