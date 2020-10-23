package com.github.shirleh.statistics.message

import com.github.shirleh.administration.ChannelRepository
import com.github.shirleh.extensions.orElseNull
import com.github.shirleh.persistence.influx.DataPointRepository
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.time.Instant

private data class MessageData(
    val guildId: String,
    val channelId: String,
    val authorId: String,
    val length: Int,
    val wordCount: Int,
    val timestamp: Instant
) {
    fun toDataPoint() = Point.measurement("message")
        .addTag("guildId", guildId)
        .addTag("channel", channelId)
        .addTag("author", authorId)
        .addField("length", length)
        .addField("wordCount", wordCount)
        .time(timestamp, WritePrecision.MS)
}

object MessageDataCollector : KoinComponent {

    private val logger = KotlinLogging.logger { }

    private val channelRepository: ChannelRepository by inject()
    private val dataPointRepository: DataPointRepository by inject()

    /**
     * Collects message data from the incoming [events].
     */
    fun addListener(events: Flow<MessageCreateEvent>) = events
        .buffer()
        .filter { event ->
            val guildId = event.guildId.map(Snowflake::asLong).orElseNull() ?: return@filter false
            val channelId = event.message.channelId.asLong()
            channelRepository.findAll(guildId).contains(channelId)
        }
        .filter { event -> event.message.author.map { !it.isBot }.orElse(false) }
        .mapNotNull(MessageDataCollector::toMessageData)
        .onEach { logger.debug { it } }
        .map(MessageData::toDataPoint)
        .onEach(dataPointRepository::save)
        .catch { error -> logger.catching(error) }

    private fun toMessageData(event: MessageCreateEvent): MessageData? {
        logger.entry(event)

        val guildId = event.guildId.map(Snowflake::asString).orElseNull() ?: return logger.exit(null)
        val message = event.message
        val channelId = message.channelId.asString()
        val authorId = message.author.map { it.id.asString() }.orElseNull() ?: return logger.exit(null)
        val timestamp = message.timestamp

        val result = MessageData(
            guildId = guildId,
            channelId = channelId,
            authorId = authorId,
            length = message.content.length,
            wordCount = message.content.split(" ").size,
            timestamp = timestamp
        )

        return logger.exit(result)
    }
}
