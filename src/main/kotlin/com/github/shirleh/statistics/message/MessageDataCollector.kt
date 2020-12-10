package com.github.shirleh.statistics.message

import com.github.shirleh.administration.ChannelRepository
import com.github.shirleh.extensions.orElseNull
import com.github.shirleh.statistics.privacy.PrivacySettingsRepository
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.time.Instant

object MessageDataCollector : KoinComponent {

    private data class Context(
        val guildId: Snowflake,
        val channelId: Snowflake,
        val user: User,
        val message: String,
        val timestamp: Instant,
    )

    private val channelRepository: ChannelRepository by inject()
    private val privacySettingsRepository: PrivacySettingsRepository by inject()
    private val messagePointRepository: MessagePointRepository by inject()

    private val logger = KotlinLogging.logger { }

    /**
     * Collects message data from the incoming [events].
     */
    @OptIn(FlowPreview::class)
    fun addListener(events: Flow<MessageCreateEvent>) = events
        .mapNotNull { event ->
            Context(
                guildId = event.guildId.orElseNull() ?: return@mapNotNull null,
                channelId = event.message.channelId,
                user = event.message.author.orElseNull() ?: return@mapNotNull null,
                message = event.message.content,
                timestamp = event.message.timestamp,
            )
        }
        .filter { context -> !context.user.isBot }
        .buffer()
        .flatMapConcat { context ->
            flowOf(context)
                .filter { channelRepository.findAll(it.guildId.asLong()).contains(it.channelId.asLong()) }
                .map(MessageDataCollector::aggregateMessageData)
                .onEach(messagePointRepository::save)
                .catch { error -> logger.catching(error) }
        }

    private suspend fun aggregateMessageData(context: Context): MessagePoint {
        logger.entry(context)

        val privacySettings = privacySettingsRepository
            .findByUserAndGuild(context.user.id.asLong(), context.guildId.asLong())

        val result = MessagePoint(
            guildId = context.guildId.asString(),
            channelId = context.channelId.asString(),
            userId = if (privacySettings?.message == true) context.user.id.asString() else null,
            length = context.message.length,
            wordCount = context.message.split(" ").size,
            timestamp = context.timestamp
        )

        return logger.exit(result)
    }
}
