package com.github.shirleh.statistics.message

import com.github.shirleh.administration.ChannelRepository
import com.github.shirleh.extensions.orElseNull
import com.github.shirleh.persistence.influx.DataPointRepository
import com.github.shirleh.statistics.privacy.PrivacySettingsRepository
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.time.Instant

private data class MessageData(
    val guildId: String,
    val channelId: String,
    val userId: String?,
    val length: Int,
    val wordCount: Int,
    val timestamp: Instant
) {
    fun toDataPoint() = Point.measurement("message")
        .addTag("guildId", guildId)
        .addTag("channel", channelId)
        .addTag("userId", userId ?: "")
        .addField("length", length)
        .addField("wordCount", wordCount)
        .time(timestamp, WritePrecision.MS)
}

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
    private val dataPointRepository: DataPointRepository by inject()

    private val logger = KotlinLogging.logger { }

    /**
     * Collects message data from the incoming [events].
     */
    fun addListener(events: Flow<MessageCreateEvent>) = events
        .buffer()
        .mapNotNull { event ->
            Context(
                guildId = event.guildId.orElseNull() ?: return@mapNotNull null,
                channelId = event.message.channelId,
                user = event.message.author.orElseNull() ?: return@mapNotNull null,
                message = event.message.content,
                timestamp = event.message.timestamp,
            )
        }
        .filter { context -> channelRepository.findAll(context.guildId.asLong()).contains(context.channelId.asLong()) }
        .filter { context -> !context.user.isBot }
        .map(MessageDataCollector::aggregateMessageData)
        .onEach { logger.debug { it } }
        .map(MessageData::toDataPoint)
        .onEach(dataPointRepository::save)
        .catch { error -> logger.catching(error) }

    private suspend fun aggregateMessageData(context: Context): MessageData {
        logger.entry(context)

        val privacySettings = privacySettingsRepository
            .findByUserAndGuild(context.user.id.asLong(), context.guildId.asLong())

        val result = MessageData(
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
