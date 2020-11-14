package com.github.shirleh.statistics.emoji

import com.github.shirleh.administration.ChannelRepository
import com.github.shirleh.extensions.orElseNull
import com.github.shirleh.persistence.influx.DataPointRepository
import com.github.shirleh.statistics.privacy.PrivacySettingsRepository
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.message.ReactionAddEvent
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.koin.core.KoinComponent
import org.koin.core.inject

object ReactionEmojiDataCollector : KoinComponent {

    private data class Context(
        val guildId: Snowflake,
        val channelId: Snowflake,
        val member: Member,
        val reactionEmoji: ReactionEmoji,
    )

    private val channelRepository: ChannelRepository by inject()
    private val dataPointRepository: DataPointRepository by inject()

    private val logger = KotlinLogging.logger { }

    /**
     * Collects emoji data from the incoming [events].
     */
    fun addListener(events: Flow<ReactionAddEvent>) = events
        .buffer()
        .mapNotNull { event ->
            Context(
                guildId = event.guildId.orElseNull() ?: return@mapNotNull null,
                channelId = event.channelId,
                member = event.member.orElseNull() ?: return@mapNotNull null,
                reactionEmoji = event.emoji
            )
        }
        .filter { context -> channelRepository.findAll(context.guildId.asLong()).contains(context.channelId.asLong()) }
        .filter { context -> !context.member.isBot }
        .map { context ->
            toEmoji(
                reactionEmoji = context.reactionEmoji,
                guildId = context.guildId.asString(),
                channelId = context.channelId.asString()
            )
        }
        .onEach { logger.debug { it } }
        .map(Emoji::toDataPoint)
        .onEach(dataPointRepository::save)
        .catch { error -> logger.catching(error) }

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
