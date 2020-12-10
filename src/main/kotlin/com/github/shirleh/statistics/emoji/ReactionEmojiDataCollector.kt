package com.github.shirleh.statistics.emoji

import com.github.shirleh.administration.ChannelRepository
import com.github.shirleh.extensions.orElseNull
import com.github.shirleh.statistics.privacy.PrivacySettingsRepository
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.message.ReactionAddEvent
import kotlinx.coroutines.FlowPreview
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

    private val logger = KotlinLogging.logger { }

    private val channelRepository: ChannelRepository by inject()
    private val privacySettingsRepository: PrivacySettingsRepository by inject()
    private val emojiPointRepository: EmojiPointRepository by inject()

    /**
     * Collects emoji data from the incoming [events].
     */
    @OptIn(FlowPreview::class)
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
        .filter { context -> !context.member.isBot }
        .flatMapConcat { context ->
            flowOf(context)
                .filter { channelRepository.findAll(it.guildId.asLong()).contains(it.channelId.asLong()) }
                .map(this::aggregateEmojiData)
                .onEach(emojiPointRepository::save)
                .catch { error -> logger.catching(error) }
        }

    private suspend fun aggregateEmojiData(context: Context): EmojiPoint {
        logger.entry(context)

        val privacySettings = privacySettingsRepository
            .findByUserAndGuild(context.member.id.asLong(), context.guildId.asLong())

        val (guildId, _, member, reactionEmoji) = context
        val (type, value) =
            reactionEmoji.asUnicodeEmoji()
                .map { Pair(Type.UNICODE, it.raw) }
                .orElseGet {
                    reactionEmoji.asCustomEmoji()
                        .map { Pair(Type.CUSTOM, it.id.asString()) }
                        .orElseThrow()
                }

        val result = EmojiPoint(
            guildId = guildId.asString(),
            userId = if (privacySettings?.emoji == true) member.id.asString() else null,
            source = Source.REACTION,
            type = type,
            id = value
        )

        return logger.exit(result)
    }
}
