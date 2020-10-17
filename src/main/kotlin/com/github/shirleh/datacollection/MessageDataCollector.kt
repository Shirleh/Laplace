package com.github.shirleh.datacollection

import com.github.shirleh.administration.ChannelRepository
import com.github.shirleh.extensions.orElseNull
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.time.Instant

private data class Message(
    val guildId: String,
    val channelId: String,
    val authorId: String,
    val count: Long,
    val timestamp: Instant
) {
    fun toDataPoint() = Point.measurement("message_count")
        .addTag("guildId", guildId)
        .addTag("channel", channelId)
        .addTag("author", authorId)
        .addField("count", count)
        .time(timestamp, WritePrecision.S)
}

object MessageDataCollector : KoinComponent {

    private val logger = KotlinLogging.logger { }

    private val channelRepository: ChannelRepository by inject()
    private val dataPointRepository: DataPointRepository by inject()

    /**
     * Collects message data from the incoming [events].
     */
    suspend fun collect(events: Flow<MessageCreateEvent>) {
        events
            .filter { event ->
                val guildId = event.guildId.map(Snowflake::asLong).orElseNull() ?: return@filter false
                val channelId = event.message.channelId.asLong()
                channelRepository.findAll(guildId).contains(channelId)
            }
            .filter { event -> event.message.author.map { !it.isBot }.orElse(false) }
            .mapNotNull(MessageDataCollector::toMessage)
            .map(Message::toDataPoint)
            .collect(dataPointRepository::save)
    }

    private fun toMessage(event: MessageCreateEvent): Message? {
        logger.entry(event)

        val guildId = event.guildId.map(Snowflake::asString).orElseNull() ?: return logger.exit(null)
        val message = event.message
        val channelId = message.channelId.asString()
        val authorId = message.author.map { it.id.asString() }.orElseNull() ?: return logger.exit(null)
        val timestamp = message.timestamp

        val result = Message(
            guildId = guildId,
            channelId = channelId,
            authorId = authorId,
            count = 1L,
            timestamp = timestamp
        )

        return logger.exit(result)
    }
}
